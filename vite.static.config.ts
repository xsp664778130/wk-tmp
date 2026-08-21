import react from "@vitejs/plugin-react";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { defineConfig } from "vite";

const projectRoot = path.dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  root: path.join(projectRoot, "web-static"),
  publicDir: path.join(projectRoot, "public"),
  plugins: [react()],
  css: { postcss: path.join(projectRoot, "postcss.config.mjs") },
  build: {
    outDir: path.join(projectRoot, "dist/static"),
    emptyOutDir: true,
    sourcemap: false,
  },
});
