export type ApiResponse<T> = { code: number; message: string; data: T };

const API_BASE = import.meta.env.VITE_API_BASE ?? "/api";

export function getToken(): string | null {
  return localStorage.getItem("token");
}

export function setToken(token: string | null) {
  if (!token) {
    localStorage.removeItem("token");
  } else {
    localStorage.setItem("token", token);
  }
}

export async function apiFetch<T>(
  path: string,
  init: RequestInit & { raw?: boolean } = {},
): Promise<T> {
  const url = path.startsWith("http") ? path : `${API_BASE}${path}`;
  const token = getToken();

  const headers = new Headers(init.headers);
  headers.set("Accept", "application/json");
  if (!(init.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const resp = await fetch(url, { ...init, headers });
  if (init.raw) {
    return resp as unknown as T;
  }

  let json: any;
  try {
    const text = await resp.text();
    if (!text) {
      let msg = resp.ok ? "OK" : `请求失败 (HTTP ${resp.status})`;
      if (resp.status === 502 || resp.status === 504) {
        msg = "后端服务正在启动或不可用，请稍候重试。";
      }
      json = { code: resp.ok ? 0 : resp.status, message: msg, data: null };
    } else if (text.trim().startsWith("<")) {
      let errorMsg = `服务器错误 (${resp.status}): 请稍后重试。`;
      if (resp.status === 502 || resp.status === 504) {
        errorMsg = "后端服务正在启动或不可用，请稍候重试。";
      } else if (resp.status === 404) {
        errorMsg = "未找到API接口，请检查配置。";
      } else if (resp.status === 405) {
        errorMsg = "请求方法不允许，请检查Nginx配置。";
      }
      throw new Error(errorMsg);
    } else {
      json = JSON.parse(text);
    }
  } catch (e) {
    if (e instanceof Error && (e.message.includes("服务器错误") || e.message.includes("后端服务") || e.message.includes("API接口") || e.message.includes("请求方法"))) {
      throw e;
    }
    throw new Error(`无效的JSON响应: ${e}`);
  }

  if (!resp.ok || json.code !== 0) {
    throw new Error(json.message || `请求失败 (HTTP ${resp.status})`);
  }
  return json.data;
}

export async function apiFetchBlob(path: string): Promise<Blob> {
  const url = path.startsWith("http") ? path : `${API_BASE}${path}`;
  const token = getToken();

  const headers = new Headers();
  if (token) headers.set("Authorization", `Bearer ${token}`);

  const resp = await fetch(url, { method: "GET", headers });
  if (!resp.ok) {
    throw new Error(`请求失败 (HTTP ${resp.status})`);
  }
  return await resp.blob();
}

export function apiFetchWithProgress<T>(
  path: string,
  fd: FormData,
  onProgress: (pct: number) => void
): Promise<T> {
  const url = path.startsWith("http") ? path : `${API_BASE}${path}`;
  const token = getToken();

  return new Promise((resolve, reject) => {
    const xhr = new XMLHttpRequest();
    xhr.open("POST", url);
    xhr.setRequestHeader("Accept", "application/json");
    if (token) {
      xhr.setRequestHeader("Authorization", `Bearer ${token}`);
    }

    xhr.upload.onprogress = (e) => {
      if (e.lengthComputable) {
        onProgress(Math.round((e.loaded / e.total) * 100));
      }
    };

    xhr.onload = () => {
      try {
        const json = JSON.parse(xhr.responseText) as ApiResponse<T>;
        if (xhr.status >= 200 && xhr.status < 300 && json.code === 0) {
          resolve(json.data);
        } else {
          reject(new Error(json.message || `请求失败 (HTTP ${xhr.status})`));
        }
      } catch (e) {
        reject(new Error(`请求失败 (HTTP ${xhr.status})`));
      }
    };

    xhr.onerror = () => reject(new Error("网络错误 (Network Error)"));
    xhr.send(fd);
  });
}
