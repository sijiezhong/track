import axios from "axios";
import { toast } from "sonner";

export const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "",
  timeout: 15000,
});

api.interceptors.response.use(
  (r) => r,
  (err) => {
    const data = err.response?.data;
    const msg = data?.message || "请求失败";
    const traceId = data?.traceId;
    if (err.response?.status === 429) {
      const retryAfter =
        Number(err.response.headers?.["retry-after"]) || undefined;
      toast.warning(
        `频率受限${retryAfter ? `，请在 ${retryAfter}s 后重试` : ""}`,
      );
    }
    toast.error(traceId ? `${msg}（traceId: ${traceId}）` : msg);
    return Promise.reject(err);
  },
);
