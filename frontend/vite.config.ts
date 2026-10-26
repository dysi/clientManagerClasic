import { defineConfig } from "vitest/config";
import vue from "@vitejs/plugin-vue";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
  plugins: [vue(), tailwindcss()],
  base: "./",
  build: {
    outDir: "../target/generated-resources/frontend/static/ui",
    emptyOutDir: true,
  },
  server: {
    proxy: {
      "/clientmanager/web": "http://127.0.0.1:3000",
    },
  },
  test: {
    environment: "jsdom",
    include: ["src/**/*.test.ts"],
    restoreMocks: true,
  },
});
