import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './tests',
  fullyParallel: false,
  forbidOnly: !!process.env.CI,
  retries: 0,
  workers: 1,
  timeout: 300_000,
  expect: {
    timeout: 20_000,
  },
  reporter: [['html', { open: 'never' }], ['list'], ['junit', { outputFile: 'test-results/junit-results.xml' }]],
  use: {
    baseURL: 'https://assets-dev.travelhouseuk.co.uk',
    headless: !!process.env.CI,
    viewport: process.env.CI ? { width: 1920, height: 1080 } : null,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    actionTimeout: 20_000,
    navigationTimeout: 30_000,
    launchOptions: {
      slowMo: process.env.CI ? 0 : 250,
      args: ['--start-maximized'],
    },
  },
});
