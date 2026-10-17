import { defineConfig } from '@playwright/test';
export default defineConfig({
  testDir: './e2e', fullyParallel: false, retries: process.env['CI'] ? 1 : 0,
  use: { baseURL: process.env['PLAYWRIGHT_BASE_URL'] ?? 'http://127.0.0.1:4200', trace: 'retain-on-failure' },
  webServer: process.env['PLAYWRIGHT_BASE_URL'] ? undefined : { command: 'npm start', url: 'http://127.0.0.1:4200', reuseExistingServer: !process.env['CI'], timeout: 120000 }
});
