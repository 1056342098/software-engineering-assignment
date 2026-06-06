import { useEffect, useMemo, useRef, useState } from "react";
import { apiFetch, apiFetchBlob, apiFetchWithProgress } from "../api";
import { useAuth } from "../auth";

type PolicyDoc = { id: number; title: string; category: string | null; fileName: string | null; status: string };

export function PolicyPage() {
  const auth = useAuth();
  const canUpload = auth.hasRole("TEACHER", "LEADER");
  const [docs, setDocs] = useState<PolicyDoc[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [qaInput, setQaInput] = useState("");
  const [qaQuery, setQaQuery] = useState("");
  const [title, setTitle] = useState("");
  const [category, setCategory] = useState("");
  const [file, setFile] = useState<File | null>(null);
  const [uploading, setUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState<number | null>(null);
  const fileInputRef = useRef<HTMLInputElement | null>(null);
  const [viewMode, setViewMode] = useState<"all" | "mine">("all");
  const [editingId, setEditingId] = useState<number | null>(null);
  const [editTitle, setEditTitle] = useState("");
  const [editCategory, setEditCategory] = useState("");

  async function refresh() {
    setError(null);
    try {
      const path = canUpload && viewMode === "mine" ? "/policy/docs/mine" : "/policy/docs";
      const list = await apiFetch<PolicyDoc[]>(path, { method: "GET" });
      setDocs(list);
    } catch (e) {
      setError((e as Error).message);
    }
  }

  useEffect(() => {
    void refresh();
  }, [viewMode, canUpload]);

  async function upload() {
    if (!file) return;
    setUploading(true);
    setUploadProgress(0);
    setError(null);
    try {
      const fd = new FormData();
      fd.append("title", (title || file.name).trim() || file.name);
      if (category.trim()) fd.append("category", category.trim());
      fd.append("file", file, file.name);
      await apiFetchWithProgress("/policy/docs", fd, setUploadProgress);
      setTitle("");
      setCategory("");
      setFile(null);
      if (fileInputRef.current) fileInputRef.current.value = "";
      await refresh();
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setUploading(false);
      setUploadProgress(null);
    }
  }

  function startEdit(d: PolicyDoc) {
    setEditingId(d.id);
    setEditTitle(d.title);
    setEditCategory(d.category ?? "");
  }

  async function saveEdit() {
    if (!editingId) return;
    setError(null);
    try {
      await apiFetch(`/policy/docs/${editingId}`, {
        method: "PUT",
        body: JSON.stringify({ title: editTitle, category: editCategory }),
      });
      setEditingId(null);
      await refresh();
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function revokeDoc(id: number) {
    if (!window.confirm("确认撤回该政策文件？撤回后学生侧不可见，且将移除检索。")) return;
    setError(null);
    try {
      await apiFetch(`/policy/docs/${id}`, { method: "DELETE" });
      if (editingId === id) setEditingId(null);
      await refresh();
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function downloadDoc(d: PolicyDoc) {
    try {
      const blob = await apiFetchBlob(`/policy/docs/${d.id}/download`);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = d.fileName ?? `policy-${d.id}`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch (e) {
      setError((e as Error).message);
    }
  }

  const qaMatches = useMemo(() => {
    const q = qaQuery.trim().toLowerCase();
    if (!q) return [];
    const scored = docs
      .map((d) => {
        const t = (d.title ?? "").toLowerCase();
        const score = t === q ? 3 : t.includes(q) ? 2 : 0;
        return { d, score };
      })
      .filter((x) => x.score > 0)
      .sort((a, b) => (b.score - a.score) || (a.d.id - b.d.id));
    return scored.slice(0, 8).map((x) => x.d);
  }, [docs, qaQuery]);

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
          {/* <span className="badge">在政策标题中匹配</span> */}
        </div>
        <div className="cardBody">
          <form
            className="row"
            style={{ alignItems: "stretch" }}
            onSubmit={(e) => {
              e.preventDefault();
              setQaQuery(qaInput);
            }}
          >
            <input
              className="input"
              value={qaInput}
              onChange={(e) => setQaInput(e.target.value)}
              placeholder="输入关键词，例如：奖学金 / 请假 / 违纪…"
              style={{ flex: 1, minWidth: 240 }}
            />
            <button className="btn btnPrimary" type="submit">
              搜索
            </button>
            <button
              className="btn"
              type="button"
              onClick={() => {
                setQaInput("");
                setQaQuery("");
              }}
              disabled={!qaInput && !qaQuery}
            >
              清空
            </button>
          </form>

          {qaQuery.trim() ? (
            <div style={{ marginTop: 10 }}>
              {qaMatches.length === 0 ? (
                <div className="muted">未匹配到标题包含 “{qaQuery.trim()}” 的政策文件。</div>
              ) : (
                <div className="grid" style={{ gap: 8 }}>
                  <div className="muted" style={{ fontSize: 12 }}>
                    匹配到 {qaMatches.length} 条（展示前 {Math.min(qaMatches.length, 8)} 条）
                  </div>
                  {qaMatches.map((d) => (
                    <div key={d.id} className="row" style={{ justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
                      <div style={{ minWidth: 220 }}>
                        <div style={{ fontWeight: 700 }}>{d.title}</div>
                        <div className="row" style={{ marginTop: 6, gap: 6 }}>
                          <span className="badge">docId={d.id}</span>
                          {d.category ? <span className="badge badgePrimary">{d.category}</span> : null}
                          <span className={badgeClassForStatus(d.status)}>{d.status}</span>
                        </div>
                      </div>
                      {d.status === "ACTIVE" ? (
                        <button className="btn" type="button" onClick={() => void downloadDoc(d)}>
                          下载
                        </button>
                      ) : null}
                    </div>
                  ))}
                </div>
              )}
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
            <div className="row" style={{ alignItems: "stretch" }}>
              <input className="input" value={title} onChange={(e) => setTitle(e.target.value)} placeholder="标题（可选）" style={{ flex: 1, minWidth: 200 }} />
              <input className="input" value={category} onChange={(e) => setCategory(e.target.value)} placeholder="分类（可选）" style={{ width: 200 }} />
              <input ref={fileInputRef} className="input" type="file" onChange={(e) => setFile(e.target.files?.[0] ?? null)} style={{ width: 260 }} />
              <div className="row" style={{ alignItems: "center", gap: 12 }}>
                {uploadProgress !== null && (
                  <div className="muted" style={{ fontSize: 14 }}>
                    上传进度：{uploadProgress}%
                  </div>
                )}
                <button className="btn btnPrimary" onClick={upload} disabled={!file || uploading}>
                  {uploading ? "上传中…" : "上传"}
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : null}

      {error ? <div style={{ color: "var(--danger)" }}>{error}</div> : null}

      <div className="grid" style={{ gap: 10 }}>
        {docs.map((d) => (
          <div key={d.id} className="card">
            <div className="cardBody" style={{ display: "flex", gap: 12, alignItems: "center", flexWrap: "wrap" }}>
              <div style={{ flex: 1, minWidth: 240 }}>
                {editingId === d.id ? (
                  <div className="grid" style={{ gap: 8 }}>
                    <input className="input" value={editTitle} onChange={(e) => setEditTitle(e.target.value)} placeholder="标题" />
                    <input className="input" value={editCategory} onChange={(e) => setEditCategory(e.target.value)} placeholder="分类（可选）" />
                  </div>
                ) : (
                  <div style={{ fontWeight: 700 }}>{d.title}</div>
                )}
                <div className="row" style={{ marginTop: 6, gap: 6 }}>
                  <span className="badge">docId={d.id}</span>
                  {d.category ? <span className="badge badgePrimary">{d.category}</span> : null}
                  {d.fileName ? <span className="badge">{d.fileName}</span> : null}
                  <span className={badgeClassForStatus(d.status)}>{d.status}</span>
                </div>
              </div>
              {editingId === d.id ? (
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
                  {d.status === "ACTIVE" ? (
                    <button className="btn" onClick={() => void downloadDoc(d)}>
                      下载
                    </button>
                  ) : null}
                  {canUpload && viewMode === "mine" && d.status === "ACTIVE" ? (
                    <>
                      <button className="btn" onClick={() => startEdit(d)}>
                        编辑
                      </button>
                      <button className="btn btnDanger" onClick={() => void revokeDoc(d.id)}>
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
