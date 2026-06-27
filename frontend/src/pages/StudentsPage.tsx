import { showError } from "../components/ErrorModal";
import { useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { apiFetch, getToken } from "../api";
import { useAuth } from "../auth";

type StudentDto = {
  id: number;
  realName: string | null;
  studentNo: string | null;
  major: string | null;
  grade: number | null;
  className: string | null;
};

export function StudentsPage() {
  const { hasRole } = useAuth();
  const [items, setItems] = useState<StudentDto[]>([]);
  const [q, setQ] = useState("");
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [showAddModal, setShowAddModal] = useState(false);
  const [addForm, setAddForm] = useState({ studentNo: "", realName: "", major: "", grade: "", className: "" });

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

  async function handleImport(e: React.ChangeEvent<HTMLInputElement>) {
    const file = e.target.files?.[0];
    if (!file) return;
    const formData = new FormData();
    formData.append("file", file);
    try {
      const token = getToken();
      const res = await fetch("/api/students/import", {
        method: "POST",
        headers: token ? { Authorization: `Bearer ${token}` } : {},
        body: formData,
      });
      const data = await res.json();
      if (data.code !== 200) {
        throw new Error(data.message || "导入失败");
      }
      await refresh();
      if (fileInputRef.current) fileInputRef.current.value = "";
      alert("导入成功");
    } catch (err) {
      showError((err as Error).message);
    } finally {
      if (fileInputRef.current) fileInputRef.current.value = "";
    }
  }

  function handleExport() {
    const token = getToken();
    const url = "/api/students/export";
    fetch(url, { headers: token ? { Authorization: `Bearer ${token}` } : {} })
      .then((res) => {
        if (!res.ok) throw new Error("导出失败");
        return res.blob();
      })
      .then((blob) => {
        const a = document.createElement("a");
        a.href = URL.createObjectURL(blob);
        a.download = "students.xlsx";
        a.click();
      })
      .catch((err) => showError(err.message));
  }

  async function handleAdd() {
    try {
      const gradeInt = parseInt(addForm.grade);
      await apiFetch("/students", {
        method: "POST",
        body: JSON.stringify({
          ...addForm,
          grade: isNaN(gradeInt) ? null : gradeInt,
        }),
      });
      setShowAddModal(false);
      setAddForm({ studentNo: "", realName: "", major: "", grade: "", className: "" });
      await refresh();
    } catch (e) {
      showError((e as Error).message);
    }
  }

  return (
    <div className="grid">
      <div className="pageTitle">
        <h2>我的学生</h2>
        <div className="row" style={{ gap: 8 }}>
          {hasRole("LEADER") && (
            <>
              <button className="btn btnPrimary" onClick={() => setShowAddModal(true)}>录入</button>
              <button className="btn" onClick={() => fileInputRef.current?.click()}>导入Excel</button>
              <input type="file" ref={fileInputRef} style={{ display: "none" }} accept=".xlsx,.xls" onChange={(e) => void handleImport(e)} />
            </>
          )}
          <button className="btn" onClick={handleExport}>导出Excel</button>
          <button className="btn" onClick={() => void refresh()}>刷新</button>
        </div>
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

      {showAddModal && (
        <div style={{ position: "fixed", top: 0, left: 0, right: 0, bottom: 0, background: "rgba(0,0,0,0.5)", zIndex: 100, display: "flex", alignItems: "center", justifyContent: "center" }}>
          <div className="card" style={{ width: 400, maxWidth: "90%" }}>
            <div className="cardBody grid" style={{ gap: 12 }}>
              <h3>录入学生信息</h3>
              <input className="input" placeholder="学号" value={addForm.studentNo} onChange={(e) => setAddForm({ ...addForm, studentNo: e.target.value })} />
              <input className="input" placeholder="姓名" value={addForm.realName} onChange={(e) => setAddForm({ ...addForm, realName: e.target.value })} />
              <input className="input" placeholder="专业" value={addForm.major} onChange={(e) => setAddForm({ ...addForm, major: e.target.value })} />
              <input className="input" placeholder="年级" value={addForm.grade} onChange={(e) => setAddForm({ ...addForm, grade: e.target.value })} />
              <input className="input" placeholder="班级" value={addForm.className} onChange={(e) => setAddForm({ ...addForm, className: e.target.value })} />
              <div className="row" style={{ justifyContent: "flex-end", gap: 8, marginTop: 12 }}>
                <button className="btn" onClick={() => setShowAddModal(false)}>取消</button>
                <button className="btn btnPrimary" onClick={() => void handleAdd()}>保存</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

