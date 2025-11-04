import { describe, it, expect } from "vitest";
import { removeNulls, hashCode } from "../../utils/index";

describe("Utils", () => {
  describe("removeNulls", () => {
    it("should remove null and undefined values", () => {
      const obj = {
        a: 1,
        b: null,
        c: undefined,
        d: "test",
      };

      const result = removeNulls(obj);
      expect(result).toEqual({ a: 1, d: "test" });
    });

    it("should handle nested objects", () => {
      const obj = {
        a: 1,
        b: {
          c: null,
          d: "test",
        },
      };

      const result = removeNulls(obj);
      expect(result).toEqual({ a: 1, b: { d: "test" } });
    });
  });

  describe("hashCode", () => {
    it("should generate hash code for string", () => {
      const hash = hashCode("test-string");
      expect(typeof hash).toBe("number");
      expect(hash).toBeGreaterThanOrEqual(0);
    });

    it("should generate consistent hash for same string", () => {
      const hash1 = hashCode("test");
      const hash2 = hashCode("test");
      expect(hash1).toBe(hash2);
    });
  });
});
