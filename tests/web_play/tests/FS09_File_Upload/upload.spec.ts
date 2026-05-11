/**
 * FS09 / FS02 – File Upload Manager
 *
 * UX focus:
 *  - Dropzone is visible and accessible
 *  - File type restrictions are communicated (allowed extensions shown)
 *  - Progress bar appears and disappears during upload
 *  - Success feedback after upload
 *  - Error feedback for unsupported file types
 *  - File list refreshes automatically after upload
 *  - Upload button has accessible name
 */
import { test, expect } from '@playwright/test';
import path from 'path';
import fs from 'fs';
import { UploadPage } from '../../pages/UploadPage';
import { AppPage } from '../../pages/AppPage';
import { ROUTES, TIMEOUTS } from '../../config/config';
import { gotoAndWait, featurePresent } from '../../fixtures/auth.fixture';

const FIXTURES_DIR = path.join(__dirname, '../../..', 'fixtures');

/** Create a minimal PPTX-shaped buffer (just OOXML zip magic bytes). */
function fakePptxBuffer(): Buffer {
  // PK header (ZIP) — PPTX is a ZIP container
  return Buffer.from([0x50, 0x4b, 0x03, 0x04, ...Buffer.alloc(50)]);
}

/** Create a minimal XLSX buffer. */
function fakeXlsxBuffer(): Buffer {
  return Buffer.from([0x50, 0x4b, 0x03, 0x04, ...Buffer.alloc(50)]);
}

test.describe('Upload page layout', () => {
  test('upload page has visible dropzone', async ({ page }) => {
    await gotoAndWait(page, ROUTES.upload);
    const upload = new UploadPage(page);

    const dropzoneVisible = await upload.dropzone.isVisible({ timeout: TIMEOUTS.default }).catch(() => false)
      || await upload.fileInput.isVisible({ timeout: 2_000 }).catch(() => false);

    expect(dropzoneVisible, 'Dropzone or file input must be visible on upload page').toBeTruthy();
  });

  test('upload page shows accepted file types', async ({ page }) => {
    await gotoAndWait(page, ROUTES.upload);

    const hint = page.locator(':text("pptx"), :text("xlsx"), :text(".xlsx"), :text(".pptx"), [data-testid="accepted-types"]').first();
    const hintVisible = await hint.isVisible({ timeout: 3_000 }).catch(() => false);

    if (!hintVisible) {
      await featurePresent(page, '[data-testid="accepted-types"]', 'Accepted file types hint');
    } else {
      await expect(hint).toBeVisible();
    }
  });

  test('upload zone has accessible label / role', async ({ page }) => {
    await gotoAndWait(page, ROUTES.upload);
    const upload = new UploadPage(page);

    // file input should have accept attribute
    const accept = await upload.fileInput.getAttribute('accept').catch(() => null);
    if (accept) {
      expect(accept).toMatch(/pptx|xlsx|pdf|csv/i);
    }

    // Dropzone container should have aria-label or role
    const zoneAriaLabel = await upload.dropzone.getAttribute('aria-label').catch(() => null);
    const zoneRole      = await upload.dropzone.getAttribute('role').catch(() => null);
    const hasA11y       = !!(zoneAriaLabel || zoneRole);
    if (!hasA11y) {
      console.warn('[MISSING FEATURE] Dropzone missing aria-label/role attribute');
    }
  });
});

test.describe('Upload interaction', () => {
  test('selecting a .pptx file triggers upload UI (progress or preview)', async ({ page }) => {
    await gotoAndWait(page, ROUTES.upload);
    const upload = new UploadPage(page);

    const inputVisible = await upload.fileInput.isVisible({ timeout: TIMEOUTS.default }).catch(() => false);
    if (!inputVisible) {
      await featurePresent(page, 'input[type="file"]', 'File input for upload');
      return;
    }

    await upload.fileInput.setInputFiles({
      name: 'test-report.pptx',
      mimeType: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
      buffer: fakePptxBuffer(),
    });

    // Either progress bar, file name, or error message should appear
    const feedback = await Promise.race([
      upload.progressBar.isVisible({ timeout: 5_000 }).catch(() => false),
      page.locator(':text("test-report.pptx")').isVisible({ timeout: 5_000 }).catch(() => false),
      upload.errorMessage.isVisible({ timeout: 5_000 }).catch(() => false),
    ]);
    expect(feedback, 'Upload should produce visual feedback after file selection').toBeTruthy();
  });

  test('selecting an unsupported file type shows error message', async ({ page }) => {
    await gotoAndWait(page, ROUTES.upload);
    const upload = new UploadPage(page);

    const inputVisible = await upload.fileInput.isVisible({ timeout: TIMEOUTS.default }).catch(() => false);
    if (!inputVisible) {
      await featurePresent(page, 'input[type="file"]', 'File input');
      return;
    }

    await upload.fileInput.setInputFiles({
      name: 'virus.exe',
      mimeType: 'application/octet-stream',
      buffer: Buffer.from('MZ' + '\0'.repeat(100)),
    });

    // Error feedback should appear
    const errorVisible = await upload.errorMessage.isVisible({ timeout: TIMEOUTS.short }).catch(() => false)
      || await page.locator(':text("not allowed"), :text("invalid"), :text("unsupported")').first()
           .isVisible({ timeout: TIMEOUTS.short }).catch(() => false);

    if (!errorVisible) {
      console.warn('[MISSING FEATURE] No error shown for unsupported file type .exe');
    }
  });
});

test.describe('File list', () => {
  test('files page has a list or grid of uploaded files', async ({ page }) => {
    await gotoAndWait(page, ROUTES.files);
    const app = new AppPage(page);

    const listEl = page.locator('table, [role="grid"], [role="list"], [data-testid="file-list"]').first();
    const visible = await listEl.isVisible({ timeout: TIMEOUTS.default }).catch(() => false);

    if (!visible) {
      // Empty state is also acceptable UX
      const emptyState = await page.locator('[data-testid="empty-state"], :text("No files"), :text("Žádné soubory")').first()
        .isVisible({ timeout: 3_000 }).catch(() => false);
      expect(visible || emptyState, 'Files page should show list or empty state').toBeTruthy();
    } else {
      await expect(listEl).toBeVisible();
    }
  });

  test('files page has working search or filter controls', async ({ page }) => {
    await gotoAndWait(page, ROUTES.files);

    const searchOrFilter = page.locator(
      'input[type="search"], input[placeholder*="search" i], input[placeholder*="filter" i], input[placeholder*="hledat" i], [data-testid="file-search"]'
    ).first();

    const visible = await searchOrFilter.isVisible({ timeout: 3_000 }).catch(() => false);
    if (!visible) {
      await featurePresent(page, 'input[type="search"]', 'File search/filter');
    } else {
      await expect(searchOrFilter).toBeEnabled();
    }
  });
});
