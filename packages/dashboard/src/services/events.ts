import { api } from "./http";

export async function getEvents(params: {
  appId?: string;
  start?: string;
  end?: string;
  type?: number;
  keyword?: string;
  page?: number;
  size?: number;
}) {
  const { data } = await api.get("/api/events", { params });
  return data as {
    items: Record<string, any>[];
    page: { index: number; size: number; total: number };
  };
}
