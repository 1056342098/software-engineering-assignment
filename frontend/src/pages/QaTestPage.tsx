import { showError } from "../components/ErrorModal";
import { useEffect, useState } from "react";
import { apiFetch } from "../api";

type QuestionDto = {
  id: number;
  content: string;
  options: string[];
  correctAnswer: string;
};

export function QaTestPage() {
  const [type, setType] = useState<"PARTY" | "LEAGUE">("PARTY");
  const [questions, setQuestions] = useState<QuestionDto[]>([]);
  const [answers, setAnswers] = useState<Record<number, string>>({});
  const [submitted, setSubmitted] = useState(false);
  const [score, setScore] = useState(0);
  const [loading, setLoading] = useState(false);
  
  useEffect(() => {
    void fetchQuestions();
  }, [type]);

  async function fetchQuestions() {
    setLoading(true);
        setAnswers({});
    setSubmitted(false);
    setScore(0);
    try {
      const data = await apiFetch<QuestionDto[]>(`/selftest/questions/${type}`, { method: "GET" });
      setQuestions(data);
    } catch (e) {
      showError((e as Error).message);
    } finally {
      setLoading(false);
    }
  }

  function handleSelect(questionId: number, option: string) {
    if (submitted) return;
    setAnswers((prev) => ({ ...prev, [questionId]: option }));
  }

  function submit() {
    let currentScore = 0;
    for (const q of questions) {
      if (answers[q.id] === q.correctAnswer) {
        currentScore++;
      }
    }
    setScore(Math.round((currentScore / questions.length) * 100));
    setSubmitted(true);
  }

  return (
    <div className="grid">
      <div className="pageTitle">
        <h2>党团理论自测</h2>
      </div>

      <div className="card">
        <div className="cardHeader">
          <div className="row" style={{ gap: 10 }}>
            <button className={`btn ${type === "PARTY" ? "btnPrimary" : ""}`} onClick={() => setType("PARTY")}>
              党建理论测试
            </button>
            <button className={`btn ${type === "LEAGUE" ? "btnPrimary" : ""}`} onClick={() => setType("LEAGUE")}>
              团建理论测试
            </button>
          </div>
          <button className="btn" onClick={() => void fetchQuestions()}>
            重新加载
          </button>
        </div>
        <div className="cardBody">
          {loading ? (
            <div className="muted">加载中...</div>
          ) : questions.length === 0 ? (
            <div className="muted">暂无题库数据。</div>
          ) : (
            <div className="grid" style={{ gap: 24 }}>
              {submitted && (
                <div className="card" style={{ padding: 16, background: "var(--surface-2)", textAlign: "center" }}>
                  <h3 style={{ margin: 0, color: score >= 60 ? "var(--ok)" : "var(--danger)" }}>
                    你的得分：{score} 分
                  </h3>
                </div>
              )}
              
              {questions.map((q, i) => (
                <div key={q.id} className="grid" style={{ gap: 12 }}>
                  <div style={{ fontWeight: 600 }}>
                    {i + 1}. {q.content}
                  </div>
                  <div className="grid" style={{ gap: 8 }}>
                    {q.options.map((opt) => {
                      const isSelected = answers[q.id] === opt;
                      const isCorrect = q.correctAnswer === opt;
                      let badgeClass = "badge";
                      if (submitted) {
                        if (isCorrect) badgeClass = "badge badgeOk";
                        else if (isSelected && !isCorrect) badgeClass = "badge badgeDanger";
                      } else {
                        if (isSelected) badgeClass = "badge badgePrimary";
                      }

                      return (
                        <div
                          key={opt}
                          className="row"
                          style={{
                            gap: 10,
                            padding: "8px 12px",
                            background: "var(--surface-2)",
                            borderRadius: 6,
                            cursor: submitted ? "default" : "pointer",
                            border: isSelected ? "1px solid var(--primary)" : "1px solid transparent",
                          }}
                          onClick={() => handleSelect(q.id, opt)}
                        >
                          <div
                            style={{
                              width: 16,
                              height: 16,
                              borderRadius: 8,
                              border: isSelected ? "4px solid var(--primary)" : "1px solid var(--border)",
                              background: "var(--surface)",
                            }}
                          />
                          <span className={badgeClass}>{opt}</span>
                        </div>
                      );
                    })}
                  </div>
                </div>
              ))}

              {!submitted && (
                <div className="row" style={{ justifyContent: "center", marginTop: 20 }}>
                  <button className="btn btnPrimary" style={{ padding: "10px 40px" }} onClick={submit}>
                    提交试卷
                  </button>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
