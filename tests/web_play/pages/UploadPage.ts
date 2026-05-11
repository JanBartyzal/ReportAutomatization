import { type Page, type Locator, expect } from '@playwright/test';
import { TIMEOUTS } from '../config/config';
import path from 'path';

export class UploadPage {
  readonly page: Page;
  readonly dropzone: Locator;
  readonly fileInput: Locator;
  readonly progressBar: Locator;
  readonly uploadButton: Locator;
  readonly fileList: Locator;
  readonly errorMessage: Locator;
  readonly successMessage: Locator;

  constructor(page: Page) {
    this.page           = page;
    this.dropzone       = page.locator('[data-testid="dropzone"], [data-testid="upload-zone"], .dropzone').first();
    this.fileInput      = page.locator('input[type="file"]').first();
    this.progressBar    = page.locator('[role="progressbar"], .progress-bar, [data-testid="upload-progress"]').first();
    this.uploadButton   = page.locator('button:has-text("Upload"), button:has-text("Browse"), [data-testid="upload-btn"]').first();
    this.fileList       = page.locator('[data-testid="file-list"], table, [role="grid"]').first();
    this.errorMessage   = page.locator('[role="alert"], [data-testid="upload-error"]').first();
    this.successMessage = page.locator('[role="status"], [data-testid="upload-success"]').first();
  }

  async waitForDropzone(): Promise<void> {
    await this.dropzone.waitFor({ state: 'visible', timeout: TIMEOUTS.default });
  }

  async uploadFile(filePath: string): Promise<void> {
    await this.fileInput.setInputFiles(filePath);
  }

  async waitForUploadComplete(): Promise<void> {
    // Wait for progress bar to appear and then disappear
    await this.progressBar.waitFor({ state: 'visible', timeout: 5_000 }).catch(() => {});
    await this.progressBar.waitFor({ state: 'hidden', timeout: TIMEOUTS.upload }).catch(() => {});
  }

  async expectFileInList(filename: string): Promise<void> {
    await expect(this.page.getByText(filename)).toBeVisible({ timeout: TIMEOUTS.default });
  }

  async expectUploadError(errorText?: string): Promise<void> {
    await expect(this.errorMessage).toBeVisible({ timeout: TIMEOUTS.default });
    if (errorText) {
      await expect(this.errorMessage).toContainText(errorText);
    }
  }

  /** Drop a file using the DataTransfer API (simulates drag & drop). */
  async dragAndDropFile(filePath: string): Promise<void> {
    const fileBuffer = require('fs').readFileSync(filePath);
    const mimeType   = filePath.endsWith('.pptx')
      ? 'application/vnd.openxmlformats-officedocument.presentationml.presentation'
      : filePath.endsWith('.xlsx')
      ? 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
      : 'application/octet-stream';

    await this.dropzone.dispatchEvent('dragenter');
    await this.dropzone.dispatchEvent('dragover');

    // Use setInputFiles for reliable file upload simulation
    await this.fileInput.setInputFiles({
      name: path.basename(filePath),
      mimeType,
      buffer: fileBuffer,
    });
  }
}
