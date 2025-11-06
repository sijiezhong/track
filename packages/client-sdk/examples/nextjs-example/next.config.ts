import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "export",
  // 显式指定追踪根，避免 monorepo 多个 lockfile 的提示
  outputFileTracingRoot: __dirname,
  basePath: "/examples/next",
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: "https://track.zhongsijie.cn/api/:path*",
      },
    ];
  },
};

export default nextConfig;
