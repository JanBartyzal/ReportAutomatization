import { test, expect } from '../fixtures';
import { FilesPage } from '../pages/files.page';
import { uniqueFileName } from '../fixtures';
import path from 'path';

// Path to test fixture files (adjust if fixtures live elsewhere)
const FIXTURES_DIR = path.resolve(__dirname, '..', '..', '..', 'fixtures');

test.describe('File Upload', () => {
    let filesPage: FilesPage;

    test.beforeEach(async ({ authenticatedPage }) => {
        filesPage = new FilesPage(authenticatedPage);
    });

    test('should upload a PPTX file successfully', async ({ authenticatedPage }) => {
        // Navigate to upload page
        await authenticatedPage.goto('/upload');
        await authenticatedPage.waitForLoadState('networkidle');

        // Verify upload page is displayed
        await expect(authenticatedPage.getByRole('heading', { name: 'Upload Files' })).toBeVisible();
        await expect(authenticatedPage.getByText('Supported: .pptx, .xlsx, .pdf, .csv')).toBeVisible();

        // Intercept the upload API call to mock a successful response
        await authenticatedPage.route('**/api/v1/files/upload', async (route) => {
            await route.fulfill({
                status: 201,
                contentType: 'application/json',
                body: JSON.stringify({
                    file_id: 'test-file-001',
                    filename: 'test-report.pptx',
                    mime_type: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
                    size_bytes: 1048576,
                    status: 'pending',
                    uploaded_at: new Date().toISOString(),
                }),
            });
        });

        // Upload via the hidden file input (dropzone)
        const fileInput = authenticatedPage.locator('input[type="file"]');
        // Create a virtual file for the test since we may not have real fixtures
        await fileInput.setInputFiles({
            name: 'test-report.pptx',
            mimeType: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
            buffer: Buffer.from('mock-pptx-content'),
        });

        // After upload, the app navigates to /files
        await authenticatedPage.waitForURL('**/files', { timeout: 15_000 });
    });

    test('should show file in the file list after upload', async ({ authenticatedPage }) => {
        // Mock the files API to return a file list including our uploaded file
        await authenticatedPage.route('**/api/v1/files*', async (route) => {
            if (route.request().method() === 'GET') {
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify({
                        items: [
                            {
                                file_id: 'test-file-001',
                                filename: 'test-report.pptx',
                                mime_type: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
                                size_bytes: 1048576,
                                status: 'completed',
                                uploaded_at: new Date().toISOString(),
                            },
                            {
                                file_id: 'test-file-002',
                                filename: 'quarterly-data.xlsx',
                                mime_type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
                                size_bytes: 524288,
                                status: 'processing',
                                uploaded_at: new Date().toISOString(),
                            },
                        ],
                        total: 2,
                    }),
                });
            } else {
                await route.continue();
            }
        });

        await filesPage.navigate();

        // Verify both files appear in the table
        const fileList = await filesPage.getFileList();
        expect(fileList).toContain('test-report.pptx');
        expect(fileList).toContain('quarterly-data.xlsx');
    });

    test('should display processing status', async ({ authenticatedPage }) => {
        // Mock files API with a file in "processing" status
        await authenticatedPage.route('**/api/v1/files*', async (route) => {
            if (route.request().method() === 'GET') {
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify({
                        items: [
                            {
                                file_id: 'test-file-processing',
                                filename: 'processing-file.pptx',
                                mime_type: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
                                size_bytes: 2097152,
                                status: 'processing',
                                uploaded_at: new Date().toISOString(),
                            },
                        ],
                        total: 1,
                    }),
                });
            } else {
                await route.continue();
            }
        });

        await filesPage.navigate();

        // Verify the status badge shows "processing"
        const status = await filesPage.getFileStatus('processing-file.pptx');
        expect(status.toLowerCase()).toContain('processing');
    });

    test('should show results after processing completes', async ({ authenticatedPage }) => {
        let requestCount = 0;

        // First request returns "processing", subsequent requests return "completed"
        await authenticatedPage.route('**/api/v1/files*', async (route) => {
            if (route.request().method() === 'GET') {
                requestCount++;
                const status = requestCount <= 1 ? 'processing' : 'completed';
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify({
                        items: [
                            {
                                file_id: 'test-file-transition',
                                filename: 'transitioning-file.pptx',
                                mime_type: 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
                                size_bytes: 1048576,
                                status,
                                uploaded_at: new Date().toISOString(),
                            },
                        ],
                        total: 1,
                    }),
                });
            } else {
                await route.continue();
            }
        });

        await filesPage.navigate();

        // Initially should show processing
        const initialStatus = await filesPage.getFileStatus('transitioning-file.pptx');
        expect(initialStatus.toLowerCase()).toContain('processing');

        // Trigger a re-fetch by reloading
        await authenticatedPage.reload();
        await authenticatedPage.waitForLoadState('networkidle');

        // Now should show completed
        const finalStatus = await filesPage.getFileStatus('transitioning-file.pptx');
        expect(finalStatus.toLowerCase()).toContain('completed');
    });
});
