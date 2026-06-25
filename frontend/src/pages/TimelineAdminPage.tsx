import { showError } from "../components/ErrorModal";
import { useEffect, useState } from "react";
import { apiFetch } from "../api";
import { useAuth } from "../auth";
import { useNavigate } from "react-router-dom";

type TimelineNode = {
  stageCode: string;
  stageName: string;
  startTime: string | null;
  endTime: string | null;
};

export function TimelineAdminPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [type, setType] = useState<"PARTY_APPLY" | "LEAGUE_APPLY">("PARTY_APPLY");
  const [nodes, setNodes] = useState<TimelineNode[]>([]);
  const [loading, setLoading] = useState(false);
  
  useEffect(() => {
    if (!user || (!user.roles.includes("ADMIN") && !user.roles.includes("LEADER"))) {
      navigate("/");
    }
  }, [user, navigate]);

  useEffect(() => {
    void fetchConfig();
  }, [type]);

  async function fetchConfig() {
    setLoading(true);
        try {
      const data = await apiFetch<any[]>(`/timeline-config/${type}`, { method: "GET" });
      const mapped = data.map(n => ({
        stageCode: n.stageCode,
        stageName: n.stageName,
        startTime: n.startTime,
        endTime: n.endTime
      }));
      setNodes(mapped);
    } catch (e) {
      showError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }

  async function saveConfig() {
    setLoading(true);
        try {
      const payload = nodes.map(n => {
        let st = null;
        let et = null;
        try {
          if (n.startTime) st = new Date(n.startTime).toISOString();
          if (n.endTime) et = new Date(n.endTime).toISOString();
        } catch(err) {
          console.error("invalid date", err);
        }
        return {
          ...n,
          startTime: st,
          endTime: et,
        };
      });
      await apiFetch(`/timeline-config/${type}`, {
        method: "PUT",
        body: JSON.stringify(payload),
      });
      alert("保存成功！");
    } catch (e) {
      showError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }

  function formatDateTimeLocal(iso: string | null) {
    if (!iso) return "";
    const d = new Date(iso);
    const pad = (n: number) => n.toString().padStart(2, '0');
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  }

  function handleNodeChange(index: number, field: keyof TimelineNode, value: string | null) {
    const newNodes = [...nodes];
    newNodes[index] = { ...newNodes[index], [field]: value };
    setNodes(newNodes);
  }

  function addNode() {
    setNodes([...nodes, { stageCode: "", stageName: "", startTime: null, endTime: null }]);
  }

  function removeNode(index: number) {
    setNodes(nodes.filter((_, i) => i !== index));
  }

  return (
    <div className="grid">
      <div className="pageTitle">
        <h2>党团时间线配置</h2>
      </div>

      <div className="card">
        <div className="cardHeader">
          <div className="row" style={{ gap: 10 }}>
            <button className={`btn ${type === "PARTY_APPLY" ? "btnPrimary" : ""}`} onClick={() => setType("PARTY_APPLY")}>
              入党流程
            </button>
            <button className={`btn ${type === "LEAGUE_APPLY" ? "btnPrimary" : ""}`} onClick={() => setType("LEAGUE_APPLY")}>
              入团流程
            </button>
          </div>
          <button className="btn btnPrimary" onClick={() => void saveConfig()} disabled={loading}>
            保存配置
          </button>
        </div>
        <div className="cardBody">
          {loading ? (
            <div className="muted">加载中...</div>
          ) : (
            <div className="grid" style={{ gap: 12 }}>
              {nodes.map((node, index) => (
                <div key={index} className="row" style={{ gap: 10, alignItems: "center" }}>
                  <div className="badge">{index + 1}</div>
                  <input
                    type="text"
                    className="input"
                    placeholder="阶段代码 (如 APPLICANT)"
                    value={node.stageCode}
                    onChange={(e) => handleNodeChange(index, "stageCode", e.target.value)}
                  />
                  <input
                    type="text"
                    className="input"
                    placeholder="阶段名称 (如 入党申请人)"
                    value={node.stageName}
                    onChange={(e) => handleNodeChange(index, "stageName", e.target.value)}
                  />
                  <div className="row" style={{ gap: 4, alignItems: "center" }}>
                    <input
                      type="datetime-local"
                      className="input"
                      style={{ width: 180 }}
                      value={formatDateTimeLocal(node.startTime)}
                      onChange={(e) => handleNodeChange(index, "startTime", e.target.value || null)}
                    />
                    <span className="muted">至</span>
                    <input
                      type="datetime-local"
                      className="input"
                      style={{ width: 180 }}
                      value={formatDateTimeLocal(node.endTime)}
                      onChange={(e) => handleNodeChange(index, "endTime", e.target.value || null)}
                    />
                  </div>
                  <button className="btn btnDanger" onClick={() => removeNode(index)}>
                    删除
                  </button>
                </div>
              ))}
              <div className="row">
                <button className="btn" onClick={addNode}>
                  + 添加节点
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
