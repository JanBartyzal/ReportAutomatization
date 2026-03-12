import { type Page, type Locator, expect } from '@playwright/test';
import path from 'path';

/**
 * Page Object for the Files page and Upload page.
 *
 * Covers listing files, uploading new files, and checking processing status.
 */
export class FilesPage {
    readonly page: Page;
    readonly heading: Locator;
    readonly uploadButton: Locator;
    readonly fileTable: Locator;
    readonly emptyState: Locator;

    constructor(page: Page) {
        this.page = page;
        this.heading = page.getByRole('heading', { name: 'Files' });
        this.uploadButton = page.getByRole('button', { name: 'Upload' });
        this.fileTable = page.locator('table');
        this.emptyState = page.getByText('No files found. Upload a file to get started.');
    }

    /** Navigate to the files listing page. */
    async navigate(): Promise<void> {
        await this.page.goto('/files');
        await this.page.waitForLoadState('networkidle');
    }

    /** Click the Upload button to navigate to the upload page. */
    async goToUpload(): Promise<void> {
        await this.uploadButton.click();
        await this.page.waitForURL('**/upload');
    }

    /**
     * Upload a file via the dropzone on the Upload page.
     * Navigates to /upload if not already there.
     *
     * @param filePath - Absolute or relative path to the test fixture file.
     */
    async uploadFile(filePath: string): Promise<void> {
        // Navigate to upload page if we are not already there
        if (!this.page.url().includes('/upload')) {
            await this.page.goto('/upload');
            await this.page.waitForLoadState('networkidle');
        }

        // The dropzone contains a hidden <input type="file">
        const fileInput = this.page.locator('input[type="file"]');
        await fileInput.setInputFiles(path.resolve(filePath));
    }

    /**
     * Wait for file processing to complete by polling the file status badge.
     * Expects the status to transition away from "processing" / "pending".
     */
    async waitForProcessing(fileName: string, timeoutMs = 30_000): Promise<void> {
        const row = this.page.locator('tr', { has: this.page.getByText(fileName) });
        // Wait until the status badge no longer shows "processing" or "pending"
        await expect(
            row.locator('[class*="badge"]').filter({ hasNot: this.page.getByText(/processing|pending/i) }),
        ).toBeVisible({ timeout: timeoutMs });
    }

    /** Return an array of file name strings visible in the table. */
    async getFileList(): Promise<string[]> {
        await this.page.waitForLoadState('networkidle');
        const rows = this.fileTable.locator('tbody tr');
        const count = await rows.count();
        const names: string[] = [];
        for (let i = 0; i < count; i++) {
            // The filename is inside a <span> within the filename cell layout
            const nameCell = rows.nth(i).locator('td').nth(1).locator('span').first();
            const text = await nameCell.textContent();
            if (text) names.push(text.trim());
        }
        return names;
    }

    /** Get the status badge text for a specific file by name. */
    async getFileStatus(fileName: string): Promise<string> {
        const row = this.page.locator('tr', { has: this.page.getByText(fileName) });
        // Status badge is in the 4th column (index 3)
        const statusBadge = row.locator('td').nth(3).locator('[class*="badge"]');
        const text = await statusBadge.textContent();
        return (text ?? '').trim();
    }
}
