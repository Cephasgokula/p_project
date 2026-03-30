import axios from "axios";

const api = axios.create({
  baseURL: process.env.REACT_APP_API_URL || "http://localhost:8080/api/v1",
  headers: { "Content-Type": "application/json" },
});

api.interceptors.request.use((cfg) => {
  const token = localStorage.getItem("token");
  if (token && cfg.headers) {
    cfg.headers.Authorization = `Bearer ${token}`;
  }
  return cfg;
});

export const submitApplication = (data: object) =>
  api.post("/applications", data).then((r) => r.data);

export const getApplication = (id: string) =>
  api.get(`/applications/${encodeURIComponent(id)}`).then((r) => r.data);

export const getDecision = (id: string) =>
  api.get(`/applications/${encodeURIComponent(id)}/decision`).then((r) => r.data);

export default api;
