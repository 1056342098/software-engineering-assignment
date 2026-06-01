import { Navigate, Outlet } from "react-router-dom";
import { useAuth } from "../auth";

export function RequireAuth() {
  const auth = useAuth();
  if (auth.isLoading) return <div>加载中…</div>;
  if (!auth.user) return <Navigate to="/login" replace />;
  return <Outlet />;
}

