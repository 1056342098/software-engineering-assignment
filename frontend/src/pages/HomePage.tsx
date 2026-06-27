import { showError } from "../components/ErrorModal";
import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { useAuth } from "../auth";
import { apiFetch } from "../api";

type ProgressResp = {
  serverNow: string;
  items: Array<{
    approvalType: "PARTY_APPLY" | "LEAGUE_APPLY";
    stageName: string;
    nextDueAt: string | null;
    pendingApprovalId: number | null;
  }>;
};

type ProfileResp = {
  kind?: "STUDENT" | "USER";
  grade?: number | null;
  public?: { [k: string]: unknown };
};

export function HomePage() {
  const auth = useAuth();
  const [progress, setProgress] = useState<ProgressResp | null>(null);
  const [profile, setProfile] = useState<ProfileResp | null>(null);
  
  useEffect(() => {
    if (!auth.hasRole("STUDENT")) return;
        void Promise.all([
      apiFetch<ProgressResp>("/approvals/progress/me", { method: "GET" }),
      apiFetch<ProfileResp>("/profile/me", { method: "GET" }),
    ])
      .then(([p, prof]) => {
        setProgress(p);
        setProfile(prof);
      })
      .catch((e) => showError((e as Error).message));
  }, [auth]);

  const notices = useMemo(() => {
    if (!progress) return [];
    const now = new Date(progress.serverNow);
    const out: Array<{
      approvalType: "PARTY_APPLY" | "LEAGUE_APPLY";
      title: string;
      dueText: string;
      badgeClass: string;
      actionTo: string;
    }> = [];
    for (const it of progress.items as any) {
      if (it.stageIndex >= it.stages.length) continue;
      const stage = it.stages[it.stageIndex];
      const start = stage?.startTime ? new Date(stage.startTime) : null;
      const end = stage?.endTime ? new Date(stage.endTime) : null;
      if (!start && !end) continue;
      
      let dueText = "";
      let badgeClass = "";
      let title = "";
      
      const kind = it.approvalType === "PARTY_APPLY" ? "入党" : "入团";
      if (it.pendingApprovalId) {
        title = `${kind}考核：已提交，等待审批`;
        dueText = "已提交";
        badgeClass = "badge badgePrimary";
      } else if (end && now > end) {
        title = `${kind}考核：已过期`;
        dueText = "已过期";
        badgeClass = "badge badgeDanger";
      } else if (start && now < start) {
        title = `${kind}考核：即将开放`;
        const diffDays = Math.ceil((start.getTime() - now.getTime()) / 86400000);
        dueText = `还有 ${diffDays} 天开放`;
        badgeClass = "badge badgeWarn";
      } else {
        title = `${kind}考核：开放中`;
        const diffDays = end ? Math.ceil((end.getTime() - now.getTime()) / 86400000) : 0;
        dueText = end ? `还有 ${diffDays} 天截止` : "开放中";
        badgeClass = diffDays <= 3 ? "badge badgeDanger" : "badge badgeOk";
      }

      out.push({ approvalType: it.approvalType, title, dueText, badgeClass, actionTo: `/approvals/new?type=${it.approvalType}` });
    }
    return out;
  }, [progress]);

  const creditNotice = useMemo(() => {
    if (!profile || profile.kind !== "STUDENT") return null;
    const grade = profile.grade ?? null;
    if (grade == null) return null;
    const yearOfStudy = calcYearOfStudy(grade);
    if (yearOfStudy < 4) return null;

    const credits = (profile.public as any)?.credits;
    const modules = Array.isArray(credits?.modules) ? credits.modules : [];
    let totalRequired = 0;
    let totalEarned = 0;
    for (const m of modules) {
      const required = safeNum(m?.required);
      const earned = safeNum(m?.earned);
      totalRequired += required;
      totalEarned += Math.min(earned, required || earned);
    }
    if (totalRequired <= 0) return null;
    const remaining = Math.max(totalRequired - totalEarned, 0);
    if (remaining <= 20) return null;

    return { remaining, totalRequired, totalEarned };
  }, [profile]);

  return (
    <div className="grid">
      <div className="pageTitle">
        <h2>首页</h2>
        {/* <span className="muted" style={{ fontSize: 12 }}>
          简洁版 Web 前端（后续可扩展小程序端）
        </span> */}
      </div>

      {auth.hasRole("STUDENT") && creditNotice ? (
        <div className="card">
          <div className="cardHeader">
            <div style={{ fontWeight: 700 }}>毕业学分提醒</div>
            <span className="badge badgeDanger">剩余 {creditNotice.remaining}</span>
          </div>
          <div className="cardBody">
            <div className="row" style={{ justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
              <div>
                <div style={{ fontWeight: 700 }}>大四仍有较多学分未修完</div>
                <div className="muted" style={{ marginTop: 4, fontSize: 12 }}>
                  当前已修 {creditNotice.totalEarned}/{creditNotice.totalRequired}，请尽快核对毕业要求并规划选课。
                </div>
              </div>
              <Link to="/profile" className="btn btnPrimary" style={{ textDecoration: "none", display: "inline-flex", alignItems: "center" }}>
                去查看学分
              </Link>
            </div>
          </div>
        </div>
      ) : null}

      {auth.hasRole("STUDENT") && notices.length ? (
        <div className="card">
          <div className="cardHeader">
            <div style={{ fontWeight: 700 }}>临近考核提醒（7天内）</div>
            <span className="badge badgeWarn">{notices.length}</span>
          </div>
          <div className="cardBody">
            <div className="grid" style={{ gap: 10 }}>
              {notices.map((n) => (
                <div key={n.approvalType} className="row" style={{ justifyContent: "space-between", gap: 12, flexWrap: "wrap" }}>
                  <div>
                    <div style={{ fontWeight: 700 }}>{n.title}</div>
                    <div className="muted" style={{ marginTop: 4, fontSize: 12 }}>
                      下一次考核：<span className={n.badgeClass}>{n.dueText}</span>
                    </div>
                  </div>
                  <Link to={n.actionTo} className="btn btnPrimary" style={{ textDecoration: "none", display: "inline-flex", alignItems: "center" }}>
                    去提交
                  </Link>
                </div>
              ))}
            </div>
          </div>
        </div>
      ) : null}

      <div className="row" style={{ gap: 12 }}>
        <Card title="政策库" to="/policy" desc="老师上传政策文档（≤30MB），系统抽取文本并支持检索。" />
        <Card title="通知中心" to="/notifications" desc="老师按标签和渠道推送通知，学生查看收件箱并标记已读。" />
        {auth.hasRole("STUDENT") ? <Card title="党团事务" to="/approvals" desc="入党/入团申请提交与进度追踪。" /> : null}
        {auth.hasRole("TEACHER", "LEADER") ? <Card title="审批中心" to="/pending" desc="老师处理待办，支持撤回/重批窗口。" /> : null}
        <Card title="我的画像" to="/profile" desc="基础信息 + 公开画像；敏感信息按角色脱敏。" />
      </div>
    </div>
  );
}

function calcYearOfStudy(grade: number) {
  if (grade <= 10) return grade;
  const currentYear = new Date().getFullYear();
  return currentYear - grade + 1;
}

function safeNum(v: unknown): number {
  if (typeof v === "number" && Number.isFinite(v)) return Math.max(0, Math.min(Math.round(v * 100) / 100, 1000));
  if (typeof v === "string") {
    const s = v.trim();
    if (!s) return 0;
    const n = Number(s);
    if (!Number.isFinite(n)) return 0;
    return Math.max(0, Math.min(Math.round(n * 100) / 100, 1000));
  }
  return 0;
}

function Card({ title, to, desc }: { title: string; to: string; desc: string }) {
  return (
    <Link
      to={to}
      style={{
        display: "block",
        width: 260,
        textDecoration: "none",
      }}
    >
      <div className="card" style={{ padding: 14 }}>
        <div style={{ display: "flex", alignItems: "center", justifyContent: "space-between", gap: 10 }}>
          <div style={{ fontWeight: 700, color: "var(--text)" }}>{title}</div>
          <span className="badge badgePrimary">进入</span>
        </div>
        <div className="muted" style={{ marginTop: 8, fontSize: 13, lineHeight: 1.45 }}>
          {desc}
        </div>
      </div>
    </Link>
  );
}
