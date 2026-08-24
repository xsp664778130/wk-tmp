import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  // Emit a self-contained Node.js server for the K3s web deployment. Sites
  // continues to consume the normal dist output from the same build.
  output: "standalone",
};

export default nextConfig;
