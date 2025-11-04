import { describe, it, expect, beforeEach, vi } from "vitest";
import { Storage } from "../../core/storage";
import { UserConfig, TrackConfig, EventData, EventType } from "../../types";
import * as utils from "../../utils";

describe("Storage", () => {
  let storage: Storage;

  beforeEach(() => {
    storage = new Storage();
    // 清理 localStorage
    localStorage.clear();
  });

  describe("UserConfig 存储", () => {
    it("should save and retrieve user config correctly", () => {
      const config: UserConfig = {
        appId: "test-app",
        userId: "user-123",
        userProps: { plan: "premium" },
      };

      storage.saveUserConfig(config);
      const retrieved = storage.getUserConfig();

      expect(retrieved).toEqual(config);
    });

    it("should return null when user config does not exist", () => {
      const retrieved = storage.getUserConfig();
      expect(retrieved).toBeNull();
    });

    it("should handle invalid JSON gracefully", () => {
      localStorage.setItem("__track_user_config__", "invalid json");

      const retrieved = storage.getUserConfig();
      expect(retrieved).toBeNull();
    });
  });

  describe("TrackConfig 存储", () => {
    it("should save and retrieve track config correctly", () => {
      const config: TrackConfig = {
        endpoint: "http://localhost:8080",
        sessionTTL: 1440,
        autoTrack: true,
        batchSize: 20,
      };

      storage.saveTrackConfig(config);
      const retrieved = storage.getTrackConfig();

      expect(retrieved).toEqual(config);
    });

    it("should return null when track config does not exist", () => {
      const retrieved = storage.getTrackConfig();
      expect(retrieved).toBeNull();
    });
  });

  describe("离线事件队列", () => {
    it("should save offline events", async () => {
      const events: EventData[] = [
        {
          type: EventType.CUSTOM,
          eventId: "event1",
          properties: { key: "value1" },
        },
        {
          type: EventType.CUSTOM,
          eventId: "event2",
          properties: { key: "value2" },
        },
      ];

      await storage.saveOfflineEvents(events);
      const retrieved = storage.getAllOfflineEvents();

      expect(retrieved).toEqual(events);
    });

    it("should append to existing offline events", async () => {
      const events1: EventData[] = [
        {
          type: EventType.CUSTOM,
          eventId: "event1",
          properties: {},
        },
      ];
      const events2: EventData[] = [
        {
          type: EventType.CUSTOM,
          eventId: "event2",
          properties: {},
        },
      ];

      await storage.saveOfflineEvents(events1);
      await storage.saveOfflineEvents(events2);
      const retrieved = storage.getAllOfflineEvents();

      expect(retrieved).toHaveLength(2);
      expect(retrieved[0].eventId).toBe("event1");
      expect(retrieved[1].eventId).toBe("event2");
    });

    it("should limit offline events to max 1000", async () => {
      const events: EventData[] = Array.from({ length: 1200 }, (_, i) => ({
        type: EventType.CUSTOM,
        eventId: `event-${i}`,
        properties: {},
      }));

      await storage.saveOfflineEvents(events);
      const retrieved = storage.getAllOfflineEvents();

      expect(retrieved).toHaveLength(1000);
      expect(retrieved[0].eventId).toBe("event-200"); // 保留最后 1000 个
      expect(retrieved[999].eventId).toBe("event-1199");
    });

    it("should clear offline events", async () => {
      const events: EventData[] = [
        {
          type: EventType.CUSTOM,
          eventId: "event1",
          properties: {},
        },
      ];

      await storage.saveOfflineEvents(events);
      await storage.clearOfflineEvents();
      const retrieved = storage.getAllOfflineEvents();

      expect(retrieved).toHaveLength(0);
    });

    it("should return empty array when offline events do not exist", () => {
      const retrieved = storage.getAllOfflineEvents();
      expect(retrieved).toEqual([]);
    });
  });

  describe("LocalStorage 不可用时的降级处理", () => {
    it("should handle gracefully when localStorage is not available", () => {
      // Mock isLocalStorageAvailable to return false
      vi.spyOn(utils, "isLocalStorageAvailable").mockReturnValue(false);

      const storageNoLocalStorage = new Storage();
      const config: UserConfig = {
        appId: "test-app",
        userId: "user-123",
      };

      // 应该静默处理，不抛出错误
      expect(() => {
        storageNoLocalStorage.saveUserConfig(config);
        storageNoLocalStorage.saveTrackConfig({ endpoint: "http://test.com" });
        storageNoLocalStorage.saveOfflineEvents([]);
      }).not.toThrow();

      expect(storageNoLocalStorage.getUserConfig()).toBeNull();
      expect(storageNoLocalStorage.getTrackConfig()).toBeNull();
      expect(storageNoLocalStorage.getAllOfflineEvents()).toEqual([]);
    });

    it("should handle localStorage quota exceeded error", () => {
      const setItemSpy = vi.spyOn(Storage.prototype, "saveUserConfig" as any);

      // Mock localStorage.setItem to throw quota exceeded error
      const originalSetItem = localStorage.setItem;
      localStorage.setItem = vi.fn().mockImplementation(() => {
        throw new DOMException("QuotaExceededError", "QuotaExceededError");
      });

      const config: UserConfig = {
        appId: "test-app",
        userId: "user-123",
      };

      // 应该静默处理，不抛出错误
      expect(() => {
        storage.saveUserConfig(config);
      }).not.toThrow();

      localStorage.setItem = originalSetItem;
    });
  });

  describe("clearAll", () => {
    it("should clear all stored data", () => {
      storage.saveUserConfig({ appId: "test", userId: "user" });
      storage.saveTrackConfig({ endpoint: "http://test.com" });

      storage.clearAll();

      expect(storage.getUserConfig()).toBeNull();
      expect(storage.getTrackConfig()).toBeNull();
      expect(storage.getAllOfflineEvents()).toEqual([]);
    });
  });
});
