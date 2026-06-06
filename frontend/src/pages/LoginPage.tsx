import { useState, type FormEvent } from "react";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../auth";

export function LoginPage() {
  const auth = useAuth();
  const nav = useNavigate();
  const [loginName, setLoginName] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  async function onSubmit(e: FormEvent) {
    e.preventDefault();
    setError(null);
    try {
      await auth.login(loginName, password);
      nav("/", { replace: true });
    } catch (err) {
      setError((err as Error).message);
    }
  }

  return (
    <div className="container" style={{ maxWidth: 480, paddingTop: 44 }}>
      <div className="card" style={{ boxShadow: "var(--shadow)" }}>
        <div className="cardHeader">
          <div style={{ fontWeight: 700 }}>登录</div>
          <span className="badge">演示环境</span>
        </div>
        <div className="cardBody">
          <form onSubmit={onSubmit} className="grid" style={{ gap: 10 }}>
            <div className="grid" style={{ gap: 6 }}>
              <div className="kvKey">学号/工号</div>
              <input className="input" name="loginName" value={loginName} onChange={(e) => setLoginName(e.target.value)} />
            </div>
            <div className="grid" style={{ gap: 6 }}>
              <div className="kvKey">密码</div>
              <input className="input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
            </div>
            <button className="btn btnPrimary" type="submit">
              登录
            </button>
          </form>
          {error ? (
            <div style={{ marginTop: 10, color: "var(--danger)" }}>
              {error}
            </div>
          ) : null}
          <div className="muted" style={{ marginTop: 12, fontSize: 12 }}>
            演示账号：student1 / teacher1 / cadre1 / leader1（密码均为 123456）
          </div>
        </div>
      </div>
    </div>
  );
}
