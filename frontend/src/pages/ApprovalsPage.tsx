import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
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

type ProgressResp = {
  serverNow: string;
  items: ProgressItem[];
};

type ProgressItem = {
  approvalType: "PARTY_APPLY" | "LEAGUE_APPLY";
  stageIndex: number;
  stageCode: string;
  stageName: string;
  stages: Array<{ code: string; name: string }>;
  lastResult: string | null;
  lastAssessedAt: string | null;
  nextDueAt: string | null;
  lastApprovalId: number | null;
  pendingApprovalId: number | null;
};

export function ApprovalsPage() {
  const [approvals, setApprovals] = useState<ApprovalDto[]>([]);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [detail, setDetail] = useState<ApprovalDetailDto | null>(null);
  const [progress, setProgress] = useState<ProgressResp | null>(null);
  const [error, setError] = useState<string | null>(null);

  const selected = useMemo(() => approvals.find((a) => a.id === selectedId) ?? null, [approvals, selectedId]);

  async function refresh() {
    const [a, p] = await Promise.all([
      apiFetch<ApprovalDto[]>("/approvals/me", { method: "GET" }),
      apiFetch<ProgressResp>("/approvals/progress/me", { method: "GET" }),
    ]);
    setApprovals(a);
    setProgress(p);
  }

  useEffect(() => {
    setError(null);
    void refresh().catch((e) => setError((e as Error).message));
  }, []);

  useEffect(() => {
    if (!selectedId) return;
    setError(null);
    setDetail(null);
    void apiFetch<ApprovalDetailDto>(`/approvals/${selectedId}`, { method: "GET" })
      .then(setDetail)
      .catch((e) => setError((e as Error).message));
  }, [selectedId]);

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
        <h2>党团事务（学生）</h2>
        <div className="row" style={{ justifyContent: "flex-end" }}>
          <Link to="/approvals/new" className="btn btnPrimary" style={{ textDecoration: "none", display: "inline-flex", alignItems: "center" }}>
            发起申请
          </Link>
          <button className="btn" onClick={() => void refresh()}>
            刷新
          </button>
        </div>
      </div>

      {error ? <div style={{ color: "var(--danger)" }}>{error}</div> : null}

      {progress ? (
        <div className="twoCol" style={{ gridTemplateColumns: "1fr 1fr" }}>
          {progress.items.map((it) => (
            <div key={it.approvalType} className="card">
              <div className="cardHeader">
                <div style={{ fontWeight: 700 }}>{it.approvalType === "PARTY_APPLY" ? "入党流程" : "入团流程"}</div>
                <span className="badge">{it.stageName}</span>
              </div>
              <div className="cardBody">
                <div className="grid" style={{ gap: 10 }}>
                  <div className="row" style={{ gap: 6, flexWrap: "wrap" }}>
                    {it.stages.map((s, idx) => (
                      <span
                        key={s.code}
                        className={idx === it.stageIndex ? "badge badgePrimary" : "badge"}
                        style={idx === it.stageIndex ? { fontWeight: 800 } : undefined}
                      >
                        {s.name}
                      </span>
                    ))}
                  </div>

                  {it.pendingApprovalId ? (
                    <div className="card" style={{ padding: 10, background: "var(--surface-2)" }}>
                      <div style={{ fontWeight: 700 }}>本次考核已提交，等待审批</div>
                      <div className="muted" style={{ marginTop: 4, fontSize: 12 }}>
                        approvalId={it.pendingApprovalId}
                      </div>
                    </div>
                  ) : null}

                  <TimeAxis serverNow={progress.serverNow} nextDueAt={it.nextDueAt} />

                  <div className="row" style={{ justifyContent: "flex-end", gap: 8 }}>
                    <Link
                      to={`/approvals/new?type=${it.approvalType}`}
                      className="btn btnPrimary"
                      style={{ textDecoration: "none", display: "inline-flex", alignItems: "center" }}
                    >
                      去提交考核
                    </Link>
                  </div>
                </div>
              </div>
            </div>
          ))}
        </div>
      ) : null}

      <div className="twoCol" style={{ gridTemplateColumns: "420px 1fr" }}>
        <div className="card">
          <div className="cardHeader">
            <div style={{ fontWeight: 600 }}>我的申请</div>
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
                  <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 10 }}>
                    <div style={{ fontWeight: 800 }}>{a.subject || typeLabel(a.type)}</div>
                    <span className={badgeClassForStatus(a.status)}>{a.status}</span>
                  </div>
                  <div className="muted" style={{ marginTop: 6, fontSize: 12 }}>
                    approvalId={a.id}
                  </div>
                </button>
              ))}
              {approvals.length === 0 ? <div className="muted">暂无申请记录。</div> : null}
            </div>
          </div>
        </div>

        <div className="card">
          <div className="cardHeader">
            <div style={{ fontWeight: 600 }}>申请详情</div>
            {selected ? <span className="badge">approvalId={selected.id}</span> : <span className="badge">未选择</span>}
          </div>
          <div className="cardBody">
            {!selected ? (
              <div className="muted">请选择一条申请查看详情。</div>
            ) : !detail ? (
              <div className="muted">加载中…</div>
            ) : (
              <div className="grid" style={{ gap: 12 }}>
                <div className="kvs">
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
              </div>
            )}
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

function TimeAxis({ serverNow, nextDueAt }: { serverNow: string; nextDueAt: string | null }) {
  const now = new Date(serverNow);
  const next = nextDueAt ? new Date(nextDueAt) : null;
  const diffDays = next ? Math.ceil((next.getTime() - now.getTime()) / 86400000) : null;
  const dueText = next ? fmtDateTime(nextDueAt!) : "已到最终阶段（无需再考核）";
  const statusBadge =
    !next
      ? "badge badgeOk"
      : diffDays !== null && diffDays <= 0
        ? "badge badgeDanger"
        : diffDays !== null && diffDays <= 7
          ? "badge badgeWarn"
          : "badge badgePrimary";

  return (
    <div className="card" style={{ padding: 12, background: "var(--surface-2)" }}>
      <div className="kvs" style={{ alignItems: "start" }}>
        <div className="kvKey">当前时间</div>
        <div className="kvVal">{fmtDateTime(serverNow)}</div>
        <div className="kvKey">下一次考核</div>
        <div className="kvVal">
          <span className={statusBadge}>{dueText}</span>
          {next && diffDays !== null ? (
            <span className="muted" style={{ marginLeft: 8, fontSize: 12 }}>
              {diffDays <= 0 ? `已到期（${Math.abs(diffDays)}天前）` : `还有 ${diffDays} 天`}
            </span>
          ) : null}
        </div>
      </div>
    </div>
  );
}

function fmtDateTime(iso: string) {
  const d = new Date(iso);
  return d.toLocaleString("zh-CN", { year: "numeric", month: "2-digit", day: "2-digit", hour: "2-digit", minute: "2-digit" });
}
