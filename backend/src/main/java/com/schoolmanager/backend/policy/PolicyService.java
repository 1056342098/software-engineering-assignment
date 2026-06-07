package com.schoolmanager.backend.policy;

import com.schoolmanager.backend.common.ApiException;
import com.schoolmanager.backend.config.AppProperties;
import com.schoolmanager.backend.oplog.OperationLogService;
import com.schoolmanager.backend.policy.entity.PolicyDoc;
import com.schoolmanager.backend.policy.entity.PolicyDocChunk;
import com.schoolmanager.backend.policy.repo.PolicyDocChunkRepository;
import com.schoolmanager.backend.policy.repo.PolicyDocRepository;
import com.schoolmanager.backend.user.entity.SysUser;
import com.schoolmanager.backend.user.repo.SysUserRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
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

@Service
public class PolicyService {
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
    public PolicyDoc upload(long uploaderId, String title, String category, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ApiException(400, "请上传文件");
        }
        String originalName = file.getOriginalFilename() == null ? "policy" : file.getOriginalFilename();
        String lowerName = originalName.toLowerCase(Locale.ROOT);
        if (!lowerName.endsWith(".pdf") && !lowerName.endsWith(".txt") && !lowerName.endsWith(".doc") && !lowerName.endsWith(".docx")) {
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

            String extracted = null;
            boolean allowEmpty = safeName.toLowerCase(Locale.ROOT).endsWith(".pdf");
            try {
                extracted = extractText(path, safeName);
            } catch (ApiException e) {
                if (!allowEmpty) {
                    throw e;
                }
                extracted = "";
            } catch (Exception e) {
                if (!allowEmpty) {
                    throw e;
                }
                extracted = "";
            }
            if (!allowEmpty && (extracted == null || extracted.isBlank())) {
                throw new ApiException(400, "文件内容为空或无法提取文本");
            }

            PolicyDoc doc = new PolicyDoc();
            doc.setTitle(title == null || title.isBlank() ? originalName : title);
            doc.setCategory(category);
            doc.setFileName(originalName);
            doc.setFilePath(path.toAbsolutePath().toString());
            doc.setUploader(uploader);
            doc = docRepository.save(doc);

            if (extracted != null && !extracted.isBlank()) {
                List<String> chunks = chunkText(extracted, 800);
                int idx = 0;
                for (String c : chunks) {
                    if (c.isBlank()) {
                        continue;
                    }
                    PolicyDocChunk chunk = new PolicyDocChunk();
                    chunk.setDoc(doc);
                    chunk.setChunkNo(idx++);
                    chunk.setChunkText(c);
                    chunkRepository.save(chunk);
                }
            }

            Map<String, Object> meta = new LinkedHashMap<>();
            meta.put("title", doc.getTitle());
            meta.put("category", doc.getCategory());
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
    public PolicyDoc updateMeta(long operatorId, long docId, String title, String category) {
        PolicyDoc doc = docRepository.findById(docId).orElseThrow(() -> new ApiException(404, "未找到该政策文件"));
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
        doc = docRepository.save(doc);
        Map<String, Object> meta = new LinkedHashMap<>();
        meta.put("title", doc.getTitle());
        meta.put("category", doc.getCategory());
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
                doc.getParagraphs().forEach(p -> {
                    String t = p.getText();
                    if (t != null && !t.isBlank()) {
                        sb.append(t).append("\n");
                    }
                });
                return sb.toString();
            }
        }
        if (lower.endsWith(".pdf")) {
            try (PDDocument pdf = Loader.loadPDF(path.toFile())) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(pdf);
            }
        }
        if (lower.endsWith(".txt")) {
            return Files.readString(path, StandardCharsets.UTF_8);
        }
        throw new ApiException(400, "不支持的文件类型");
    }

    private static List<String> chunkText(String text, int maxLen) {
        String normalized = text.replace("\r", "");
        String[] blocks = normalized.split("\\n\\s*\\n");
        List<String> out = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        for (String b : blocks) {
            String s = b.strip();
            if (s.isBlank()) {
                continue;
            }
            if (cur.length() == 0) {
                cur.append(s);
                continue;
            }
            if (cur.length() + 2 + s.length() <= maxLen) {
                cur.append("\n\n").append(s);
                continue;
            }
            out.add(cur.toString());
            cur.setLength(0);
            if (s.length() <= maxLen) {
                cur.append(s);
            } else {
                int i = 0;
                while (i < s.length()) {
                    int end = Math.min(i + maxLen, s.length());
                    out.add(s.substring(i, end));
                    i = end;
                }
            }
        }
        if (cur.length() > 0) {
            out.add(cur.toString());
        }
        return out;
    }
}
