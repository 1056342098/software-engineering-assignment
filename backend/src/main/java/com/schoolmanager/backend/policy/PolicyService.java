package com.schoolmanager.backend.policy;

import com.schoolmanager.backend.common.ApiException;
import com.schoolmanager.backend.config.AppProperties;
import com.schoolmanager.backend.oplog.OperationLogService;
import com.schoolmanager.backend.policy.entity.PolicyDoc;
import com.schoolmanager.backend.policy.entity.PolicyDocChunk;
import com.schoolmanager.backend.policy.repo.PolicyDocChunkRepository;
import com.schoolmanager.backend.policy.repo.PolicyDocRepository;
import com.schoolmanager.backend.qa.AcademicQueryExpander;
import com.schoolmanager.backend.qa.QueryTextAnalyzer;
import com.schoolmanager.backend.user.entity.SysUser;
import com.schoolmanager.backend.user.repo.SysUserRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Pattern;

@Service
public class PolicyService {
    private static final Pattern STRUCTURED_SPACES = Pattern.compile("(?<=\\S)\\s{2,}(?=\\S)");
    private static final Pattern HEADING_PATTERN = Pattern
            .compile("^(第[一二三四五六七八九十百0-9]+[章节]|[0-9]+(\\.[0-9]+)*|[一二三四五六七八九十]+、).+");
    private static final List<String> CURRICULUM_SECTION_HINTS = List.of(
            "培养方案", "课程设置", "修读要求", "教学计划", "课程体系", "毕业要求", "专业核心课程", "主干学科");

    private final PolicyDocRepository docRepository;
    private final PolicyDocChunkRepository chunkRepository;
    private final SysUserRepository userRepository;
    private final OperationLogService opLogService;
    private final Path policyDir;

    public PolicyService(
            PolicyDocRepository docRepository,
            PolicyDocChunkRepository chunkRepository,
            SysUserRepository userRepository,
            OperationLogService opLogService,
            AppProperties props) {
        this.docRepository = docRepository;
        this.chunkRepository = chunkRepository;
        this.userRepository = userRepository;
        this.opLogService = opLogService;
        this.policyDir = Path.of(props.getStorage().getPolicyDir());
    }

    public List<PolicyDoc> listDocs() {
        return docRepository.findByStatusOrderByIdDesc("ACTIVE");
    }

    public List<PolicyDoc> listMyDocs(long uploaderId) {
        return docRepository.findByUploader_IdOrderByIdDesc(uploaderId);
    }

    public Resource getFile(long docId) {
        PolicyDoc doc = docRepository.findById(docId).orElseThrow(() -> new ApiException(404, "未找到该政策文件"));
        if (!"ACTIVE".equalsIgnoreCase(doc.getStatus())) {
            throw new ApiException(400, "该政策文件已撤回");
        }
        if (doc.getFilePath() == null || doc.getFilePath().isBlank()) {
            throw new ApiException(404, "未找到文件");
        }
        return new FileSystemResource(doc.getFilePath());
    }

    @Transactional
    public PolicyDoc upload(long uploaderId, String title, String category, String versionLabel, String summaryText,
            String standardAnswer, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(400, "请上传文件");
        }
        String originalName = file.getOriginalFilename() == null ? "policy" : file.getOriginalFilename();
        String lowerName = originalName.toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".pdf") && !lowerName.endsWith(".txt") && !lowerName.endsWith(".doc")
                && !lowerName.endsWith(".docx")) {
            throw new ApiException(400, "不支持的文件类型");
        }
        if (file.getSize() > 30L * 1024 * 1024) {
            throw new ApiException(400, "文件过大");
        }
        SysUser uploader = userRepository.findById(uploaderId)
                .orElseThrow(() -> new ApiException(404, "未找到用户"));

        Path path = null;
        try {
            Files.createDirectories(policyDir);
            originalName = originalName.replace("\\", "/");
            int lastSlash = originalName.lastIndexOf('/');
            if (lastSlash >= 0) {
                originalName = originalName.substring(lastSlash + 1);
            }
            if (originalName.isBlank()) {
                originalName = "policy";
            }
            String safeName = originalName.replaceAll("[^a-zA-Z0-9._\\-\\u4e00-\\u9fa5]", "_");
            path = policyDir.resolve(Instant.now().toEpochMilli() + "_" + safeName);
            Files.copy(file.getInputStream(), path);

            String extracted;
            try {
                extracted = extractText(path, safeName);
            } catch (ApiException e) {
                if (normalize(summaryText) == null && normalize(standardAnswer) == null) {
                    throw e;
                }
                extracted = null;
            }

            PolicyDoc doc = new PolicyDoc();
            doc.setTitle(title == null || title.isBlank() ? originalName : title);
            doc.setCategory(category);
            doc.setVersionLabel(normalize(versionLabel));
            doc.setFileName(originalName);
            doc.setFilePath(path.toAbsolutePath().toString());
            doc.setSummaryText(normalize(summaryText));
            doc.setStandardAnswer(normalize(standardAnswer));
            doc.setUploader(uploader);
            doc = docRepository.save(doc);
            rebuildChunks(doc, extracted);

            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("title", doc.getTitle());
            meta.put("category", doc.getCategory());
            meta.put("versionLabel", doc.getVersionLabel());
            meta.put("fileName", doc.getFileName());
            opLogService.log(uploaderId, "POLICY_UPLOAD", "policy_doc", doc.getId(), meta);
            return doc;
        } catch (ApiException e) {
            if (path != null) {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                }
            }
            throw e;
        } catch (Exception e) {
            if (path != null) {
                try {
                    Files.deleteIfExists(path);
                } catch (Exception ignored) {
                }
            }
            throw new ApiException(500, "文件上传失败");
        }
    }

    @Transactional
    public PolicyDoc updateMeta(long operatorId, long docId, String title, String category, String versionLabel,
            String summaryText, String standardAnswer) {
        PolicyDoc doc = docRepository.findByIdWithUploader(docId).orElseThrow(() -> new ApiException(404, "未找到该政策文件"));
        if (doc.getUploader() == null || doc.getUploader().getId() == null || doc.getUploader().getId() != operatorId) {
            throw new ApiException(403, "只能修改自己上传的政策文件");
        }
        if (!"ACTIVE".equalsIgnoreCase(doc.getStatus())) {
            throw new ApiException(400, "该政策文件已撤回");
        }
        if (title != null) {
            String t = title.strip();
            if (t.isBlank()) {
                throw new ApiException(400, "标题不能为空");
            }
            doc.setTitle(t);
        }
        if (category != null) {
            String c = category.strip();
            doc.setCategory(c.isBlank() ? null : c);
        }
        if (versionLabel != null) {
            doc.setVersionLabel(normalize(versionLabel));
        }
        if (summaryText != null) {
            doc.setSummaryText(normalize(summaryText));
        }
        if (standardAnswer != null) {
            doc.setStandardAnswer(normalize(standardAnswer));
        }
        doc = docRepository.save(doc);
        try {
            rebuildChunks(doc, extractText(Path.of(doc.getFilePath()), doc.getFileName()));
        } catch (ApiException e) {
            if (normalizeMetadataText(doc) == null) {
                throw e;
            }
            rebuildChunks(doc, null);
        } catch (Exception e) {
            throw new ApiException(500, "重建政策索引失败");
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("title", doc.getTitle());
        meta.put("category", doc.getCategory());
        meta.put("versionLabel", doc.getVersionLabel());
        opLogService.log(operatorId, "POLICY_UPDATE", "policy_doc", doc.getId(), meta);
        return doc;
    }

    @Transactional
    public void revoke(long operatorId, long docId) {
        PolicyDoc doc = docRepository.findById(docId).orElseThrow(() -> new ApiException(404, "未找到该政策文件"));
        if (doc.getUploader() == null || doc.getUploader().getId() == null || doc.getUploader().getId() != operatorId) {
            throw new ApiException(403, "只能撤回自己上传的政策文件");
        }
        if (!"ACTIVE".equalsIgnoreCase(doc.getStatus())) {
            return;
        }
        doc.setStatus("REVOKED");
        docRepository.save(doc);
        chunkRepository.deleteByDoc_Id(doc.getId());
        if (doc.getFilePath() != null && !doc.getFilePath().isBlank()) {
            try {
                Files.deleteIfExists(Path.of(doc.getFilePath()));
            } catch (Exception ignored) {
            }
        }
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("title", doc.getTitle());
        meta.put("category", doc.getCategory());
        meta.put("fileName", doc.getFileName());
        opLogService.log(operatorId, "POLICY_REVOKE", "policy_doc", doc.getId(), meta);
    }

    private static String extractText(Path path, String fileName) throws Exception {
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".docx")) {
            try (InputStream in = Files.newInputStream(path); XWPFDocument doc = new XWPFDocument(in)) {
                StringBuilder sb = new StringBuilder();
                for (XWPFParagraph paragraph : doc.getParagraphs()) {
                    String t = normalizeStructuredLine(paragraph.getText());
                    if (t != null) {
                        sb.append(t).append("\n");
                    }
                }
                for (XWPFTable table : doc.getTables()) {
                    sb.append("\n");
                    for (XWPFTableRow row : table.getRows()) {
                        List<String> cells = new ArrayList<>();
                        for (XWPFTableCell cell : row.getTableCells()) {
                            String cellText = normalizeStructuredLine(cell.getText());
                            if (cellText != null) {
                                cells.add(cellText);
                            }
                        }
                        if (!cells.isEmpty()) {
                            sb.append(String.join(" | ", cells)).append("\n");
                        }
                    }
                }
                return ensureUsableExtractedText(fileName, sb.toString());
            }
        }
        if (lower.endsWith(".pdf")) {
            try (PDDocument pdf = Loader.loadPDF(path.toFile())) {
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                stripper.setLineSeparator("\n");
                stripper.setParagraphEnd("\n\n");
                return ensureUsableExtractedText(fileName, stripper.getText(pdf));
            }
        }
        if (lower.endsWith(".txt")) {
            return ensureUsableExtractedText(fileName, Files.readString(path, StandardCharsets.UTF_8));
        }
        if (lower.endsWith(".doc")) {
            throw new ApiException(400, "暂不支持 .doc 老格式文件，请转换为 .docx 后上传");
        }
        throw new ApiException(400, "不支持的文件类型");
    }

    private void rebuildChunks(PolicyDoc doc, String extractedText) {
        chunkRepository.deleteByDoc_Id(doc.getId());
        List<String> units = buildRetrievalUnits(doc, extractedText);
        if (units.isEmpty()) {
            throw new ApiException(400, "文档未提取到可检索内容，请补充摘要/标准答案或上传可复制文本的文件");
        }
        int idx = 0;
        for (String unit : units) {
            if (unit.isBlank()) {
                continue;
            }
            PolicyDocChunk chunk = new PolicyDocChunk();
            chunk.setDoc(doc);
            chunk.setChunkNo(idx++);
            chunk.setChunkText(unit);
            chunk.setSearchText(buildSearchText(doc, unit));
            chunkRepository.save(chunk);
        }
    }

    private static List<String> buildRetrievalUnits(PolicyDoc doc, String extractedText) {
        String extracted = normalize(extractedText);
        if (extracted != null) {
            String structured = normalizeStructuredDocument(extracted);
            if (looksLikeCurriculumPlan(doc, structured)) {
                List<String> specialized = curriculumPlanChunks(doc, structured, 900);
                if (!specialized.isEmpty()) {
                    return specialized;
                }
            }
            return chunkText(structured, 700, 120);
        }
        String metadataOnly = normalizeMetadataText(doc);
        if (metadataOnly == null) {
            return List.of();
        }
        return chunkText(metadataOnly, 500, 80);
    }

    private static String buildSearchText(PolicyDoc doc, String chunkText) {
        StringJoiner joiner = new StringJoiner("\n");
        appendIfPresent(joiner, doc.getTitle());
        appendIfPresent(joiner, doc.getCategory());
        appendIfPresent(joiner, doc.getVersionLabel());
        appendIfPresent(joiner, doc.getSummaryText());
        appendIfPresent(joiner, doc.getStandardAnswer());
        appendIfPresent(joiner, chunkText);
        appendIfPresent(joiner, AcademicQueryExpander.buildDocumentHints(
                doc.getTitle(), doc.getCategory(), doc.getSummaryText(), doc.getStandardAnswer(), doc.getFileName(),
                chunkText));
        return joiner.toString();
    }

    private static String normalizeMetadataText(PolicyDoc doc) {
        StringJoiner joiner = new StringJoiner("\n\n");
        appendIfPresent(joiner, doc.getStandardAnswer());
        appendIfPresent(joiner, doc.getSummaryText());
        return normalize(joiner.toString());
    }

    private static void appendIfPresent(StringJoiner joiner, String value) {
        String normalized = normalize(value);
        if (normalized != null) {
            joiner.add(normalized);
        }
    }

    private static String ensureUsableExtractedText(String fileName, String rawText) {
        String normalized = normalizeStructuredDocument(rawText == null ? null : rawText.replace("\u0000", ""));
        if (normalized != null) {
            return normalized;
        }
        String lower = fileName == null ? "" : fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf")) {
            throw new ApiException(400, "PDF 未提取到可用文本，可能是扫描件或图片版，请上传可复制文本的 PDF、DOCX 或补充摘要/标准答案");
        }
        throw new ApiException(400, "文件内容为空或无法提取文本");
    }

    private static List<String> chunkText(String text, int maxLen, int overlap) {
        String normalized = text.replace("\r", "");
        String[] blocks = normalized.split("\\n\\s*\\n");
        List<String> paragraphs = new ArrayList<>();
        for (String block : blocks) {
            String paragraph = block.strip();
            if (paragraph.isBlank()) {
                continue;
            }
            if (paragraph.length() <= maxLen) {
                paragraphs.add(paragraph);
                continue;
            }
            int start = 0;
            while (start < paragraph.length()) {
                int end = Math.min(start + maxLen, paragraph.length());
                if (end < paragraph.length()) {
                    int split = paragraph.lastIndexOf('。', end);
                    if (split <= start + Math.min(120, maxLen / 3)) {
                        split = paragraph.lastIndexOf('；', end);
                    }
                    if (split <= start + Math.min(120, maxLen / 3)) {
                        split = paragraph.lastIndexOf('，', end);
                    }
                    if (split > start + Math.min(80, maxLen / 4)) {
                        end = split + 1;
                    }
                }
                paragraphs.add(paragraph.substring(start, end).strip());
                if (end >= paragraph.length()) {
                    break;
                }
                start = Math.max(end - overlap, start + 1);
            }
        }

        List<String> out = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            if (current.length() == 0) {
                current.append(paragraph);
                continue;
            }
            if (current.length() + 2 + paragraph.length() <= maxLen) {
                current.append("\n\n").append(paragraph);
                continue;
            }
            out.add(current.toString());
            String carry = current.substring(Math.max(0, current.length() - overlap));
            current.setLength(0);
            current.append(carry).append("\n\n").append(paragraph);
            if (current.length() > maxLen) {
                out.add(current.toString());
                current.setLength(0);
            }
        }
        if (current.length() > 0) {
            out.add(current.toString());
        }
        return out.stream().map(String::strip).filter(s -> !s.isBlank()).distinct().toList();
    }

    private static List<String> curriculumPlanChunks(PolicyDoc doc, String text, int maxLen) {
        List<SectionBlock> sections = splitIntoSections(text);
        if (sections.isEmpty()) {
            return List.of();
        }
        List<String> chunks = new ArrayList<>();
        for (SectionBlock section : sections) {
            String heading = section.heading();
            String body = section.body();
            if (body.isBlank()) {
                continue;
            }
            String combined = (heading.isBlank() ? "" : heading + "\n") + body;
            if (combined.length() <= maxLen) {
                chunks.add(combined.strip());
                continue;
            }
            List<String> lines = body.lines().map(String::strip).filter(line -> !line.isBlank()).toList();
            StringBuilder current = new StringBuilder();
            if (!heading.isBlank()) {
                current.append(heading).append("\n");
            }
            for (String line : lines) {
                String candidate = current.length() == 0 ? line : current + "\n" + line;
                if (candidate.length() <= maxLen) {
                    if (current.length() > 0) {
                        current.append("\n");
                    }
                    current.append(line);
                    continue;
                }
                if (current.length() > 0) {
                    chunks.add(current.toString().strip());
                }
                current.setLength(0);
                if (!heading.isBlank()) {
                    current.append(heading).append("\n");
                }
                current.append(line);
            }
            if (current.length() > 0) {
                chunks.add(current.toString().strip());
            }
        }
        return chunks.stream().filter(value -> !value.isBlank()).distinct().toList();
    }

    private static List<SectionBlock> splitIntoSections(String text) {
        List<String> lines = text.lines()
                .map(PolicyService::normalizeStructuredLine)
                .filter(line -> line != null && !line.isBlank())
                .toList();
        List<SectionBlock> sections = new ArrayList<>();
        String currentHeading = "";
        StringBuilder body = new StringBuilder();
        for (String line : lines) {
            if (looksLikeHeading(line)) {
                if (body.length() > 0) {
                    sections.add(new SectionBlock(currentHeading, body.toString().strip()));
                    body.setLength(0);
                }
                currentHeading = line;
                continue;
            }
            if (body.length() > 0) {
                body.append("\n");
            }
            body.append(line);
        }
        if (body.length() > 0) {
            sections.add(new SectionBlock(currentHeading, body.toString().strip()));
        }
        return sections;
    }

    private static boolean looksLikeCurriculumPlan(PolicyDoc doc, String text) {
        String title = QueryTextAnalyzer.normalize(doc.getTitle());
        String category = QueryTextAnalyzer.normalize(doc.getCategory());
        String fileName = QueryTextAnalyzer.normalize(doc.getFileName());
        String combined = title + "\n" + category + "\n" + fileName + "\n" + QueryTextAnalyzer.normalize(text);
        int hitCount = 0;
        for (String hint : CURRICULUM_SECTION_HINTS) {
            if (combined.contains(QueryTextAnalyzer.normalize(hint))) {
                hitCount++;
            }
        }
        return hitCount >= 2 || combined.contains("专业培养方案");
    }

    private static boolean looksLikeHeading(String line) {
        if (line == null || line.isBlank()) {
            return false;
        }
        String normalized = QueryTextAnalyzer.normalize(line);
        if (normalized.length() > 48) {
            return false;
        }
        if (HEADING_PATTERN.matcher(line).matches()) {
            return true;
        }
        return normalized.contains("专业培养方案")
                || normalized.endsWith("课程设置")
                || normalized.endsWith("修读要求")
                || normalized.endsWith("毕业要求")
                || normalized.endsWith("专业核心课程")
                || normalized.endsWith("课程体系")
                || normalized.endsWith("教学计划")
                || normalized.endsWith("培养目标")
                || normalized.endsWith("主干学科");
    }

    private static String normalizeStructuredDocument(String text) {
        String normalized = normalize(text);
        if (normalized == null) {
            return null;
        }
        List<String> lines = text.replace("\r", "").lines()
                .map(PolicyService::normalizeStructuredLine)
                .filter(line -> line != null && !line.isBlank())
                .toList();
        if (lines.isEmpty()) {
            return null;
        }
        return String.join("\n", lines);
    }

    private static String normalizeStructuredLine(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.replace('\u3000', ' ')
                .replace('\t', '|')
                .replaceAll("\\s*\\|\\s*", " | ");
        normalized = STRUCTURED_SPACES.matcher(normalized).replaceAll(" | ");
        normalized = normalized.replaceAll("\\s+", " ").strip();
        return normalized.isBlank() ? null : normalized;
    }

    private record SectionBlock(String heading, String body) {
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        return trimmed.isBlank() ? null : trimmed;
    }
}
