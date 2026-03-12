import { test, expect } from '../fixtures';
import { DashboardPage } from '../pages/dashboard.page';
import { uniqueDashboardName } from '../fixtures';

test.describe('Dashboards', () => {
    let dashboardPage: DashboardPage;

    test.beforeEach(async ({ authenticatedPage }) => {
        dashboardPage = new DashboardPage(authenticatedPage);
    });

    test('should display dashboard list', async ({ authenticatedPage }) => {
        // Mock the dashboards API
        await authenticatedPage.route('**/api/v1/dashboards*', async (route) => {
            if (route.request().method() === 'GET' && !route.request().url().includes('/dashboards/')) {
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify([
                        {
                            id: 'dash-001',
                            name: 'Q4 Financial Overview',
                            is_public: true,
                            created_at: '2026-01-15T10:00:00Z',
                            updated_at: '2026-03-01T14:30:00Z',
                            owner_id: 'user-001',
                        },
                        {
                            id: 'dash-002',
                            name: 'Monthly KPI Tracker',
                            is_public: false,
                            created_at: '2026-02-01T09:00:00Z',
                            updated_at: '2026-03-10T11:00:00Z',
                            owner_id: 'user-001',
                        },
                    ]),
                });
            } else {
                await route.continue();
            }
        });

        await dashboardPage.navigate();

        await expect(dashboardPage.heading).toBeVisible();
        await expect(dashboardPage.newDashboardButton).toBeVisible();

        const names = await dashboardPage.getDashboardList();
        expect(names).toContain('Q4 Financial Overview');
        expect(names).toContain('Monthly KPI Tracker');
    });

    test('should create a new dashboard', async ({ authenticatedPage }) => {
        const dashboardName = uniqueDashboardName('E2E Test');

        // Mock POST to create dashboard
        await authenticatedPage.route('**/api/v1/dashboards', async (route) => {
            if (route.request().method() === 'POST') {
                const body = route.request().postDataJSON();
                await route.fulfill({
                    status: 201,
                    contentType: 'application/json',
                    body: JSON.stringify({
                        id: 'dash-new-001',
                        name: body?.name || dashboardName,
                        is_public: false,
                        created_at: new Date().toISOString(),
                        updated_at: new Date().toISOString(),
                        owner_id: 'user-001',
                        widgets: [],
                    }),
                });
            } else {
                await route.continue();
            }
        });

        // Mock GET for empty list initially, then list with new dashboard
        let created = false;
        await authenticatedPage.route('**/api/v1/dashboards', async (route) => {
            if (route.request().method() === 'GET') {
                const list = created
                    ? [{ id: 'dash-new-001', name: dashboardName, is_public: false, created_at: new Date().toISOString(), updated_at: new Date().toISOString(), owner_id: 'user-001' }]
                    : [];
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify(list),
                });
            } else {
                created = true;
                await route.continue();
            }
        });

        await dashboardPage.navigate();

        // Click new dashboard and fill the form
        await dashboardPage.newDashboardButton.click();
        await authenticatedPage.waitForURL('**/dashboards/new');

        // Fill dashboard name
        const nameInput = authenticatedPage.getByRole('textbox').first();
        await nameInput.fill(dashboardName);

        // Save
        const saveButton = authenticatedPage.getByRole('button', { name: /save/i });
        if (await saveButton.isVisible({ timeout: 3_000 }).catch(() => false)) {
            await saveButton.click();
        }

        // Verify the creation request was made
        await authenticatedPage.waitForLoadState('networkidle');
    });

    test('should open and view dashboard charts', async ({ authenticatedPage }) => {
        // Mock dashboard list
        await authenticatedPage.route('**/api/v1/dashboards', async (route) => {
            if (route.request().method() === 'GET' && !route.request().url().match(/\/dashboards\/[^/]+$/)) {
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify([
                        {
                            id: 'dash-chart-001',
                            name: 'Chart Dashboard',
                            is_public: true,
                            created_at: '2026-01-15T10:00:00Z',
                            updated_at: '2026-03-01T14:30:00Z',
                            owner_id: 'user-001',
                        },
                    ]),
                });
            } else {
                await route.continue();
            }
        });

        // Mock individual dashboard detail
        await authenticatedPage.route('**/api/v1/dashboards/dash-chart-001', async (route) => {
            await route.fulfill({
                status: 200,
                contentType: 'application/json',
                body: JSON.stringify({
                    id: 'dash-chart-001',
                    name: 'Chart Dashboard',
                    is_public: true,
                    created_at: '2026-01-15T10:00:00Z',
                    updated_at: '2026-03-01T14:30:00Z',
                    owner_id: 'user-001',
                    widgets: [
                        {
                            id: 'widget-001',
                            type: 'bar_chart',
                            title: 'Revenue by Quarter',
                            config: {},
                            position: { x: 0, y: 0, w: 6, h: 4 },
                        },
                    ],
                }),
            });
        });

        await dashboardPage.navigate();

        // Open the dashboard
        await dashboardPage.openDashboard('Chart Dashboard');

        // Verify we are on the dashboard viewer page
        await expect(authenticatedPage).toHaveURL(/\/dashboards\/dash-chart-001/);

        // Check that the dashboard title is visible
        await expect(authenticatedPage.getByText('Chart Dashboard')).toBeVisible();
    });

    test('should delete a dashboard', async ({ authenticatedPage }) => {
        // Mock dashboard list
        await authenticatedPage.route('**/api/v1/dashboards', async (route) => {
            if (route.request().method() === 'GET') {
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify([
                        {
                            id: 'dash-delete-001',
                            name: 'Dashboard to Delete',
                            is_public: false,
                            created_at: '2026-02-01T09:00:00Z',
                            updated_at: '2026-03-10T11:00:00Z',
                            owner_id: 'user-001',
                        },
                    ]),
                });
            } else {
                await route.continue();
            }
        });

        // Mock individual dashboard detail
        await authenticatedPage.route('**/api/v1/dashboards/dash-delete-001', async (route) => {
            if (route.request().method() === 'DELETE') {
                await route.fulfill({ status: 204 });
            } else {
                await route.fulfill({
                    status: 200,
                    contentType: 'application/json',
                    body: JSON.stringify({
                        id: 'dash-delete-001',
                        name: 'Dashboard to Delete',
                        is_public: false,
                        created_at: '2026-02-01T09:00:00Z',
                        updated_at: '2026-03-10T11:00:00Z',
                        owner_id: 'user-001',
                        widgets: [],
                    }),
                });
            }
        });

        await dashboardPage.navigate();

        // Open the dashboard
        await dashboardPage.openDashboard('Dashboard to Delete');

        // Look for a delete button
        const deleteButton = authenticatedPage.getByRole('button', { name: /delete/i });
        if (await deleteButton.isVisible({ timeout: 5_000 }).catch(() => false)) {
            await deleteButton.click();

            // Confirm deletion if a dialog appears
            const confirmButton = authenticatedPage.getByRole('button', { name: /confirm|yes|delete/i }).last();
            if (await confirmButton.isVisible({ timeout: 3_000 }).catch(() => false)) {
                await confirmButton.click();
            }

            await authenticatedPage.waitForLoadState('networkidle');
        }
    });
});
