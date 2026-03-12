import { defineConfig } from 'vitest/config';
import path from 'path';

export default defineConfig({
  resolve: {
    alias: {
      '@helpers': path.resolve(__dirname, 'src/helpers'),
    },
  },
  test: {
    globals: true,
    globalSetup: ['./src/setup.ts'],
    teardownTimeout: 30_000,
    testTimeout: 60_000,
    hookTimeout: 30_000,
    // Run tests sequentially to avoid cross-test interference
    pool: 'forks',
    poolOptions: {
      forks: {
        singleFork: true,
      },
    },
    // Run test files sequentially
    fileParallelism: false,
    include: ['src/tests/**/*.test.ts'],
  },
});
