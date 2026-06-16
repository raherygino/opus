import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import electron from "vite-plugin-electron";
import electronRenderer from "vite-plugin-electron-renderer";
import path from "path";
import fs from "fs";

export default defineConfig({
  plugins: [
    react(),
    electron([
      {
        entry: "electron/main.ts",
        vite: {
          build: {
            outDir: "dist-electron",
            rollupOptions: {
              external: ["electron"],
            },
          },
        },
      },
    ]),
    electronRenderer(),
    {
      name: "copy-preload",
      closeBundle() {
        const src = path.resolve(__dirname, "electron/preload.cjs");
        const dest = path.resolve(__dirname, "dist-electron/preload.cjs");
        const dir = path.dirname(dest);
        if (!fs.existsSync(dir)) {
          fs.mkdirSync(dir, { recursive: true });
        }
        fs.copyFileSync(src, dest);
      },
    },
  ],
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ["react", "react-dom", "react-router-dom"],
          ui: ["framer-motion", "lucide-react"],
        },
      },
    },
  },
});
