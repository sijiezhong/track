import { api } from "./http";

export type ProjectItem = { appId: string; appName: string };

export async function getProjects(params?: { active?: boolean }) {
  const { data } = await api.get("/api/projects", { params });
  // 期望结构：{ list: ProjectItem[] }
  return (data?.list || []) as ProjectItem[];
}
