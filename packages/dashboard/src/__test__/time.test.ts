import { describe, it, expect } from "vitest";
import { getLastHoursRange } from "../utils/time";

describe("time utils", () => {
  it("getLastHoursRange returns start before end and formatted", () => {
    const { start, end } = getLastHoursRange(1);
    expect(start).toMatch(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/);
    expect(end).toMatch(/\d{4}-\d{2}-\d{2}T\d{2}:\d{2}/);
  });
});
