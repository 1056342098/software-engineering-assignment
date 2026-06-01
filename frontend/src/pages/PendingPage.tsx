import { useEffect, useMemo, useState } from "react";
import { apiFetch, apiFetchBlob } from "../api";

type ApprovalDto = {
  id: number;
  applicantId: number | null;
  applicantName: string | null;
  finalApproverId: number | null;
  type: string;
  subject: string | null;
  status: string;
};

type ApprovalDetailDto = {
  id: number;
  applicantId: number | null;
  applicantName: string | null;
  type: string;
  subject: string | null;
  content: string | null;
  status: string;
  assignees: Array<{ id: number | null; name: string | null; status: string }>;
  attachments: Array<{ id: number; fileName: string; size: number | null }>;
};

export function PendingPage() {
  const [mode, setMode] = useState<"pending" | "history">("pending");
  const [approvals, setApprovals] = useState<ApprovalDto[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detail, setDetail] = useState<ApprovalDetailDto | null>(null);
  const [comment, setComment] = useState("");
  const [error, setError] = useState<string | null>(null);

  const selected = useMemo(() => approvals.find((a) => a.id === selectedId) ?? null, [approvals, selectedId]);

  async function refresh() {
    const path = mode === "pending" ? "/approvals/pending" : "/approvals/history";
    const list = await apiFetch<ApprovalDto[]>(path, { method: "GET" });
    setApprovals(list);
  }

  useEffect(() => {
    setError(null);
    void refresh().catch((e) => setError((e as Error).message));
  }, [mode]);

  useEffect(() => {
    if (!selectedId) return;
    setError(null);
    setDetail(null);
    void apiFetch<ApprovalDetailDto>(`/approvals/${selectedId}`, { method: "GET" })
      .then(setDetail)
      .catch((e) => setError((e as Error).message));
  }, [selectedId]);

  async function act(action: "approve" | "reject" | "revoke") {
    if (!selectedId) return;
    setError(null);
    try {
      await apiFetch(`/approvals/${selectedId}/${action}`, {
        method: "POST",
        body: JSON.stringify({ comment: comment.trim() || null }),
      });
      setComment("");
      await refresh();
      const next = await apiFetch<ApprovalDetailDto>(`/approvals/${selectedId}`, { method: "GET" });
      setDetail(next);
    } catch (e) {
      setError((e as Error).message);
    }
  }

  async function downloadAttachment(approvalId: number, attachmentId: number, fileName: string) {
    setError(null);
    try {
      const blob = await apiFetchBlob(`/approvals/${approvalId}/attachments/${attachmentId}/download`);
      const url = URL.createObjectURL(blob);
      const a = document.createElement("a");
      a.href = url;
      a.download = fileName || `attachment-${attachmentId}`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      URL.revokeObjectURL(url);
    } catch (e) {
      setError((e as Error).message);
    }
  }

  return (
    <div className="grid">
      <div className="pageTitle">
        <h2>审批中心（老师/领导）</h2>
        <div className="row">
          <button
            className={mode === "pending" ? "btn btnPrimary" : "btn"}
            onClick={() => {
              setMode("pending");
              setSelectedId(null);
              setDetail(null);
            }}
          >
            待办
          </button>
          <button
            className={mode === "history" ? "btn btnPrimary" : "btn"}
            onClick={() => {
              setMode("history");
              setSelectedId(null);
              setDetail(null);
            }}
          >
            历史
          </button>
        <button className="btn" onClick={() => void refresh()}>
          刷新
        </button>
        </div>
      </div>

      {error ? <div style={{ color: "var(--danger)" }}>{error}</div> : null}

      <div className="twoCol" style={{ gridTemplateColumns: "420px 1fr" }}>
        <div className="card">
          <div className="cardHeader">
            <div style={{ fontWeight: 600 }}>{mode === "pending" ? "待办审批" : "历史审批"}</div>
            <span className="badge">{approvals.length}</span>
          </div>
          <div className="cardBody">
            <div className="grid" style={{ gap: 10 }}>
              {approvals.map((a) => (
                <button
                  key={a.id}
                  className="btn"
                  onClick={() => setSelectedId(a.id)}
                  style={{
                    textAlign: "left",
                    padding: 12,
                    background: a.id === selectedId ? "rgba(37,99,235,0.06)" : "var(--surface)",
                    borderColor: a.id === selectedId ? "rgba(37,99,235,0.35)" : "var(--border)",
                  }}
                >
                  <div className="row" style={{ justifyContent: "space-between" }}>
                    <div style={{ fontWeight: 800 }}>{a.subject || typeLabel(a.type)}</div>
                    <span className={badgeClassForStatus(a.status)}>{a.status}</span>
                  </div>
                  <div className="muted" style={{ marginTop: 6, fontSize: 12 }}>
                    approvalId={a.id} · {a.applicantName ?? `applicantId=${a.applicantId ?? "-"}`}
                  </div>
                </button>
              ))}
              {approvals.length === 0 ? <div className="muted">{mode === "pending" ? "暂无待办记录。" : "暂无历史记录。"}</div> : null}
            </div>
          </div>
        </div>

        <div className="grid">
          <div className="card">
            <div className="cardHeader">
              <div style={{ fontWeight: 600 }}>处理</div>
              {selected ? <span className="badge">approvalId={selected.id}</span> : <span className="badge">未选择</span>}
            </div>
            <div className="cardBody">
              {!selected ? (
                <div className="muted">请选择一条审批记录。</div>
              ) : !detail ? (
                <div className="muted">加载中…</div>
              ) : (
                <div className="grid" style={{ gap: 12 }}>
                  <div className="kvs">
                    <div className="kvKey">申请人</div>
                    <div className="kvVal">{detail.applicantName ?? detail.applicantId ?? "-"}</div>
                    <div className="kvKey">事务类型</div>
                    <div className="kvVal">{typeLabel(detail.type)}</div>
                    <div className="kvKey">主题</div>
                    <div className="kvVal">{detail.subject ?? "-"}</div>
                    <div className="kvKey">状态</div>
                    <div className="kvVal">
                      <span className={badgeClassForStatus(detail.status)}>{detail.status}</span>
                    </div>
                    <div className="kvKey">审批人</div>
                    <div className="kvVal">
                      <div className="row">
                        {detail.assignees.map((x, idx) => (
                          <span key={idx} className={badgeClassForStatus(x.status)}>
                            {(x.name ?? x.id ?? "-") + " · " + x.status}
                          </span>
                        ))}
                      </div>
                    </div>
                  </div>

                  <div>
                    <div className="kvKey" style={{ marginBottom: 6 }}>
                      说明
                    </div>
                    <div className="card" style={{ background: "var(--surface-2)", padding: 12, whiteSpace: "pre-wrap" }}>
                      {detail.content?.trim() ? detail.content : "（无）"}
                    </div>
                  </div>

                  <div>
                    <div className="kvKey" style={{ marginBottom: 6 }}>
                      材料附件
                    </div>
                    {detail.attachments.length === 0 ? (
                      <div className="muted">（无附件）</div>
                    ) : (
                      <div className="row" style={{ gap: 8 }}>
                        {detail.attachments.map((f) => (
                          <button
                            key={f.id}
                            className="btn"
                            onClick={() => void downloadAttachment(detail.id, f.id, f.fileName)}
                          >
                            {f.fileName}
                          </button>
                        ))}
                      </div>
                    )}
                  </div>

                  <div className="grid" style={{ gap: 6 }}>
                    <div className="kvKey">处理备注</div>
                    <textarea
                      className="input"
                      value={comment}
                      onChange={(e) => setComment(e.target.value)}
                      placeholder="可选，填写审批意见…"
                      style={{ minHeight: 96, resize: "vertical" }}
                    />
                  </div>

                  <div className="row" style={{ justifyContent: "flex-end" }}>
                    <button className="btn btnPrimary" onClick={() => void act("approve")} disabled={detail.status !== "PENDING"}>
                      通过
                    </button>
                    <button className="btn btnDanger" onClick={() => void act("reject")} disabled={detail.status !== "PENDING"}>
                      驳回
                    </button>
                    <button className="btn" onClick={() => void act("revoke")} disabled={detail.status === "PENDING"}>
                      撤回/重批
                    </button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

function typeLabel(type: string) {
  if (type === "PARTY_APPLY") return "入党申请";
  if (type === "LEAGUE_APPLY") return "入团申请";
  if (type === "OTHER") return "其他";
  return type;
}

function badgeClassForStatus(status: string) {
  if (status === "APPROVED") return "badge badgeOk";
  if (status === "REJECTED") return "badge badgeDanger";
  if (status === "REVOKED") return "badge badgeWarn";
  if (status === "PENDING") return "badge badgePrimary";
  if (status === "DONE") return "badge badgeOk";
  return "badge";
}
