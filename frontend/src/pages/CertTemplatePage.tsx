import { useEffect, useState } from "react";
import { apiFetch } from "../api";
import { useAuth } from "../auth";
import { showError } from "../components/ErrorModal";

type Template = {
  id: number;
  name: string;
  content: string;
  enabled: boolean;
};

export function CertTemplatePage() {
  const { user } = useAuth();
  const [templates, setTemplates] = useState<Template[]>([]);
  const [loading, setLoading] = useState(true);

  const [showModal, setShowModal] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [formName, setFormName] = useState("");
  const [formContent, setFormContent] = useState("");
  const [formEnabled, setFormEnabled] = useState(true);

  const loadTemplates = async () => {
    try {
      const res = await apiFetch("/api/certificates/templates");
      setTemplates(res.data || []);
    } catch (e: any) {
      showError(e.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTemplates();
  }, []);

  const openAdd = () => {
    setEditingId(null);
    setFormName("");
    setFormContent("");
    setFormEnabled(true);
    setShowModal(true);
  };

  const openEdit = (t: Template) => {
    setEditingId(t.id);
    setFormName(t.name);
    setFormContent(t.content);
    setFormEnabled(t.enabled);
    setShowModal(true);
  };

  const saveTemplate = async () => {
    if (!formName.trim() || !formContent.trim()) {
      showError("名称和内容不能为空");
      return;
    }
    try {
      if (editingId) {
        await apiFetch(`/api/certificates/templates/${editingId}`, "POST", {
          name: formName,
          content: formContent,
          enabled: formEnabled,
        });
      } else {
        await apiFetch("/api/certificates/templates", "POST", {
          name: formName,
          content: formContent,
        });
      }
      setShowModal(false);
      loadTemplates();
    } catch (e: any) {
      showError(e.message);
    }
  };

  const deleteTemplate = async (id: number) => {
    if (!confirm("确认删除该模板吗？")) return;
    try {
      await apiFetch(`/api/certificates/templates/${id}`, "DELETE");
      loadTemplates();
    } catch (e: any) {
      showError(e.message);
    }
  };

  if (loading) return <div>加载中...</div>;

  return (
    <div className="grid">
      <div className="pageTitle">
        <h2>证明模板管理</h2>
        {user?.roles.some((r) => r === "LEADER" || r === "TEACHER") && (
          <button className="btn btnPrimary" onClick={openAdd}>
            新建模板
          </button>
        )}
      </div>

      <div className="card">
        <div className="cardBody" style={{ padding: 0 }}>
          <table className="table">
            <thead>
              <tr>
                <th>ID</th>
                <th>名称</th>
                <th>状态</th>
                <th>操作</th>
              </tr>
            </thead>
            <tbody>
              {templates.map((t) => (
                <tr key={t.id}>
                  <td>{t.id}</td>
                  <td>{t.name}</td>
                  <td>
                    <span className={`badge ${t.enabled ? "badgeSuccess" : "badgeWarning"}`}>
                      {t.enabled ? "启用" : "禁用"}
                    </span>
                  </td>
                  <td>
                    <button className="btn" onClick={() => openEdit(t)} style={{ marginRight: 8 }}>
                      编辑
                    </button>
                    <button className="btn btnDanger" onClick={() => deleteTemplate(t.id)}>
                      删除
                    </button>
                  </td>
                </tr>
              ))}
              {templates.length === 0 && (
                <tr>
                  <td colSpan={4} style={{ textAlign: "center", color: "var(--text-muted)" }}>
                    暂无模板
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>

      {showModal && (
        <div className="modalWrap">
          <div className="modal" style={{ width: 600 }}>
            <div className="modalHeader">{editingId ? "编辑模板" : "新建模板"}</div>
            <div className="modalBody grid">
              <div className="formGroup">
                <label>模板名称</label>
                <input
                  className="input"
                  value={formName}
                  onChange={(e) => setFormName(e.target.value)}
                  placeholder="如：在读证明"
                />
              </div>
              <div className="formGroup">
                <label>模板内容（支持变量：{`\${realName}, \${studentNo}, \${major}, \${grade}, \${className}`} 等）</label>
                <textarea
                  className="input"
                  style={{ height: 200, fontFamily: "monospace" }}
                  value={formContent}
                  onChange={(e) => setFormContent(e.target.value)}
                />
              </div>
              {editingId && (
                <div className="formGroup">
                  <label className="row" style={{ gap: 8, justifyContent: "flex-start" }}>
                    <input
                      type="checkbox"
                      checked={formEnabled}
                      onChange={(e) => setFormEnabled(e.target.checked)}
                    />
                    启用该模板
                  </label>
                </div>
              )}
            </div>
            <div className="modalFooter">
              <button className="btn" onClick={() => setShowModal(false)}>
                取消
              </button>
              <button className="btn btnPrimary" onClick={saveTemplate}>
                保存
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
