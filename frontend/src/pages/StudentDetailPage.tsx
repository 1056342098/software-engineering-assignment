import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { apiFetch } from "../api";

type Competition = { name?: string; level?: string; year?: number };
type Practice = { name?: string; hours?: number };

type ProfileResp = {
  kind?: "STUDENT" | "USER";
  userId?: number;
  loginName?: string | null;
  email?: string | null;
  studentId?: number | null;
  studentNo?: string | null;
  major?: string | null;
  grade?: number | null;
  className?: string | null;
  realName?: string | null;
  public: {
    competitions?: Competition[];
    practices?: Practice[];
    [k: string]: unknown;
  };
  sensitive: { masked?: boolean } | null | Record<string, unknown>;
};

export function StudentDetailPage() {
  const params = useParams();
  const studentId = Number(params.studentId);
  const [data, setData] = useState<ProfileResp | null>(null);
  const [studentProgress, setStudentProgress] = useState<{ serverNow: string; items: any[] } | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!studentId) return;
    setError(null);
    void apiFetch<ProfileResp>(`/profile/students/${studentId}`, { method: "GET" })
      .then(setData)
      .catch((e) => setError((e as Error).message));
      
    void apiFetch<{ serverNow: string; items: any[] }>(`/approvals/progress/student/${studentId}`, { method: "GET" })
      .then(setStudentProgress)
      .catch(() => {});
  }, [studentId]);

  const comps = data?.public?.competitions ?? [];
  const practices = data?.public?.practices ?? [];
  const sensitiveMasked = data?.sensitive && typeof data.sensitive === "object" && "masked" in data.sensitive;

  return (
    <div className="grid">
      <div className="pageTitle">
        <div className="row" style={{ gap: 10 }}>
          <h2>学生详情</h2>
          <Link to="/students" className="btn" style={{ textDecoration: "none", display: "inline-flex", alignItems: "center" }}>
            返回列表
          </Link>
        </div>
        <span className="muted" style={{ fontSize: 12 }}>
          studentId={studentId}
        </span>
      </div>

      {error ? <div style={{ color: "var(--danger)" }}>{error}</div> : null}
      {!data ? <div className="muted">加载中…</div> : null}

      {data ? (
        <div className="twoCol">
          <section className="card">
            <div className="cardHeader">
              <div style={{ fontWeight: 600 }}>基本信息</div>
              <span className="badge badgePrimary">{data.className ?? "-"}</span>
            </div>
            <div className="cardBody">
              <div className="kvs">
                <div className="kvKey">姓名</div>
                <div className="kvVal">{data.realName ?? "-"}</div>
                <div className="kvKey">学号</div>
                <div className="kvVal">{data.studentNo ?? "-"}</div>
                <div className="kvKey">专业</div>
                <div className="kvVal">{data.major ?? "-"}</div>
                <div className="kvKey">年级</div>
                <div className="kvVal">{data.grade ?? "-"}</div>
                <div className="kvKey">登录名</div>
                <div className="kvVal">{data.loginName ?? "-"}</div>
              </div>
            </div>
          </section>

          <section className="card">
            <div className="cardHeader">
              <div style={{ fontWeight: 600 }}>敏感信息</div>
              {sensitiveMasked ? <span className="badge badgeWarn">已脱敏</span> : <span className="badge badgeOk">可见</span>}
            </div>
            <div className="cardBody">
              {data.sensitive == null ? (
                <div className="muted">暂无敏感信息。</div>
              ) : sensitiveMasked ? (
                <div className="muted">当前角色无权限查看敏感字段。</div>
              ) : (
                <div className="kvs">
                  {Object.entries(data.sensitive as Record<string, unknown>).flatMap(([k, v]) => [
                    <div key={k + ":k"} className="kvKey">
                      {k}
                    </div>,
                    <div key={k + ":v"} className="kvVal">
                      {v == null ? "-" : String(v)}
                    </div>,
                  ])}
                </div>
              )}
            </div>
          </section>

          <section className="card" style={{ gridColumn: "1 / -1" }}>
            <div className="cardHeader">
              <div style={{ fontWeight: 600 }}>公开画像</div>
              <span className="badge">{Object.keys(data.public ?? {}).length} 项</span>
            </div>
            <div className="cardBody">
              <div className="twoCol">
                <div className="card" style={{ background: "var(--surface-2)" }}>
                  <div className="cardHeader">
                    <div style={{ fontWeight: 600 }}>科研/竞赛</div>
                    <span className="badge">{comps.length}</span>
                  </div>
                  <div className="cardBody">
                    {comps.length === 0 ? (
                      <div className="muted">暂无记录。</div>
                    ) : (
                      <div className="grid" style={{ gap: 8 }}>
                        {comps.map((c, idx) => (
                          <div key={idx} className="card" style={{ padding: 10, borderRadius: 10 }}>
                            <div style={{ fontWeight: 600 }}>{c.name ?? "未命名"}</div>
                            <div className="muted" style={{ fontSize: 12 }}>
                              {(c.level ?? "未注明")} · {(c.year ?? "-")}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>

                <div className="card" style={{ background: "var(--surface-2)" }}>
                  <div className="cardHeader">
                    <div style={{ fontWeight: 600 }}>社会实践</div>
                    <span className="badge">{practices.length}</span>
                  </div>
                  <div className="cardBody">
                    {practices.length === 0 ? (
                      <div className="muted">暂无记录。</div>
                    ) : (
                      <div className="grid" style={{ gap: 8 }}>
                        {practices.map((p, idx) => (
                          <div key={idx} className="card" style={{ padding: 10, borderRadius: 10 }}>
                            <div style={{ fontWeight: 600 }}>{p.name ?? "未命名"}</div>
                            <div className="muted" style={{ fontSize: 12 }}>
                              {p.hours == null ? "-" : `${p.hours} 小时`}
                            </div>
                          </div>
                        ))}
                      </div>
                    )}
                  </div>
                </div>
              </div>
            </div>
          </section>
          
          {studentProgress && studentProgress.items.length > 0 && (
            <section className="card" style={{ gridColumn: "1 / -1" }}>
              <div className="cardHeader">
                <div style={{ fontWeight: 600 }}>党团事务进度</div>
              </div>
              <div className="cardBody">
                <div className="twoCol">
                  {studentProgress.items.map((it: any) => (
                    <div key={it.approvalType} className="card" style={{ background: "var(--surface-2)" }}>
                      <div className="cardHeader">
                        <div style={{ fontWeight: 600 }}>{it.approvalType === "PARTY_APPLY" ? "入党流程" : "入团流程"}</div>
                        <span className="badge badgePrimary">{it.stageName}</span>
                      </div>
                      <div className="cardBody">
                        <div className="row" style={{ gap: 8, flexWrap: "wrap", alignItems: "center" }}>
                          {it.stages.map((s: any, idx: number) => (
                            <div key={s.code} className="row" style={{ alignItems: "center", gap: 8 }}>
                              <span
                                className={
                                  idx < it.stageIndex
                                    ? "badge badgeOk"
                                    : idx === it.stageIndex
                                      ? "badge badgePrimary"
                                      : "badge"
                                }
                                style={
                                  idx === it.stageIndex
                                    ? { fontWeight: 800, border: "2px solid var(--primary)" }
                                    : undefined
                                }
                              >
                                {s.name}
                              </span>
                              {idx < it.stages.length - 1 && (
                                <span style={{ color: "var(--muted)", fontWeight: 700 }}>→</span>
                              )}
                            </div>
                          ))}
                        </div>
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            </section>
          )}
        </div>
      ) : null}
    </div>
  );
}

