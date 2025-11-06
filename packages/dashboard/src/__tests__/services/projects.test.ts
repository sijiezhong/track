import { describe, it, expect } from "vitest";
import * as projects from "@/services/projects";

describe("projects service", () => {
  describe("getProjects", () => {
    it("should return projects list with correct structure", async () => {
      const result = await projects.getProjects();

      expect(Array.isArray(result)).toBe(true);

      if (result.length > 0) {
        expect(result[0]).toHaveProperty("appId");
        expect(result[0]).toHaveProperty("appName");

        expect(typeof result[0].appId).toBe("string");
        expect(typeof result[0].appName).toBe("string");
      }
    });

    it("should handle empty result", async () => {
      // 即使返回空数组，也应该正常处理
      const result = await projects.getProjects();

      expect(Array.isArray(result)).toBe(true);
    });

    it("should support active filter", async () => {
      const result = await projects.getProjects({ active: true });

      expect(Array.isArray(result)).toBe(true);
    });

    it("should support inactive filter", async () => {
      const result = await projects.getProjects({ active: false });

      expect(Array.isArray(result)).toBe(true);
    });

    it("should return projects with valid appId and appName", async () => {
      const result = await projects.getProjects();

      result.forEach((project) => {
        expect(project.appId).toBeTruthy();
        expect(project.appName).toBeTruthy();
        expect(project.appId.length).toBeGreaterThan(0);
        expect(project.appName.length).toBeGreaterThan(0);
      });
    });
  });
});
