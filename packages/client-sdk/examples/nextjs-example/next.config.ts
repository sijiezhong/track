import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  output: "export",
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
