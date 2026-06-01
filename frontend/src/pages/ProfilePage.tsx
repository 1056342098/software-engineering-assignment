import { useEffect, useMemo, useState } from "react";
import type { Dispatch, SetStateAction } from "react";
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

type CreditModule = { name: string; required: number; earned: number };

export function ProfilePage() {
  const [data, setData] = useState<ProfileResp | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [creditModules, setCreditModules] = useState<CreditModule[]>([]);
  const [creditEditOpen, setCreditEditOpen] = useState(false);
  const [creditDraft, setCreditDraft] = useState<CreditModule[]>([]);

  useEffect(() => {
    setError(null);
    void apiFetch<ProfileResp>("/profile/me", { method: "GET" })
      .then(setData)
      .catch((e) => setError((e as Error).message));
  }, []);

  useEffect(() => {
    const loaded = readCreditModules(data?.public?.credits);
    setCreditModules(loaded ?? []);
  }, [data]);

  const comps = data?.public?.competitions ?? [];
  const practices = data?.public?.practices ?? [];
  const sensitiveMasked = data?.sensitive && typeof data.sensitive === "object" && "masked" in data.sensitive;

  const creditStats = useMemo(() => {
    const modules = creditModules ?? [];
    const totalRequired = sum(modules.map((m) => safeNum(m.required)));
    const totalEarned = sum(modules.map((m) => Math.min(safeNum(m.earned), safeNum(m.required) || safeNum(m.earned))));
    const remaining = Math.max(totalRequired - totalEarned, 0);
    const pct = totalRequired > 0 ? Math.min(totalEarned / totalRequired, 1) : 0;
    return { totalRequired, totalEarned, remaining, pct };
  }, [creditModules]);

  async function saveCredits(modules: CreditModule[]) {
    if (!data) return;
    setError(null);
    setSaving(true);
    try {
      const nextPublic = { ...(data.public ?? {}) };
      const cleaned = modules
        .map((m) => ({ name: (m.name ?? "").trim(), required: safeNum(m.required), earned: safeNum(m.earned) }))
        .filter((m) => m.name);
      nextPublic.credits = { modules: cleaned };
      await apiFetch("/profile/me/public", { method: "PUT", body: JSON.stringify(nextPublic) });
      const refreshed = await apiFetch<ProfileResp>("/profile/me", { method: "GET" });
      setData(refreshed);
      setCreditEditOpen(false);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setSaving(false);
    }
  }

  return (
    <div className="grid">
      <div className="pageTitle">
        <h2>我的画像</h2>
        <span className="muted" style={{ fontSize: 12 }}>
          敏感信息按角色权限展示
        </span>
      </div>

      {error ? <div style={{ color: "var(--danger)" }}>{error}</div> : null}

      {!data ? <div className="muted">加载中…</div> : null}

      {data ? (
        <div className="twoCol">
          <section className="card">
            <div className="cardHeader">
              <div style={{ fontWeight: 600 }}>基本信息</div>
              <span className="badge badgePrimary">{data.studentId != null ? `studentId=${data.studentId}` : `userId=${data.userId ?? "-"}`}</span>
            </div>
            <div className="cardBody">
              <div className="kvs">
                <div className="kvKey">姓名</div>
                <div className="kvVal">{data.realName ?? "-"}</div>
                <div className="kvKey">登录名</div>
                <div className="kvVal">{data.loginName ?? "-"}</div>
                <div className="kvKey">学号</div>
                <div className="kvVal">{data.studentNo ?? "-"}</div>
                <div className="kvKey">专业</div>
                <div className="kvVal">{data.major ?? "-"}</div>
                <div className="kvKey">年级</div>
                <div className="kvVal">{data.grade ?? "-"}</div>
                <div className="kvKey">班级</div>
                <div className="kvVal">{data.className ?? "-"}</div>
                <div className="kvKey">邮箱</div>
                <div className="kvVal">{data.email ?? "-"}</div>
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


          <section className="card">
            <div className="cardHeader">
              <div style={{ fontWeight: 600 }}>公开画像</div>
              <span className="badge">{Object.keys(data.public ?? {}).length} 项</span>
            </div>
            <div className="cardBody">
              <div className="grid">
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
        
          {data.kind === "STUDENT" ? (
            <section className="card">
              <div className="cardHeader">
                <div style={{ fontWeight: 600 }}>学分进度</div>
                <div className="row" style={{ justifyContent: "flex-end" }}>
                  <span className="badge badgePrimary">
                    {creditStats.totalRequired > 0 ? `${creditStats.totalEarned}/${creditStats.totalRequired}` : "未设置"}
                  </span>
                  <button
                    className="btn"
                    onClick={() => {
                      setCreditDraft(creditModules.map((m) => ({ ...m })));
                      setCreditEditOpen(true);
                    }}
                  >
                    修改
                  </button>
                </div>
              </div>
              <div className="cardBody">
                {creditModules.length === 0 ? (
                  <div className="muted">暂无学分数据，可点击“修改”填写。</div>
                ) : (
                  <div className="grid" style={{ gap: 10 }}>
                    {creditStats.totalRequired > 0 ? (
                      <div className="grid" style={{ gap: 6 }}>
                        <div className="row" style={{ justifyContent: "space-between" }}>
                          <div style={{ fontWeight: 700 }}>总进度</div>
                          <span className="badge">{Math.round(creditStats.pct * 100)}%</span>
                        </div>
                        <ProgressBar pct={creditStats.pct} />
                        <div className="muted" style={{ fontSize: 12 }}>
                          剩余学分：{creditStats.remaining}
                        </div>
                      </div>
                    ) : (
                      <div className="muted">请先填写各模块毕业要求学分。</div>
                    )}

                    <div className="grid" style={{ gap: 8 }}>
                      {creditModules.map((m) => {
                        const required = safeNum(m.required);
                        const earned = safeNum(m.earned);
                        const pct = required > 0 ? Math.min(earned / required, 1) : 0;
                        return (
                          <div key={m.name} className="card" style={{ padding: 10, borderRadius: 10 }}>
                            <div className="row" style={{ justifyContent: "space-between", gap: 10, flexWrap: "wrap" }}>
                              <div style={{ fontWeight: 700 }}>{m.name}</div>
                              <span className="badge">{required > 0 ? `${Math.min(earned, required)}/${required}` : earned}</span>
                            </div>
                            <div style={{ marginTop: 8 }}>
                              <ProgressBar pct={pct} />
                            </div>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                )}
              </div>
            </section>
          ) : null}

          {creditEditOpen ? (
            <div
              style={{
                position: "fixed",
                inset: 0,
                background: "rgba(15,23,42,0.45)",
                display: "flex",
                alignItems: "center",
                justifyContent: "center",
                padding: 16,
                zIndex: 50,
              }}
              onClick={() => {
                if (!saving) setCreditEditOpen(false);
              }}
            >
              <div
                className="card"
                style={{ width: "min(920px, 100%)", maxHeight: "85vh", overflow: "auto" }}
                onClick={(e) => e.stopPropagation()}
              >
                <div className="cardHeader">
                  <div style={{ fontWeight: 700 }}>修改学分</div>
                  <span className="badge">{creditDraft.length}</span>
                </div>
                <div className="cardBody">
                  <div className="grid" style={{ gap: 10 }}>
                    {creditDraft.length === 0 ? <div className="muted">暂无模块，点击“添加模块”。</div> : null}

                    {creditDraft.map((m, idx) => (
                      <div key={idx} className="card" style={{ padding: 10, borderRadius: 10, background: "var(--surface-2)" }}>
                        <div className="row" style={{ gap: 8, alignItems: "stretch", flexWrap: "wrap" }}>
                          <input
                            className="input"
                            value={m.name}
                            onChange={(e) => updateDraft(idx, { name: e.target.value }, setCreditDraft)}
                            placeholder="模块名称（例如：专业核心课）"
                            style={{ flex: 1, minWidth: 220 }}
                          />
                          <input
                            className="input"
                            inputMode="numeric"
                            value={String(m.required ?? "")}
                            onChange={(e) => updateDraft(idx, { required: e.target.value }, setCreditDraft)}
                            placeholder="毕业要求"
                            style={{ width: 160 }}
                          />
                          <input
                            className="input"
                            inputMode="numeric"
                            value={String(m.earned ?? "")}
                            onChange={(e) => updateDraft(idx, { earned: e.target.value }, setCreditDraft)}
                            placeholder="已修学分"
                            style={{ width: 160 }}
                          />
                          <button className="btn btnDanger" onClick={() => removeDraft(idx, setCreditDraft)} disabled={saving}>
                            删除
                          </button>
                        </div>
                      </div>
                    ))}

                    <div className="row" style={{ justifyContent: "space-between" }}>
                      <button
                        className="btn"
                        onClick={() => setCreditDraft((prev) => [...prev, { name: "", required: 0, earned: 0 }])}
                        disabled={saving}
                      >
                        添加模块
                      </button>
                      <div className="row" style={{ justifyContent: "flex-end" }}>
                        <button className="btn btnPrimary" onClick={() => void saveCredits(creditDraft)} disabled={saving}>
                          {saving ? "保存中…" : "保存"}
                        </button>
                        <button className="btn" onClick={() => setCreditEditOpen(false)} disabled={saving}>
                          取消
                        </button>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          ) : null}
        </div>
      ) : null}
    </div>
  );
}

function ProgressBar({ pct }: { pct: number }) {
  const p = Math.max(0, Math.min(pct, 1));
  return (
    <div style={{ height: 10, background: "rgba(15,23,42,0.10)", borderRadius: 999, overflow: "hidden" }}>
      <div style={{ width: `${Math.round(p * 100)}%`, height: "100%", background: "rgba(37,99,235,0.85)" }} />
    </div>
  );
}

function readCreditModules(v: unknown): CreditModule[] | null {
  if (!v || typeof v !== "object") return null;
  const modules = (v as any).modules;
  if (!Array.isArray(modules)) return null;
  const out: CreditModule[] = [];
  for (const m of modules) {
    const name = typeof m?.name === "string" ? m.name : null;
    if (!name) continue;
    out.push({ name, required: safeNum(m.required), earned: safeNum(m.earned) });
  }
  return out.length ? out : null;
}

function updateDraft(
  idx: number,
  patch: { name?: string; required?: string; earned?: string },
  set: Dispatch<SetStateAction<CreditModule[]>>,
) {
  set((prev) => {
    const next = [...prev];
    const cur = next[idx];
    if (!cur) return prev;
    next[idx] = {
      ...cur,
      name: patch.name != null ? patch.name : cur.name,
      required: patch.required != null ? safeNum(patch.required) : cur.required,
      earned: patch.earned != null ? safeNum(patch.earned) : cur.earned,
    };
    return next;
  });
}

function removeDraft(idx: number, set: Dispatch<SetStateAction<CreditModule[]>>) {
  set((prev) => prev.filter((_, i) => i !== idx));
}

function safeNum(v: unknown): number {
  if (typeof v === "number" && Number.isFinite(v)) return Math.max(0, Math.round(v * 100) / 100);
  if (typeof v === "string") {
    const s = v.trim();
    if (!s) return 0;
    const n = Number(s);
    if (!Number.isFinite(n)) return 0;
    return Math.max(0, Math.round(n * 100) / 100);
  }
  return 0;
}

function sum(nums: number[]) {
  return nums.reduce((a, b) => a + b, 0);
}
