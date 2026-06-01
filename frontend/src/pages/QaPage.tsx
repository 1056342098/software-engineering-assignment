import { useState } from "react";
import { apiFetch } from "../api";

type AskResp = { answer: string; source: { docId: number; title: string; chunkNo: number } | null };

export function QaPage() {
  const [question, setQuestion] = useState("");
  const [answer, setAnswer] = useState<AskResp | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  async function ask() {
    setError(null);
    setAnswer(null);
    setLoading(true);
    try {
      const data = await apiFetch<AskResp>("/qa/ask", {
        method: "POST",
        body: JSON.stringify({ question, topK: 5 }),
      });
      setAnswer(data);
    } catch (e) {
      setError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="grid">
      <div className="pageTitle">
        <h2>智能问答</h2>
        <span className="muted" style={{ fontSize: 12 }}>
          基于政策库检索返回答案片段
        </span>
      </div>

      <div className="card">
        <div className="cardBody">
          <div className="row" style={{ alignItems: "stretch" }}>
            <input
              className="input"
              value={question}
              onChange={(e) => setQuestion(e.target.value)}
              placeholder="例如：入党申请需要哪些材料？"
              style={{ flex: 1, minWidth: 240 }}
            />
            <button className="btn btnPrimary" onClick={ask} disabled={!question.trim() || loading}>
              {loading ? "查询中…" : "查询"}
            </button>
          </div>
          {error ? <div style={{ marginTop: 10, color: "var(--danger)" }}>{error}</div> : null}
        </div>
      </div>

      {answer ? (
        <div className="card">
          <div className="cardHeader">
            <div style={{ fontWeight: 600 }}>检索结果</div>
            {answer.source ? <span className="badge">docId={answer.source.docId}</span> : <span className="badge">无匹配</span>}
          </div>
          <div className="cardBody">
            <div style={{ whiteSpace: "pre-wrap", lineHeight: 1.6 }}>{answer.answer}</div>
            {answer.source ? (
              <div className="muted" style={{ marginTop: 10, fontSize: 12 }}>
                来源：{answer.source.title}（chunkNo={answer.source.chunkNo}）
              </div>
            ) : null}
          </div>
        </div>
      ) : null}
    </div>
  );
}
