import { defineConfig, devices } from '@playwright/test'

// Sobe o sistema inteiro: LLM falso, back-end (precisa de PostgreSQL com pgvector) e o front em modo preview.
export default defineConfig({
  testDir: './e2e',
  timeout: 30_000,
  retries: process.env.CI ? 1 : 0,
  reporter: process.env.CI ? [['html', { open: 'never' }], ['list']] : 'list',
  use: {
    baseURL: 'http://localhost:4173',
    trace: 'retain-on-failure',
    video: 'retain-on-failure',
  },
  projects: [{ name: 'chromium', use: { ...devices['Desktop Chrome'] } }],
  webServer: [
    {
      command: 'node e2e/fake-llm.mjs',
      url: 'http://127.0.0.1:9999/health',
      reuseExistingServer: !process.env.CI,
    },
    {
      command: 'java -jar ../backend/target/docmind-1.0.0.jar',
      url: 'http://localhost:8080/actuator/health',
      timeout: 120_000,
      reuseExistingServer: !process.env.CI,
      env: {
        LLM_API_KEY: 'e2e',
        LLM_BASE_URL: 'http://127.0.0.1:9999/v1',
        JWT_SECRET: 'segredo-somente-para-o-teste-e2e-0123456789',
      },
    },
    {
      command: 'npm run build && npx vite preview --port 4173 --strictPort',
      url: 'http://localhost:4173',
      timeout: 120_000,
      reuseExistingServer: !process.env.CI,
    },
  ],
})
