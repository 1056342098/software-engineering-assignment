import { useEffect, useMemo, useState } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { apiFetch } from "../api";

type UserDto = { id: number; loginName: string; realName: string };

export function ApprovalApplyPage() {
  const nav = useNavigate();
  const [searchParams] = useSearchParams();
  const [type, setType] = useState<"PARTY_APPLY" | "LEAGUE_APPLY" | "OTHER">("PARTY_APPLY");
  const [subject, setSubject] = useState("");
  const [content, setContent] = useState("");
  const [teachers, setTeachers] = useState<UserDto[]>([]);
  const [leaders, setLeaders] = useState<UserDto[]>([]);
  const [selected, setSelected] = useState<number[]>([]);
  const [files, setFiles] = useState<File[]>([]);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const t = searchParams.get("type");
    if (t === "PARTY_APPLY" || t === "LEAGUE_APPLY" || t === "OTHER") {
      setType(t);
    }
  }, [searchParams]);

  useEffect(() => {
    setError(null);
    void Promise.all([
      apiFetch<UserDto[]>("/users?role=TEACHER", { method: "GET" }),
      apiFetch<UserDto[]>("/users?role=LEADER", { method: "GET" }),
    ])
      .then(([t, l]) => {
        setTeachers(t);
        setLeaders(l);
        const first = [...t, ...l][0]?.id;
        if (first && selected.length === 0) setSelected([first]);
      })
      .catch((e) => setError((e as Error).message));
  }, []);

  const approvers = useMemo(() => {
    const seen = new Set<number>();
    const all = [...teachers, ...leaders];
    const out: UserDto[] = [];
    for (const u of all) {
      if (seen.has(u.id)) continue;
      seen.add(u.id);
      out.push(u);
    }
    return out;
  }, [teachers, leaders]);

  const selectedSet = useMemo(() => new Set(selected), [selected]);

  function toggleApprover(id: number) {
    setSelected((prev) => {
      if (prev.includes(id)) return prev.filter((x) => x !== id);
      return [...prev, id];
    });
  }

  async function submit() {
    setError(null);
    if (!subject.trim()) {
      setError("请填写主题。");
      return;
    }
    if (selected.length === 0) {
      setError("请至少选择一位审批老师。");
      return;
    }
    setSubmitting(true);
    try {
      const fd = new FormData();
      fd.append("type", type);
      fd.append("subject", subject.trim());
      if (content.trim()) fd.append("content", content);
      fd.append("approverIds", selected.join(","));
      for (const f of files) {
        fd.append("files", f, f.name);
      }
      await apiFetch("/approvals/apply", { method: "POST", body: fd });
      nav("/approvals");
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="grid" style={{ maxWidth: 980 }}>
      <div className="pageTitle">
        <div className="row" style={{ gap: 10 }}>
          <h2>发起申请</h2>
          <Link to="/approvals" className="btn" style={{ textDecoration: "none", display: "inline-flex", alignItems: "center" }}>
            返回
          </Link>
        </div>
        <span className="muted" style={{ fontSize: 12 }}>
          多选审批人 · 任一通过即可
        </span>
      </div>

      {error ? <div style={{ color: "var(--danger)" }}>{error}</div> : null}

      <div className="twoCol" style={{ gridTemplateColumns: "1fr 360px" }}>
        <div className="card">
          <div className="cardHeader">
            <div style={{ fontWeight: 600 }}>申请内容</div>
            <span className="badge">邮件式编辑</span>
          </div>
          <div className="cardBody">
            <div className="grid" style={{ gap: 10 }}>
              <div className="row" style={{ alignItems: "stretch" }}>
                <select className="select" value={type} onChange={(e) => setType(e.target.value as typeof type)} style={{ width: 200 }}>
                  <option value="PARTY_APPLY">入党申请</option>
                  <option value="LEAGUE_APPLY">入团申请</option>
                  <option value="OTHER">其他</option>
                </select>
                <input className="input" value={subject} onChange={(e) => setSubject(e.target.value)} placeholder="主题（必填）" style={{ flex: 1 }} />
              </div>

              <div className="grid" style={{ gap: 6 }}>
                <div className="kvKey">说明</div>
                <textarea
                  className="input"
                  value={content}
                  onChange={(e) => setContent(e.target.value)}
                  placeholder="请填写说明、材料概述、关键信息等…"
                  style={{ minHeight: 240, resize: "vertical" }}
                />
              </div>

              <div className="grid" style={{ gap: 6 }}>
                <div className="kvKey">材料附件</div>
                <input
                  className="input"
                  type="file"
                  multiple
                  onChange={(e) => setFiles(Array.from(e.target.files ?? []))}
                />
                {files.length ? (
                  <div className="row" style={{ gap: 6 }}>
                    {files.map((f) => (
                      <span key={f.name + f.size} className="badge">
                        {f.name}
                      </span>
                    ))}
                  </div>
                ) : (
                  <div className="muted" style={{ fontSize: 12 }}>
                    可选，单个文件建议不超过 30MB。
                  </div>
                )}
              </div>

              <div className="row" style={{ justifyContent: "flex-end" }}>
                <button className="btn btnPrimary" onClick={submit} disabled={submitting}>
                  {submitting ? "提交中…" : "发送申请"}
                </button>
              </div>
            </div>
          </div>
        </div>

        <div className="card">
          <div className="cardHeader">
            <div style={{ fontWeight: 600 }}>审批人</div>
            <span className="badge">{selected.length}</span>
          </div>
          <div className="cardBody">
            {approvers.length === 0 ? (
              <div className="muted">暂无可选审批人。</div>
            ) : (
              <div className="grid" style={{ gap: 8 }}>
                {approvers.map((u) => (
                  <label key={u.id} className="card" style={{ padding: 10, borderRadius: 10, background: "var(--surface-2)" }}>
                    <div className="row" style={{ justifyContent: "space-between", width: "100%" }}>
                      <div>
                        <div style={{ fontWeight: 700 }}>{u.realName}</div>
                        <div className="muted" style={{ fontSize: 12 }}>
                          {u.loginName}
                        </div>
                      </div>
                      <input type="checkbox" checked={selectedSet.has(u.id)} onChange={() => toggleApprover(u.id)} />
                    </div>
                  </label>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
