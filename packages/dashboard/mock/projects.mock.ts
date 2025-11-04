import Mock from "mockjs";
import type { MockDef } from "./common";
import { ok, maybeError } from "./common";

const defs: MockDef[] = [
  {
    url: "/api/projects",
    method: "get",
    response: ({ url }) => {
      const err = maybeError(url);
      if (err) return err;
      const list = [
        { appId: "demo-web", appName: "演示站点 Web" },
        { appId: "demo-admin", appName: "演示后台 Admin" },
        { appId: "marketing-site", appName: "营销站点" },
      ];
      return ok({ list });
    },
  },
];

export default defs;
