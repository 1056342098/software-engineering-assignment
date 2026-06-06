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
      json = { code: 0, message: "OK", data: null };
    } else if (text.trim().startsWith("<")) {
      let errorMsg = `Server Error (${resp.status}): Please try again later.`;
      if (resp.status === 502 || resp.status === 504) {
        errorMsg = "Backend server is starting or unavailable, please wait a moment and try again.";
      } else if (resp.status === 404) {
        errorMsg = "API endpoint not found. Please check your configuration.";
      } else if (resp.status === 405) {
        errorMsg = "Method not allowed. Please check your Nginx configuration.";
      }
      throw new Error(errorMsg);
    } else {
      json = JSON.parse(text);
    }
  } catch (e) {
    if (e instanceof Error && (e.message.includes("Server Error") || e.message.includes("Backend server") || e.message.includes("API endpoint") || e.message.includes("Method not allowed"))) {
      throw e;
    }
    throw new Error(`Invalid JSON response: ${e}`);
  }

  if (!resp.ok || json.code !== 0) {
    throw new Error(json.message || `HTTP_${resp.status}`);
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
    throw new Error(`HTTP_${resp.status}`);
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
          reject(new Error(json.message || `HTTP_${xhr.status}`));
        }
      } catch (e) {
        reject(new Error(`HTTP_${xhr.status}`));
      }
    };

    xhr.onerror = () => reject(new Error("Network Error"));
    xhr.send(fd);
  });
}
