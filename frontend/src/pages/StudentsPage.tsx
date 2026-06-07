import { showError } from "../components/ErrorModal";
import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";
import { apiFetch } from "../api";

type StudentDto = {
  id: number;
  realName: string | null;
  studentNo: string | null;
  major: string | null;
  grade: number | null;
  className: string | null;
};

export function StudentsPage() {
  const [items, setItems] = useState<StudentDto[]>([]);
  const [q, setQ] = useState("");
  
  async function refresh() {
        try {
      const list = await apiFetch<StudentDto[]>("/students", { method: "GET" });
      setItems(list);
    } catch (e) {
      showError((e as Error).message);
    }
  }

  useEffect(() => {
    void refresh();
  }, []);

  const filtered = useMemo(() => {
    const s = q.trim().toLowerCase();
    if (!s) return items;
    return items.filter((x) => {
      const hay = `${x.realName ?? ""} ${x.studentNo ?? ""} ${x.major ?? ""} ${x.className ?? ""}`.toLowerCase();
      return hay.includes(s);
    });
  }, [items, q]);

  return (
    <div className="grid">
      <div className="pageTitle">
        <h2>我的学生</h2>
        <button className="btn" onClick={() => void refresh()}>
          刷新
        </button>
      </div>

      <div className="card">
        <div className="cardBody">
          <div className="row" style={{ alignItems: "stretch" }}>
            <input className="input" value={q} onChange={(e) => setQ(e.target.value)} placeholder="搜索：姓名/学号/专业/班级" style={{ flex: 1 }} />
            <span className="badge">{filtered.length}</span>
          </div>
        </div>
      </div>

      <div className="grid" style={{ gap: 10 }}>
        {filtered.map((s) => (
          <Link key={s.id} to={`/students/${s.id}`} style={{ textDecoration: "none" }}>
            <div className="card">
              <div className="cardBody" style={{ display: "flex", gap: 12, alignItems: "center", flexWrap: "wrap" }}>
                <div style={{ flex: 1, minWidth: 260 }}>
                  <div style={{ fontWeight: 800, color: "var(--text)" }}>{s.realName ?? "未命名"}</div>
                  <div className="row" style={{ marginTop: 6, gap: 6 }}>
                    <span className="badge">id={s.id}</span>
                    {s.studentNo ? <span className="badge">{s.studentNo}</span> : null}
                    {s.className ? <span className="badge badgePrimary">{s.className}</span> : null}
                    {s.major ? <span className="badge">{s.major}</span> : null}
                    {s.grade != null ? <span className="badge">{s.grade}</span> : null}
                  </div>
                </div>
                <span className="badge badgePrimary">查看详情</span>
              </div>
            </div>
          </Link>
        ))}
        {filtered.length === 0 ? <div className="muted">暂无学生数据或无权限查看。</div> : null}
      </div>
    </div>
  );
}

