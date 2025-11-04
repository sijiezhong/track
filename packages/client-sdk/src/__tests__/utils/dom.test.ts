import { describe, it, expect } from "vitest";
import { getDomPath } from "../../utils/dom";

describe("DOM Utils", () => {
  it("should generate DOM path for element with id", () => {
    const element = document.createElement("button");
    element.id = "test-btn";
    document.body.appendChild(element);

    const domPath = getDomPath(element);
    expect(domPath).toContain("button#test-btn");

    document.body.removeChild(element);
  });

  it("should generate DOM path for element without id", () => {
    const container = document.createElement("div");
    const button = document.createElement("button");
    container.appendChild(button);
    document.body.appendChild(container);

    const domPath = getDomPath(button);
    expect(domPath).toContain("button");

    document.body.removeChild(container);
  });
});
