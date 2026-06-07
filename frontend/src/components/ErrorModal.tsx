import { useEffect, useState } from "react";

export const errorEmitter = new EventTarget();

export function showError(msg: string) {
  errorEmitter.dispatchEvent(new CustomEvent("error", { detail: msg }));
}

export function ErrorModal() {
  const [msg, setMsg] = useState("");

  useEffect(() => {
    const handler = (e: Event) => {
      setMsg((e as CustomEvent).detail);
    };
    errorEmitter.addEventListener("error", handler);
    return () => errorEmitter.removeEventListener("error", handler);
  }, []);

  if (!msg) return null;

  return (
    <div style={{
      position: "fixed", top: 0, left: 0, right: 0, bottom: 0,
      backgroundColor: "rgba(0,0,0,0.5)",
      display: "flex", justifyContent: "center", alignItems: "center",
      zIndex: 9999
    }}>
      <div style={{
        background: "var(--surface, #fff)",
        padding: "20px 30px",
        borderRadius: 8,
        minWidth: 300,
        maxWidth: "80%",
        boxShadow: "0 4px 12px rgba(0,0,0,0.15)",
        color: "var(--text, #333)",
        display: "flex",
        flexDirection: "column",
        gap: 16
      }}>
        <h3 style={{ margin: 0, color: "var(--danger, #dc2626)", fontSize: 18 }}>错误提示</h3>
        <p style={{ margin: 0, lineHeight: 1.5, wordBreak: "break-word" }}>{msg}</p>
        <div style={{ textAlign: "right" }}>
          <button 
            className="btn btnPrimary" 
            onClick={() => setMsg("")}
          >
            关闭
          </button>
        </div>
      </div>
    </div>
  );
}
