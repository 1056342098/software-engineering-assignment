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

  const json = (await resp.json()) as ApiResponse<T>;
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
