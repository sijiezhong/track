import { describe, it, expect, vi, beforeEach, afterEach } from "vitest";
import { Sender } from "../../core/sender";
import { EventData, EventType } from "../../types";

describe("Sender", () => {
  let sender: Sender;
  const endpoint = "http://localhost:8080";
  let onSessionExpired: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    onSessionExpired = vi.fn();
    sender = new Sender(endpoint, onSessionExpired);

    // 清理全局 mock
    global.fetch = vi.fn();
    if (global.navigator) {
      (global.navigator as any).sendBeacon = vi.fn();
    }
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  describe("sendBeacon 优先使用", () => {
    it("should use sendBeacon when available and successful", async () => {
      const sendBeaconSpy = vi.fn().mockReturnValue(true);
      (global.navigator as any).sendBeacon = sendBeaconSpy;

      const events: EventData[] = [
        {
          type: EventType.CUSTOM,
          eventId: "test-event",
          properties: { key: "value" },
        },
      ];

      await sender.sendEvents(events);

      expect(sendBeaconSpy).toHaveBeenCalledTimes(1);
      expect(sendBeaconSpy).toHaveBeenCalledWith(
        `${endpoint}/api/ingest`,
        expect.any(Blob),
      );
      expect(global.fetch).not.toHaveBeenCalled();
    });

    it("should fallback to fetch when sendBeacon fails", async () => {
      const sendBeaconSpy = vi.fn().mockReturnValue(false);
      (global.navigator as any).sendBeacon = sendBeaconSpy;

      const fetchSpy = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
      } as Response);
      global.fetch = fetchSpy;

      const events: EventData[] = [
        {
          type: EventType.CUSTOM,
          eventId: "test-event",
          properties: { key: "value" },
        },
      ];

      await sender.sendEvents(events);

      expect(sendBeaconSpy).toHaveBeenCalledTimes(1);
      expect(fetchSpy).toHaveBeenCalledTimes(1);
      expect(fetchSpy).toHaveBeenCalledWith(
        `${endpoint}/api/ingest`,
        expect.objectContaining({
          method: "POST",
          mode: "cors",
          credentials: "include",
          headers: { "Content-Type": "application/json" },
          keepalive: true,
        }),
      );
    });

    it("should fallback to fetch when sendBeacon is not available", async () => {
      (global.navigator as any).sendBeacon = undefined;

      const fetchSpy = vi.fn().mockResolvedValue({
        ok: true,
        status: 200,
      } as Response);
      global.fetch = fetchSpy;

      const events: EventData[] = [
        {
          type: EventType.CUSTOM,
          eventId: "test-event",
          properties: { key: "value" },
        },
      ];

      await sender.sendEvents(events);

      expect(fetchSpy).toHaveBeenCalledTimes(1);
    });
  });

  describe("Session 失效处理", () => {
    it("should retry once after re-init on 401", async () => {
      // 第一次返回 401，重试后返回 200
      const fetchSpy = vi
        .fn()
        .mockResolvedValueOnce({
          ok: false,
          status: 401,
        } as Response)
        .mockResolvedValueOnce({
          ok: true,
          status: 200,
        } as Response);
      global.fetch = fetchSpy;
      (global.navigator as any).sendBeacon = undefined;

      // onSessionExpired 返回 Promise，模拟重新 init
      onSessionExpired = vi.fn().mockResolvedValue(undefined);
      sender = new Sender(endpoint, onSessionExpired);

      const events: EventData[] = [
        {
          type: EventType.CUSTOM,
          eventId: "test-event",
          properties: {},
        },
      ];

      await sender.sendEvents(events);

      // 验证 onSessionExpired 被调用
      expect(onSessionExpired).toHaveBeenCalledTimes(1);
      // 验证重试了一次（总共调用 2 次 fetch）
      expect(fetchSpy).toHaveBeenCalledTimes(2);
    });

    it("should retry once after re-init on 403", async () => {
      // 第一次返回 403，重试后返回 200
      const fetchSpy = vi
        .fn()
        .mockResolvedValueOnce({
          ok: false,
          status: 403,
        } as Response)
        .mockResolvedValueOnce({
          ok: true,
          status: 200,
        } as Response);
      global.fetch = fetchSpy;
      (global.navigator as any).sendBeacon = undefined;

      // onSessionExpired 返回 Promise，模拟重新 init
      onSessionExpired = vi.fn().mockResolvedValue(undefined);
      sender = new Sender(endpoint, onSessionExpired);

      const events: EventData[] = [
        {
          type: EventType.CUSTOM,
          eventId: "test-event",
          properties: {},
        },
      ];

      await sender.sendEvents(events);

      // 验证 onSessionExpired 被调用
      expect(onSessionExpired).toHaveBeenCalledTimes(1);
      // 验证重试了一次（总共调用 2 次 fetch）
      expect(fetchSpy).toHaveBeenCalledTimes(2);
    });

    it("should throw error if retry still fails with 401", async () => {
      // 两次都返回 401
      const fetchSpy = vi.fn().mockResolvedValue({
        ok: false,
        status: 401,
      } as Response);
      global.fetch = fetchSpy;
      (global.navigator as any).sendBeacon = undefined;

      // onSessionExpired 返回 Promise，模拟重新 init
      onSessionExpired = vi.fn().mockResolvedValue(undefined);
      sender = new Sender(endpoint, onSessionExpired);

      const events: EventData[] = [
        {
          type: EventType.CUSTOM,
          eventId: "test-event",
          properties: {},
        },
      ];

      await expect(sender.sendEvents(events)).rejects.toThrow(
        "Session expired after retry",
      );
      expect(onSessionExpired).toHaveBeenCalledTimes(1);
      // 验证重试了一次（总共调用 2 次 fetch）
      expect(fetchSpy).toHaveBeenCalledTimes(2);
    });

    it("should throw error if no onSessionExpired callback on 401", async () => {
      const fetchSpy = vi.fn().mockResolvedValue({
        ok: false,
        status: 401,
      } as Response);
      global.fetch = fetchSpy;
      (global.navigator as any).sendBeacon = undefined;

      // 没有 onSessionExpired 回调
      sender = new Sender(endpoint);

      const events: EventData[] = [
        {
          type: EventType.CUSTOM,
          eventId: "test-event",
          properties: {},
        },
      ];

      await expect(sender.sendEvents(events)).rejects.toThrow(
        "Session expired",
      );
      // 只调用一次，没有重试
      expect(fetchSpy).toHaveBeenCalledTimes(1);
    });

    it("should throw error on non-ok response", async () => {
      const fetchSpy = vi.fn().mockResolvedValue({
        ok: false,
        status: 500,
        statusText: "Internal Server Error",
      } as Response);
      global.fetch = fetchSpy;
      (global.navigator as any).sendBeacon = undefined;

      const events: EventData[] = [
        {
          type: EventType.CUSTOM,
          eventId: "test-event",
          properties: {},
        },
      ];

      await expect(sender.sendEvents(events)).rejects.toThrow("HTTP 500");
      expect(onSessionExpired).not.toHaveBeenCalled();
    });
  });

  describe("网络错误处理", () => {
    it("should throw error on network failure", async () => {
      const fetchSpy = vi.fn().mockRejectedValue(new Error("Network error"));
      global.fetch = fetchSpy;
      (global.navigator as any).sendBeacon = undefined;

      const events: EventData[] = [
        {
          type: EventType.CUSTOM,
          eventId: "test-event",
          properties: {},
        },
      ];

      await expect(sender.sendEvents(events)).rejects.toThrow("Network error");
    });

    it("should handle empty events array", async () => {
      await expect(sender.sendEvents([])).resolves.toBeUndefined();
      expect(global.fetch).not.toHaveBeenCalled();
    });
  });

  describe("数据压缩", () => {
    it("should remove null and undefined values from payload", async () => {
      const sendBeaconSpy = vi.fn().mockReturnValue(true);
      (global.navigator as any).sendBeacon = sendBeaconSpy;

      const events: EventData[] = [
        {
          type: EventType.CUSTOM,
          eventId: "test-event",
          properties: {
            valid: "value",
            nullValue: null,
            undefinedValue: undefined,
          },
        },
      ];

      await sender.sendEvents(events);

      expect(sendBeaconSpy).toHaveBeenCalledTimes(1);
      const blob = sendBeaconSpy.mock.calls[0][1] as Blob;

      // 使用 FileReader 读取 Blob（jsdom 可能不支持 blob.text()）
      const reader = new FileReader();
      const textPromise = new Promise<string>((resolve, reject) => {
        reader.onload = () => resolve(reader.result as string);
        reader.onerror = reject;
        reader.readAsText(blob);
      });

      const text = await textPromise;
      const payload = JSON.parse(text);

      expect(payload.e[0].p).toEqual({ valid: "value" });
      expect(payload.e[0].p).not.toHaveProperty("nullValue");
      expect(payload.e[0].p).not.toHaveProperty("undefinedValue");
    });

    it("should use short field names in payload", async () => {
      const sendBeaconSpy = vi.fn().mockReturnValue(true);
      (global.navigator as any).sendBeacon = sendBeaconSpy;

      const events: EventData[] = [
        {
          type: EventType.CUSTOM,
          eventId: "test-event",
          properties: { key: "value" },
        },
      ];

      await sender.sendEvents(events);

      const blob = sendBeaconSpy.mock.calls[0][1] as Blob;

      // 使用 FileReader 读取 Blob
      const reader = new FileReader();
      const textPromise = new Promise<string>((resolve, reject) => {
        reader.onload = () => resolve(reader.result as string);
        reader.onerror = reject;
        reader.readAsText(blob);
      });

      const text = await textPromise;
      const payload = JSON.parse(text);

      expect(payload).toHaveProperty("e");
      expect(payload.e[0]).toHaveProperty("t");
      expect(payload.e[0]).toHaveProperty("id");
      expect(payload.e[0]).toHaveProperty("p");
      expect(payload.e[0].t).toBe(EventType.CUSTOM);
      expect(payload.e[0].id).toBe("test-event");
    });
  });

  describe("setEndpoint", () => {
    it("should update endpoint correctly", () => {
      const newEndpoint = "https://new-endpoint.com";
      sender.setEndpoint(newEndpoint);

      // 验证端点已更新（通过后续调用验证）
      const sendBeaconSpy = vi.fn().mockReturnValue(true);
      (global.navigator as any).sendBeacon = sendBeaconSpy;

      const events: EventData[] = [
        {
          type: EventType.CUSTOM,
          eventId: "test",
          properties: {},
        },
      ];

      sender.sendEvents(events);

      expect(sendBeaconSpy).toHaveBeenCalledWith(
        `${newEndpoint}/api/ingest`,
        expect.any(Blob),
      );
    });
  });
});
