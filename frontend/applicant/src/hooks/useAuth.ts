import { useState, useCallback } from "react";
import axios from "axios";

interface AuthState {
  token: string | null;
  role: "applicant" | "lender" | "admin" | null;
  userId: string | null;
}

export function useAuth() {
  const [auth, setAuth] = useState<AuthState>({
    token: localStorage.getItem("token"),
    role: localStorage.getItem("role") as AuthState["role"],
    userId: localStorage.getItem("userId"),
  });

  const login = useCallback(async (email: string, password: string) => {
    const { data } = await axios.post("/api/v1/auth/login", { email, password });
    localStorage.setItem("token", data.token);
    localStorage.setItem("role", data.role);
    localStorage.setItem("userId", data.user_id);
    setAuth({ token: data.token, role: data.role, userId: data.user_id });
    axios.defaults.headers.common["Authorization"] = `Bearer ${data.token}`;
  }, []);

  const logout = useCallback(() => {
    localStorage.clear();
    setAuth({ token: null, role: null, userId: null });
    delete axios.defaults.headers.common["Authorization"];
  }, []);

  return { ...auth, login, logout };
}
