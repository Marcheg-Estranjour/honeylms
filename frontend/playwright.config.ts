import { defineConfig, devices } from '@playwright/test';

/**
 * End-to-end tests (S10-4) — run against the LOCAL stack, before a demo or a release:
 *   1. docker compose up -d            (PostgreSQL + backend, profile dev → demo seed)
 *   2. npm run e2e                     (starts `ng serve` if it is not already running)
 *
 * CHOIX TECHNIQUE : not run in GitHub Actions for the MVP — it needs the whole stack
 * (database, backend, seed). Unit tests (Vitest, JUnit) stay the CI safety net.
 * Tests create their own uniquely named student, so they can be replayed on the same database.
 *
 * E2E_BASE_URL=http://localhost:8000 → run against the production-like Docker image (nginx)
 * instead of ng serve (no dev server is started in that case).
 */
const baseURL = process.env['E2E_BASE_URL'] || 'http://localhost:4200';
const useDevServer = !process.env['E2E_BASE_URL'];

export default defineConfig({
  testDir: './e2e',
  fullyParallel: false,
  workers: 1,
  retries: 0,
  timeout: 30_000,
  expect: { timeout: 7_000 },
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL,
    locale: 'fr-FR',
    timezoneId: 'Europe/Paris',
    trace: 'retain-on-failure',
    screenshot: 'only-on-failure',
  },
  projects: [
    {
      name: 'chromium',
      use: {
        ...devices['Desktop Chrome'],
        // PW_CHANNEL=chrome → drive the Google Chrome installed on the machine instead of the
        // Chromium downloaded by Playwright (not available on Ubuntu 20.04, for example).
        channel: process.env['PW_CHANNEL'] || undefined,
      },
    },
  ],
  webServer: useDevServer
    ? { command: 'npm start', url: 'http://localhost:4200', reuseExistingServer: true, timeout: 120_000 }
    : undefined,
});
