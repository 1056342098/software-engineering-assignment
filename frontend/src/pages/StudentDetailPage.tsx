import { showError } from "../components/ErrorModal";
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { apiFetch } from "../api";
import { useAuth } from "../auth";

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
  const { hasRole } = useAuth();
  const params = useParams();
  const studentId = Number(params.studentId);
  const [data, setData] = useState<ProfileResp | null>(null);
  const [studentProgress, setStudentProgress] = useState<{ serverNow: string; items: any[] } | null>(null);

  const [showSensitiveModal, setShowSensitiveModal] = useState(false);
  const [sensitiveForm, setSensitiveForm] = useState({
    idCardNo: "",
    hukouAddr: "",
    hometown: "",
    tutor: "",
    delayInfo: ""
  });

  const [showPublicModal, setShowPublicModal] = useState(false);
  const [publicForm, setPublicForm] = useState({
    competitions: [] as Competition[],
    practices: [] as Practice[]
  });

  async function refresh() {
    if (!studentId) return;
    try {
      const res = await apiFetch<ProfileResp>(`/profile/students/${studentId}`, { method: "GET" });
      setData(res);
    } catch (e) {
      showError((e as Error).message);
    }
  }

  useEffect(() => {
    void refresh();
    if (studentId) {
      void apiFetch<{ serverNow: string; items: any[] }>(`/approvals/progress/student/${studentId}`, { method: "GET" })
        .then(setStudentProgress)
        .catch(() => {});
    }
  }, [studentId]);

  function handleEditSensitive() {
    const s = data?.sensitive as Record<string, string> | undefined;
    setSensitiveForm({
      idCardNo: s?.idCardNo ?? "",
      hukouAddr: s?.hukouAddr ?? "",
      hometown: s?.hometown ?? "",
      tutor: s?.tutor ?? "",
      delayInfo: s?.delayInfo ?? ""
    });
    setShowSensitiveModal(true);
  }

  async function handleSaveSensitive() {
    try {
      await apiFetch(`/profile/students/${studentId}/sensitive`, {
        method: "PUT",
        body: JSON.stringify(sensitiveForm),
      });
      setShowSensitiveModal(false);
      await refresh();
    } catch (e) {
      showError((e as Error).message);
    }
  }

  function handleEditPublic() {
    setPublicForm({
      competitions: data?.public?.competitions ? JSON.parse(JSON.stringify(data.public.competitions)) : [],
      practices: data?.public?.practices ? JSON.parse(JSON.stringify(data.public.practices)) : []
    });
    setShowPublicModal(true);
  }

  async function handleSavePublic() {
    try {
      await apiFetch(`/profile/students/${studentId}/public`, {
        method: "PUT",
        body: JSON.stringify(publicForm),
      });
      setShowPublicModal(false);
      await refresh();
    } catch (e) {
      showError((e as Error).message);
    }
  }

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
              <div className="row" style={{ gap: 8 }}>
                {sensitiveMasked ? <span className="badge badgeWarn">已脱敏</span> : <span className="badge badgeOk">可见</span>}
                {hasRole("LEADER", "TEACHER") && !sensitiveMasked && (
                  <button className="btn" style={{ padding: "2px 8px", fontSize: 12 }} onClick={handleEditSensitive}>编辑</button>
                )}
              </div>
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
              <div className="row" style={{ gap: 8 }}>
                <span className="badge">{Object.keys(data.public ?? {}).length} 项</span>
                {hasRole("LEADER", "TEACHER") && (
                  <button className="btn" style={{ padding: "2px 8px", fontSize: 12 }} onClick={handleEditPublic}>编辑</button>
                )}
              </div>
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

      {showSensitiveModal && (
        <div style={{ position: "fixed", top: 0, left: 0, right: 0, bottom: 0, background: "rgba(0,0,0,0.5)", zIndex: 100, display: "flex", alignItems: "center", justifyContent: "center" }}>
          <div className="card" style={{ width: 400, maxWidth: "90%" }}>
            <div className="cardBody grid" style={{ gap: 12 }}>
              <h3>编辑敏感信息</h3>
              <input className="input" placeholder="身份证号" value={sensitiveForm.idCardNo} onChange={(e) => setSensitiveForm({ ...sensitiveForm, idCardNo: e.target.value })} />
              <input className="input" placeholder="户口所在地" value={sensitiveForm.hukouAddr} onChange={(e) => setSensitiveForm({ ...sensitiveForm, hukouAddr: e.target.value })} />
              <input className="input" placeholder="生源地" value={sensitiveForm.hometown} onChange={(e) => setSensitiveForm({ ...sensitiveForm, hometown: e.target.value })} />
              <input className="input" placeholder="导师/辅导员" value={sensitiveForm.tutor} onChange={(e) => setSensitiveForm({ ...sensitiveForm, tutor: e.target.value })} />
              <textarea className="input" placeholder="延毕/学籍异动信息" value={sensitiveForm.delayInfo} onChange={(e) => setSensitiveForm({ ...sensitiveForm, delayInfo: e.target.value })} rows={3} />
              <div className="row" style={{ justifyContent: "flex-end", gap: 8, marginTop: 12 }}>
                <button className="btn" onClick={() => setShowSensitiveModal(false)}>取消</button>
                <button className="btn btnPrimary" onClick={() => void handleSaveSensitive()}>保存</button>
              </div>
            </div>
          </div>
        </div>
      )}

      {showPublicModal && (
        <div style={{ position: "fixed", top: 0, left: 0, right: 0, bottom: 0, background: "rgba(0,0,0,0.5)", zIndex: 100, display: "flex", alignItems: "center", justifyContent: "center" }}>
          <div className="card" style={{ width: 600, maxWidth: "90%", maxHeight: "90vh", overflow: "auto" }}>
            <div className="cardBody grid" style={{ gap: 12 }}>
              <h3>编辑公开画像</h3>
              
              <div style={{ fontWeight: 600, marginTop: 10 }}>科研/竞赛</div>
              {publicForm.competitions.map((c, i) => (
                <div key={i} className="row" style={{ gap: 8 }}>
                  <input className="input" style={{ flex: 2 }} placeholder="名称" value={c.name ?? ""} onChange={e => {
                    const newComps = [...publicForm.competitions];
                    newComps[i].name = e.target.value;
                    setPublicForm({ ...publicForm, competitions: newComps });
                  }} />
                  <input className="input" style={{ flex: 1 }} placeholder="级别" value={c.level ?? ""} onChange={e => {
                    const newComps = [...publicForm.competitions];
                    newComps[i].level = e.target.value;
                    setPublicForm({ ...publicForm, competitions: newComps });
                  }} />
                  <input className="input" style={{ flex: 1 }} type="number" placeholder="年份" value={c.year ?? ""} onChange={e => {
                    const newComps = [...publicForm.competitions];
                    newComps[i].year = Number(e.target.value) || undefined;
                    setPublicForm({ ...publicForm, competitions: newComps });
                  }} />
                  <button className="btn" onClick={() => {
                    const newComps = publicForm.competitions.filter((_, idx) => idx !== i);
                    setPublicForm({ ...publicForm, competitions: newComps });
                  }}>删</button>
                </div>
              ))}
              <button className="btn" style={{ alignSelf: "flex-start" }} onClick={() => setPublicForm({ ...publicForm, competitions: [...publicForm.competitions, {}] })}>+ 添加科研/竞赛</button>

              <div style={{ fontWeight: 600, marginTop: 10 }}>社会实践</div>
              {publicForm.practices.map((p, i) => (
                <div key={i} className="row" style={{ gap: 8 }}>
                  <input className="input" style={{ flex: 2 }} placeholder="名称" value={p.name ?? ""} onChange={e => {
                    const newPracs = [...publicForm.practices];
                    newPracs[i].name = e.target.value;
                    setPublicForm({ ...publicForm, practices: newPracs });
                  }} />
                  <input className="input" style={{ flex: 1 }} type="number" placeholder="小时数" value={p.hours ?? ""} onChange={e => {
                    const newPracs = [...publicForm.practices];
                    newPracs[i].hours = Number(e.target.value) || undefined;
                    setPublicForm({ ...publicForm, practices: newPracs });
                  }} />
                  <button className="btn" onClick={() => {
                    const newPracs = publicForm.practices.filter((_, idx) => idx !== i);
                    setPublicForm({ ...publicForm, practices: newPracs });
                  }}>删</button>
                </div>
              ))}
              <button className="btn" style={{ alignSelf: "flex-start" }} onClick={() => setPublicForm({ ...publicForm, practices: [...publicForm.practices, {}] })}>+ 添加社会实践</button>

              <div className="row" style={{ justifyContent: "flex-end", gap: 8, marginTop: 24 }}>
                <button className="btn" onClick={() => setShowPublicModal(false)}>取消</button>
                <button className="btn btnPrimary" onClick={() => void handleSavePublic()}>保存</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

