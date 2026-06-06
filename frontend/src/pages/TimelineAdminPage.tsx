import { useEffect, useState } from "react";
import { apiFetch } from "../api";
import { useAuth } from "../auth";
import { useNavigate } from "react-router-dom";

type TimelineNode = {
  stageCode: string;
  stageName: string;
  intervalDays: number | string;
};

export function TimelineAdminPage() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [type, setType] = useState<"PARTY_APPLY" | "LEAGUE_APPLY">("PARTY_APPLY");
  const [nodes, setNodes] = useState<TimelineNode[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

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
    setError(null);
    try {
      const data = await apiFetch<TimelineNode[]>(`/timeline-config/${type}`, { method: "GET" });
      setNodes(data);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }

  async function saveConfig() {
    setLoading(true);
    setError(null);
    try {
      const payload = nodes.map(n => ({
        ...n,
        intervalDays: typeof n.intervalDays === "string" ? 0 : n.intervalDays
      }));
      await apiFetch(`/timeline-config/${type}`, {
        method: "PUT",
        body: JSON.stringify(payload),
      });
      alert("保存成功！");
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }

  function handleNodeChange(index: number, field: keyof TimelineNode, value: string | number) {
    const newNodes = [...nodes];
    newNodes[index] = { ...newNodes[index], [field]: value };
    setNodes(newNodes);
  }

  function addNode() {
    setNodes([...nodes, { stageCode: "", stageName: "", intervalDays: 0 }]);
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
          {error ? <div style={{ color: "var(--danger)", marginBottom: 10 }}>{error}</div> : null}
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
                      type="number"
                      className="input"
                      style={{ width: 80 }}
                      value={node.intervalDays}
                      onChange={(e) => {
                        const val = e.target.value;
                        handleNodeChange(index, "intervalDays", val === "" ? "" : parseInt(val) || 0);
                      }}
                    />
                    <span className="muted">天后可进入下一阶段</span>
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
