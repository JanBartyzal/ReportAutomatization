import { defineConfig, devices } from '@playwright/test';
import dotenv from 'dotenv';
import path from 'path';

dotenv.config({ path: path.resolve(__dirname, '.env') });

const baseURL = process.env.BASE_URL || 'http://localhost';

export default defineConfig({
    testDir: './src/tests',
    fullyParallel: true,
    forbidOnly: !!process.env.CI,
    retries: process.env.CI ? 2 : 0,
    workers: process.env.CI ? 1 : undefined,
    reporter: [
        ['html', { open: 'never' }],
        ['list'],
    ],
    use: {
        baseURL,
        trace: 'on-first-retry',
        screenshot: 'only-on-failure',
        video: 'on-first-retry',
    },

    projects: [
        // Auth setup project - runs first and produces storageState
        {
            name: 'setup',
            testMatch: /auth\.setup\.ts/,
            testDir: './src',
        },

        // Chromium tests using authenticated state
        {
            name: 'chromium',
            use: {
                ...devices['Desktop Chrome'],
                storageState: '.auth/user.json',
            },
            dependencies: ['setup'],
        },

        // Firefox tests using authenticated state
        {
            name: 'firefox',
            use: {
                ...devices['Desktop Firefox'],
                storageState: '.auth/user.json',
            },
            dependencies: ['setup'],
        },
    ],
});
