import Mock from "mockjs";
import type { MockDef } from "./common";
import { ok, maybeError } from "./common";

const defs: MockDef[] = [
  {
    url: "/api/events",
    method: "get",
    response: ({ url, query }) => {
      const err = maybeError(url);
      if (err) return err;
      const size = Number((query as any).size || 20);
      const index = Number((query as any).page || 1);
      const items = Array.from({ length: size }).map(() => ({
        ts: new Date().toISOString(),
        type: Mock.Random.pick([
          "page_view",
          "click",
          "performance",
          "error",
          "custom",
        ]),
        pageUrl: Mock.Random.url("http"),
        userId: Mock.Random.string("lower", 8),
        customEventId: Mock.Random.boolean() ? Mock.Random.word(3, 8) : null,
        properties: { id: Mock.Random.integer(1, 1000) },
      }));
      return ok({ items, page: { index, size, total: 200 } });
    },
  },
];

export default defs;
