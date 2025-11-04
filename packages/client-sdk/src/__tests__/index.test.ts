import { describe, it, expect } from "vitest";
import track, {
  Track,
  EventType,
  PRESET_CONFIGS,
  getDomPath,
  removeNulls,
  hashCode,
} from "../index";

describe("SDK Entry Point", () => {
  it("should export default track instance", () => {
    expect(track).toBeDefined();
    expect(track).toBeInstanceOf(Track);
  });

  it("should export Track class", () => {
    expect(Track).toBeDefined();
    expect(typeof Track.getInstance).toBe("function");
  });

  it("should export EventType enum", () => {
    expect(EventType).toBeDefined();
    expect(EventType.PAGE_VIEW).toBe(1);
    expect(EventType.CLICK).toBe(2);
    expect(EventType.PERFORMANCE).toBe(3);
    expect(EventType.ERROR).toBe(4);
    expect(EventType.CUSTOM).toBe(5);
    expect(EventType.PAGE_STAY).toBe(6);
  });

  it("should export preset configs", () => {
    expect(PRESET_CONFIGS).toBeDefined();
    expect(PRESET_CONFIGS.CLICK_TRACK_STRICT).toBeDefined();
    expect(PRESET_CONFIGS.CLICK_TRACK_BALANCED).toBeDefined();
    expect(PRESET_CONFIGS.CLICK_TRACK_LOOSE).toBeDefined();
  });

  it("should export utility functions", () => {
    // 验证工具函数可以被导入
    expect(typeof getDomPath).toBe("function");
    expect(typeof removeNulls).toBe("function");
    expect(typeof hashCode).toBe("function");
  });
});
