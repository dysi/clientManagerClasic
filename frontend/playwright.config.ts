import { defineConfig, devices } from "@playwright/test";

export default defineConfig({
  testDir: "./tests/e2e",
  workers: 1,
  timeout: 45_000,
  use: {
    ...devices["Desktop Chrome"],
    channel: process.env.E2E_CHANNEL || "msedge",
    baseURL:
      process.env.E2E_BASE_URL || "http://127.0.0.1:3000/clientmanager/ui/",
    screenshot: "only-on-failure",
    trace: "retain-on-failure",
  },
});
