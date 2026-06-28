import { NavLink, Outlet, useNavigate } from "react-router-dom";
import { useAuth } from "../auth";

export function Layout() {
  const auth = useAuth();
  const navigate = useNavigate();

  const items: Array<{ to: string; label: string; show?: boolean }> = [
    { to: "/", label: "首页", show: true },
    { to: "/policy", label: "政策库", show: true },
    { to: "/notifications", label: "通知中心", show: auth.hasRole("STUDENT", "TEACHER", "LEADER", "CADRE") },
    { to: "/approvals", label: "党团事务", show: auth.hasRole("STUDENT") },
    { to: "/selftest", label: "党团理论自测", show: auth.hasRole("STUDENT") },
    { to: "/timeline-admin", label: "时间线配置", show: auth.hasRole("LEADER", "ADMIN") },
    { to: "/cert-templates", label: "证明模板", show: auth.hasRole("LEADER", "TEACHER") },
    { to: "/pending", label: "审批中心", show: auth.hasRole("TEACHER", "LEADER") },
    { to: "/students", label: "我的学生", show: auth.hasRole("TEACHER", "LEADER", "CADRE") },
    { to: "/profile", label: "我的画像", show: true },
  ];

  return (
    <div style={{ minHeight: "100vh" }}>
      <header style={{ position: "sticky", top: 0, zIndex: 10, background: "var(--surface)", borderBottom: "1px solid var(--border)" }}>
        <div className="container" style={{ paddingTop: 12, paddingBottom: 12 }}>
          <div className="row" style={{ justifyContent: "space-between" }}>
            <div className="row" style={{ gap: 10 }}>
              <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                <div
                  style={{
                    width: 28,
                    height: 28,
                    borderRadius: 9,
                    background: "rgba(37,99,235,0.12)",
                    border: "1px solid rgba(37,99,235,0.25)",
                  }}
                />
                <strong>学院服务平台</strong>
              </div>

              <nav className="row" style={{ gap: 6 }}>
                {items
                  .filter((x) => x.show)
                  .map((x) => (
                    <NavLink
                      key={x.to}
                      to={x.to}
                      style={({ isActive }) => ({
                        textDecoration: "none",
                        padding: "6px 10px",
                        borderRadius: 10,
                        border: "1px solid var(--border)",
                        color: isActive ? "var(--primary)" : "var(--text)",
                        background: isActive ? "rgba(37,99,235,0.06)" : "var(--surface)",
                      })}
                    >
                      {x.label}
                    </NavLink>
                  ))}
              </nav>
            </div>

            {auth.user ? (
              <div className="row" style={{ justifyContent: "flex-end" }}>
                <span className="badge">
                  {auth.user.realName} · {auth.user.roles.join(",")}
                </span>
                <button
                  className="btn"
                  onClick={() => {
                    auth.logout();
                    navigate("/login");
                  }}
                >
                  退出
                </button>
              </div>
            ) : null}
          </div>
        </div>
      </header>

      <main className="container">
        <Outlet />
      </main>
    </div>
  );
}
