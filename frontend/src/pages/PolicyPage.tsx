import { showError } from "../components/ErrorModal";
import { useEffect, useRef, useState } from "react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { apiFetch, apiFetchBlob, apiFetchWithProgress } from "../api";
import { useAuth } from "../auth";

type PolicyDoc = {
  id: number;
  title: string;
  category: string | null;
  versionLabel: string | null;
  summaryText: string | null;
  standardAnswer: string | null;
  fileName: string | null;
  status: string;
};

type QaSource = {
  docId: number | null;
  title: string | null;
  chunkNo: number;
  snippet: string;
  fileName: string | null;
  category: string | null;
  score: number | null;
};

type QaResp = {
  answer: string;
  sources: QaSource[];
  grounded: boolean;
  strategy: string;
};

export function PolicyPage() {
  const auth = useAuth();
  const canUpload = auth.hasRole("TEACHER", "LEADER");
  const [docs, setDocs] = useState<PolicyDoc[]>([]);
  const [qaInput, setQaInput] = useState("");
  const [qaResult, setQaResult] = useState<QaResp | null>(null);
  const [qaLoading, setQaLoading] = useState(false);
  const [displayedAnswer, setDisplayedAnswer] = useState("");
  const [qaTyping, setQaTyping] = useState(false);
  const [title, setTitle] = useState("");
  const [category, setCategory] = useState("");
  const [versionLabel, setVersionLabel] = useState("");
  const [summaryText, setSummaryText] = useState("");
  const [standardAnswer, setStandardAnswer] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const typingTimerRef = useRef<number | null>(null);
  const [viewMode, setViewMode] = useState<"all" | "mine">("all");
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editTitle, setEditTitle] = useState("");
  const [editCategory, setEditCategory] = useState("");
  const [editVersionLabel, setEditVersionLabel] = useState("");
  const [editSummaryText, setEditSummaryText] = useState("");
  const [editStandardAnswer, setEditStandardAnswer] = useState("");

  async function refresh() {
    try {
      const path = canUpload && viewMode === "mine" ? "/policy/docs/mine" : "/policy/docs";
      const list = await apiFetch<PolicyDoc[]>(path, { method: "GET" });
      setDocs(list);
    } catch (e) {
      showError((e as Error).message);
    }
  }

  useEffect(() => {
    void refresh();
  }, [viewMode, canUpload]);

  useEffect(() => {
    stopAnswerTyping();
    const nextAnswer = qaResult?.answer ?? "";
    if (!nextAnswer) {
      setDisplayedAnswer("");
      setQaTyping(false);
      return;
    }

    let nextIndex = 0;
    setDisplayedAnswer("");
    setQaTyping(true);

    const tick = () => {
      nextIndex += 1;
      setDisplayedAnswer(nextAnswer.slice(0, nextIndex));
      if (nextIndex < nextAnswer.length) {
        typingTimerRef.current = window.setTimeout(tick, 14);
      } else {
        typingTimerRef.current = null;
        setQaTyping(false);
      }
    };

    typingTimerRef.current = window.setTimeout(tick, 14);
    return stopAnswerTyping;
  }, [qaResult]);

  function stopAnswerTyping() {
    if (typingTimerRef.current != null) {
      window.clearTimeout(typingTimerRef.current);
      typingTimerRef.current = null;
    }
  }

  function skipTyping() {
    stopAnswerTyping();
    setDisplayedAnswer(qaResult?.answer ?? "");
    setQaTyping(false);
  }

  function replayTyping() {
    if (!qaResult?.answer) return;
    setQaResult({ ...qaResult });
  }

  async function askQuestion() {
    if (!qaInput.trim()) return;
    stopAnswerTyping();
    setQaLoading(true);
    setQaResult(null);
    setDisplayedAnswer("");
    setQaTyping(false);
    try {
      const result = await apiFetch<QaResp>("/qa/ask", {
        method: "POST",
        body: JSON.stringify({ question: qaInput.trim(), topK: 5 }),
      });
      setQaResult(result);
    } catch (e) {
      showError((e as Error).message);
    } finally {
      setQaLoading(false);
    }
  }

  async function upload() {
    if (!file) return;
    const allowedExtensions = [".pdf", ".txt", ".doc", ".docx"];
    const fileExt = file.name.toLowerCase().substring(file.name.lastIndexOf("."));
    if (!allowedExtensions.includes(fileExt)) {
      showError("政策库仅支持上传 pdf, txt, word (.doc, .docx) 格式的文件。");
      return;
    }
    if (file.size > 30 * 1024 * 1024) {
      showError("文件大小不能超过 30MB。");
      return;
    }

    setUploading(true);
    setUploadProgress(0);
    try {
      const fd = new FormData();
      fd.append("title", (title || file.name).trim() || file.name);
      if (category.trim()) fd.append("category", category.trim());
      if (versionLabel.trim()) fd.append("versionLabel", versionLabel.trim());
      if (summaryText.trim()) fd.append("summaryText", summaryText.trim());
      if (standardAnswer.trim()) fd.append("standardAnswer", standardAnswer.trim());
      fd.append("file", file, file.name);
      await apiFetchWithProgress("/policy/docs", fd, setUploadProgress);
      setTitle("");
      setCategory("");
      setVersionLabel("");
      setSummaryText("");
      setStandardAnswer("");
      setFile(null);
      if (fileInputRef.current) fileInputRef.current.value = "";
      await refresh();
    } catch (e) {
      showError((e as Error).message);
    } finally {
      setUploading(false);
      setUploadProgress(null);
    }
  }

  function startEdit(doc: PolicyDoc) {
    setEditingId(doc.id);
    setEditTitle(doc.title);
    setEditCategory(doc.category ?? "");
    setEditVersionLabel(doc.versionLabel ?? "");
    setEditSummaryText(doc.summaryText ?? "");
    setEditStandardAnswer(doc.standardAnswer ?? "");
  }

  async function saveEdit() {
    if (!editingId) return;
    try {
      await apiFetch(`/policy/docs/${editingId}`, {
        method: "PUT",
        body: JSON.stringify({
          title: editTitle,
          category: editCategory,
          versionLabel: editVersionLabel,
          summaryText: editSummaryText,
          standardAnswer: editStandardAnswer,
        }),
      });
      setEditingId(null);
      await refresh();
    } catch (e) {
      showError((e as Error).message);
    }
  }

  async function revokeDoc(id: number) {
    if (!window.confirm("确认撤回该政策文件？撤回后学生侧不可见，且将移除检索。")) return;
    try {
      await apiFetch(`/policy/docs/${id}`, { method: "DELETE" });
      if (editingId === id) setEditingId(null);
      await refresh();
    } catch (e) {
      showError((e as Error).message);
    }
  }

  async function downloadDoc(doc: PolicyDoc) {
    try {
      const blob = await apiFetchBlob(`/policy/docs/${doc.id}/download`);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = doc.fileName ?? `policy-${doc.id}`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch (e) {
      showError((e as Error).message);
    }
  }

  return (
    <div className="grid">
      <div className="pageTitle">
        <h2>政策库</h2>
        <div className="row" style={{ justifyContent: "flex-end" }}>
          {canUpload ? (
            <>
              <button className="btn" onClick={() => setViewMode("all")} disabled={viewMode === "all"}>
                全部
              </button>
              <button className="btn" onClick={() => setViewMode("mine")} disabled={viewMode === "mine"}>
                我上传的
              </button>
            </>
          ) : null}
          <button className="btn" onClick={() => void refresh()}>
            刷新
          </button>
        </div>
      </div>

      <div className="card">
        <div className="cardHeader">
          <div style={{ fontWeight: 600 }}>智能问答</div>
          <span className="badge">{qaResult?.strategy ?? "RAG"}</span>
        </div>
        <div className="cardBody">
          <form
            className="row"
            style={{ alignItems: "stretch" }}
            onSubmit={(e) => {
              e.preventDefault();
              void askQuestion();
            }}
          >
            <input
              className="input"
              value={qaInput}
              onChange={(e) => setQaInput(e.target.value)}
              placeholder="例如：请假流程需要哪些材料？奖学金评定依据哪份文件？"
              style={{ flex: 1, minWidth: 240 }}
            />
            <button className="btn btnPrimary" type="submit" disabled={!qaInput.trim() || qaLoading}>
              {qaLoading ? "查询中…" : "提问"}
            </button>
            <button
              className="btn"
              type="button"
              onClick={() => {
                stopAnswerTyping();
                setQaInput("");
                setQaResult(null);
                setDisplayedAnswer("");
                setQaTyping(false);
              }}
              disabled={!qaInput && !qaResult}
            >
              清空
            </button>
          </form>

          <div className="muted" style={{ marginTop: 10, fontSize: 12, lineHeight: 1.5 }}>
            系统优先使用标准答案、摘要和原文片段组织回复；若未配置大模型接口，会自动回退到保守检索答案。
          </div>

          {qaResult ? (
            <div className="grid" style={{ gap: 12, marginTop: 14 }}>
              <div className="card" style={{ border: "1px dashed var(--border)", boxShadow: "none" }}>
                <div className="cardBody">
                  <div className="row" style={{ justifyContent: "space-between", gap: 12, marginBottom: 8 }}>
                    <div style={{ fontWeight: 700 }}>回答</div>
                    <div className="row" style={{ gap: 8 }}>
                      {qaTyping ? <span className="badge badgePrimary">输出中</span> : null}
                      {qaTyping ? (
                        <button className="btn" type="button" onClick={skipTyping}>
                          跳过动画
                        </button>
                      ) : qaResult.answer ? (
                        <button className="btn" type="button" onClick={replayTyping}>
                          重新播放
                        </button>
                      ) : null}
                    </div>
                  </div>
                  <div className="markdownAnswer">
                    <ReactMarkdown remarkPlugins={[remarkGfm]}>{displayedAnswer}</ReactMarkdown>
                    {qaTyping ? <span className="typingCursor" aria-hidden="true" /> : null}
                  </div>
                </div>
              </div>

              <div className="grid" style={{ gap: 8 }}>
                <div style={{ fontWeight: 700 }}>来源溯源</div>
                {qaResult.sources.map((source, idx) => {
                  const doc = source.docId ? docs.find((item) => item.id === source.docId) : null;
                  return (
                    <div key={`${source.docId ?? "unknown"}-${source.chunkNo}-${idx}`} className="card" style={{ boxShadow: "none" }}>
                      <div className="cardBody">
                        <div className="row" style={{ justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
                          <div style={{ minWidth: 220 }}>
                            <div style={{ fontWeight: 700 }}>{source.title ?? "未知来源"}</div>
                            <div className="row" style={{ marginTop: 6, gap: 6 }}>
                              {source.docId ? <span className="badge">docId={source.docId}</span> : null}
                              {source.category ? <span className="badge badgePrimary">{source.category}</span> : null}
                              <span className="badge">chunk={source.chunkNo}</span>
                              {source.score != null ? <span className="badge">score={source.score.toFixed(3)}</span> : null}
                            </div>
                          </div>
                          {doc && doc.status === "ACTIVE" ? (
                            <button className="btn" type="button" onClick={() => void downloadDoc(doc)}>
                              下载原文
                            </button>
                          ) : null}
                        </div>
                        <div className="muted" style={{ marginTop: 10, fontSize: 13, lineHeight: 1.55 }}>
                          {source.snippet}
                        </div>
                      </div>
                    </div>
                  );
                })}
                {qaResult.sources.length === 0 ? <div className="muted">当前没有可展示的来源。</div> : null}
              </div>
            </div>
          ) : null}
        </div>
      </div>

      {canUpload ? (
        <div className="card">
          <div className="cardHeader">
            <div style={{ fontWeight: 600 }}>上传政策文档</div>
            <span className="badge">docx / pdf / txt · ≤30MB</span>
          </div>
          <div className="cardBody">
            <div className="grid" style={{ gap: 10 }}>
              <div className="row" style={{ alignItems: "stretch" }}>
                <input className="input" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="标题（可选）" style={{ flex: 1, minWidth: 200 }} />
                <input className="input" value={category} onChange={(e) => setCategory(e.target.value)} placeholder="分类（可选）" style={{ width: 180 }} />
                <input className="input" value={versionLabel} onChange={(e) => setVersionLabel(e.target.value)} placeholder="版本号，如 v2026.1" style={{ width: 180 }} />
                <input ref={fileInputRef} className="input" type="file" accept=".pdf,.txt,.doc,.docx" onChange={(e) => setFile(e.target.files?.[0] ?? null)} style={{ width: 260 }} />
              </div>
              <textarea className="input" value={summaryText} onChange={(e) => setSummaryText(e.target.value)} placeholder="知识条目摘要（可选）" rows={3} />
              <textarea className="input" value={standardAnswer} onChange={(e) => setStandardAnswer(e.target.value)} placeholder="标准答案（可选，问答时会优先使用）" rows={4} />
              <div className="row" style={{ alignItems: "center", gap: 12 }}>
                {uploadProgress !== null ? <div className="muted" style={{ fontSize: 14 }}>上传进度：{uploadProgress}%</div> : null}
                <button className="btn btnPrimary" onClick={upload} disabled={!file || uploading}>
                  {uploading ? "上传中…" : "上传"}
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}

      <div className="grid" style={{ gap: 10 }}>
        {docs.map((doc) => (
          <div key={doc.id} className="card">
            <div className="cardBody" style={{ display: "flex", gap: 12, alignItems: "center", flexWrap: "wrap" }}>
              <div style={{ flex: 1, minWidth: 240 }}>
                {editingId === doc.id ? (
                  <div className="grid" style={{ gap: 8 }}>
                    <input className="input" value={editTitle} onChange={(e) => setEditTitle(e.target.value)} placeholder="标题" />
                    <div className="row" style={{ alignItems: "stretch" }}>
                      <input className="input" value={editCategory} onChange={(e) => setEditCategory(e.target.value)} placeholder="分类（可选）" style={{ flex: 1 }} />
                      <input className="input" value={editVersionLabel} onChange={(e) => setEditVersionLabel(e.target.value)} placeholder="版本号（可选）" style={{ flex: 1 }} />
                    </div>
                    <textarea className="input" value={editSummaryText} onChange={(e) => setEditSummaryText(e.target.value)} placeholder="摘要（可选）" rows={3} />
                    <textarea className="input" value={editStandardAnswer} onChange={(e) => setEditStandardAnswer(e.target.value)} placeholder="标准答案（可选）" rows={4} />
                  </div>
                ) : (
                  <div className="grid" style={{ gap: 6 }}>
                    <div style={{ fontWeight: 700 }}>{doc.title}</div>
                    {doc.standardAnswer ? <div className="muted" style={{ fontSize: 13, lineHeight: 1.5 }}>标准答案：{doc.standardAnswer}</div> : null}
                    {doc.summaryText ? <div className="muted" style={{ fontSize: 13, lineHeight: 1.5 }}>摘要：{doc.summaryText}</div> : null}
                  </div>
                )}
                <div className="row" style={{ marginTop: 6, gap: 6 }}>
                  <span className="badge">docId={doc.id}</span>
                  {doc.category ? <span className="badge badgePrimary">{doc.category}</span> : null}
                  {doc.versionLabel ? <span className="badge">{doc.versionLabel}</span> : null}
                  {doc.fileName ? <span className="badge">{doc.fileName}</span> : null}
                  <span className={badgeClassForStatus(doc.status)}>{doc.status}</span>
                </div>
              </div>

              {editingId === doc.id ? (
                <div className="row" style={{ justifyContent: "flex-end" }}>
                  <button className="btn btnPrimary" onClick={() => void saveEdit()}>
                    保存
                  </button>
                  <button className="btn" onClick={() => setEditingId(null)}>
                    取消
                  </button>
                </div>
              ) : (
                <div className="row" style={{ justifyContent: "flex-end" }}>
                  {doc.status === "ACTIVE" ? (
                    <button className="btn" onClick={() => void downloadDoc(doc)}>
                      下载
                    </button>
                  ) : null}
                  {canUpload && viewMode === "mine" && doc.status === "ACTIVE" ? (
                    <>
                      <button className="btn" onClick={() => startEdit(doc)}>
                        编辑
                      </button>
                      <button className="btn btnDanger" onClick={() => void revokeDoc(doc.id)}>
                        撤回
                      </button>
                    </>
                  ) : null}
                </div>
              )}
            </div>
          </div>
        ))}
        {docs.length === 0 ? <div className="muted">{canUpload && viewMode === "mine" ? "暂无你上传的政策文档。" : "暂无政策文档，请先上传。"}</div> : null}
      </div>
    </div>
  );
}

function badgeClassForStatus(status: string) {
  if (status === "ACTIVE") return "badge badgeOk";
  if (status === "REVOKED") return "badge badgeDanger";
  return "badge";
}
