import { describe, it, expect, beforeEach, afterEach, vi } from "vitest";
import { ErrorMonitor } from "../../plugins/error";
import { Track } from "../../core/tracker";
import { EventType } from "../../types";

describe("ErrorMonitor", () => {
  let errorMonitor: ErrorMonitor;
  let track: Track;
  let addEventSpy: ReturnType<typeof vi.spyOn>;

  beforeEach(() => {
    track = Track.getInstance();
    errorMonitor = new ErrorMonitor(track);

    vi.spyOn(track, "isStarted").mockReturnValue(true);
    addEventSpy = vi.spyOn(track, "addEvent").mockImplementation(() => {});
  });

  afterEach(() => {
    errorMonitor.remove();
    vi.restoreAllMocks();
  });

  describe("JavaScript 错误监控", () => {
    it("should track JavaScript errors", () => {
      errorMonitor.setup();

      const errorEvent = new ErrorEvent("error", {
        message: "Test error",
        filename: "test.js",
        lineno: 10,
        colno: 5,
        error: new Error("Test error"),
      });

      window.dispatchEvent(errorEvent);

      expect(addEventSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          type: EventType.ERROR,
          properties: expect.objectContaining({
            message: "Test error",
            filename: "test.js",
            lineno: 10,
            colno: 5,
            type: "Error",
            pageUrl: window.location.href,
            pageTitle: document.title,
            userAgent: navigator.userAgent,
          }),
        }),
      );
    });

    it("should handle errors without error object", () => {
      errorMonitor.setup();

      const errorEvent = new ErrorEvent("error", {
        message: "Test error",
        filename: "test.js",
        lineno: 10,
        colno: 5,
      });

      window.dispatchEvent(errorEvent);

      expect(addEventSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          properties: expect.objectContaining({
            message: "Test error",
            stack: "",
            type: "Error",
          }),
        }),
      );
    });

    it("should handle errors with stack trace", () => {
      errorMonitor.setup();

      const error = new Error("Test error");
      error.stack = "Error: Test error\n    at test.js:10:5";

      const errorEvent = new ErrorEvent("error", {
        message: "Test error",
        filename: "test.js",
        lineno: 10,
        colno: 5,
        error: error,
      });

      window.dispatchEvent(errorEvent);

      expect(addEventSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          properties: expect.objectContaining({
            stack: "Error: Test error\n    at test.js:10:5",
          }),
        }),
      );
    });

    it("should handle unknown error format", () => {
      errorMonitor.setup();

      const errorEvent = new ErrorEvent("error", {
        message: "",
      });

      window.dispatchEvent(errorEvent);

      expect(addEventSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          properties: expect.objectContaining({
            message: "Unknown error",
            filename: "",
            lineno: 0,
            colno: 0,
          }),
        }),
      );
    });
  });

  describe("Promise 拒绝监控", () => {
    // jsdom 不支持 PromiseRejectionEvent，需要手动创建
    class MockPromiseRejectionEvent extends Event {
      reason: any;
      promise: Promise<any>;

      constructor(
        type: string,
        eventInitDict: { reason: any; promise: Promise<any> },
      ) {
        super(type);
        this.reason = eventInitDict.reason;
        this.promise = eventInitDict.promise;
      }
    }

    beforeEach(() => {
      // 在 jsdom 环境中添加 PromiseRejectionEvent
      if (typeof (global as any).PromiseRejectionEvent === "undefined") {
        (global as any).PromiseRejectionEvent =
          MockPromiseRejectionEvent as any;
      }
    });

    it("should track unhandled promise rejection with Error", () => {
      errorMonitor.setup();

      const error = new Error("Promise rejection");
      error.stack = "Error: Promise rejection\n    at test.js:20:5";

      const rejectionEvent = new MockPromiseRejectionEvent(
        "unhandledrejection",
        {
          reason: error,
          promise: Promise.reject(error).catch(() => {}), // 捕获 promise 避免未处理错误
        },
      );

      window.dispatchEvent(rejectionEvent as any);

      expect(addEventSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          type: EventType.ERROR,
          properties: expect.objectContaining({
            message: "Promise rejection",
            type: "Error",
            stack: "Error: Promise rejection\n    at test.js:20:5",
            reason: error,
          }),
        }),
      );
    });

    it("should track unhandled promise rejection with string reason", () => {
      errorMonitor.setup();

      const rejectionEvent = new MockPromiseRejectionEvent(
        "unhandledrejection",
        {
          reason: "String rejection",
          promise: Promise.reject("String rejection").catch(() => {}), // 捕获 promise
        },
      );

      window.dispatchEvent(rejectionEvent as any);

      expect(addEventSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          properties: expect.objectContaining({
            message: "String rejection",
            type: "PromiseRejection",
            stack: "",
            reason: "String rejection",
          }),
        }),
      );
    });

    it("should track unhandled promise rejection with object reason", () => {
      errorMonitor.setup();

      const reason = { code: 500, message: "Server error" };
      const rejectionEvent = new MockPromiseRejectionEvent(
        "unhandledrejection",
        {
          reason: reason,
          promise: Promise.reject(reason).catch(() => {}), // 捕获 promise
        },
      );

      window.dispatchEvent(rejectionEvent as any);

      expect(addEventSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          properties: expect.objectContaining({
            message: JSON.stringify(reason),
            type: "PromiseRejection",
            reason: reason,
          }),
        }),
      );
    });

    it("should track unhandled promise rejection with default message", () => {
      errorMonitor.setup();

      const rejectionEvent = new MockPromiseRejectionEvent(
        "unhandledrejection",
        {
          reason: null,
          promise: Promise.reject(null).catch(() => {}), // 捕获 promise
        },
      );

      window.dispatchEvent(rejectionEvent as any);

      expect(addEventSpy).toHaveBeenCalledWith(
        expect.objectContaining({
          properties: expect.objectContaining({
            message: "Unhandled Promise Rejection",
            type: "PromiseRejection",
          }),
        }),
      );
    });
  });

  describe("错误处理保护", () => {
    it("should not track when tracker is not started", () => {
      vi.spyOn(track, "isStarted").mockReturnValue(false);

      errorMonitor.setup();

      const errorEvent = new ErrorEvent("error", {
        message: "Test error",
      });

      window.dispatchEvent(errorEvent);

      expect(addEventSpy).not.toHaveBeenCalled();
    });

    it("should handle errors in error handler gracefully", () => {
      errorMonitor.setup();

      // Mock addEvent to throw error
      addEventSpy.mockImplementation(() => {
        throw new Error("Handler error");
      });

      const consoleSpy = vi.spyOn(console, "warn").mockImplementation(() => {});

      const errorEvent = new ErrorEvent("error", {
        message: "Test error",
      });

      // 不应该抛出错误，应该静默处理
      expect(() => {
        window.dispatchEvent(errorEvent);
      }).not.toThrow();

      expect(consoleSpy).toHaveBeenCalledWith(
        expect.stringContaining("Failed to handle error"),
        expect.any(Error),
      );

      consoleSpy.mockRestore();
    });
  });

  describe("setup/remove", () => {
    it("should remove event listeners on remove", () => {
      errorMonitor.setup();

      const errorEvent = new ErrorEvent("error", {
        message: "Test error",
      });

      window.dispatchEvent(errorEvent);
      expect(addEventSpy).toHaveBeenCalledTimes(1);

      errorMonitor.remove();

      addEventSpy.mockClear();
      window.dispatchEvent(errorEvent);
      expect(addEventSpy).not.toHaveBeenCalled();
    });

    it("should not setup when already monitored", () => {
      errorMonitor.setup();

      const errorEvent = new ErrorEvent("error", {
        message: "Test error",
      });

      window.dispatchEvent(errorEvent);
      expect(addEventSpy).toHaveBeenCalledTimes(1);

      // 再次 setup 不应该重复添加监听器
      errorMonitor.setup();

      addEventSpy.mockClear();
      window.dispatchEvent(errorEvent);
      // 应该仍然调用（因为监听器还在）
      expect(addEventSpy).toHaveBeenCalledTimes(1);
    });
  });
});
