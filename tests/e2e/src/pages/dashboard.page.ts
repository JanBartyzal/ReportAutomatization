import { type Page, type Locator, expect } from '@playwright/test';

/**
 * Page Object for Dashboard list and individual dashboard views.
 *
 * Covers creating, listing, opening, and deleting dashboards.
 */
export class DashboardPage {
    readonly page: Page;
    readonly heading: Locator;
    readonly newDashboardButton: Locator;
    readonly dashboardGrid: Locator;
    readonly emptyState: Locator;

    constructor(page: Page) {
        this.page = page;
        this.heading = page.getByRole('heading', { name: 'Dashboards' });
        this.newDashboardButton = page.getByRole('button', { name: 'New Dashboard' });
        this.dashboardGrid = page.locator('[class*="grid"]');
        this.emptyState = page.getByText('No dashboards yet');
    }

    /** Navigate to the dashboard list page. */
    async navigate(): Promise<void> {
        await this.page.goto('/dashboards');
        await this.page.waitForLoadState('networkidle');
    }

    /**
     * Create a new dashboard with the given name.
     * Assumes we start on the dashboard list page and navigates to the editor.
     */
    async createDashboard(name: string): Promise<void> {
        await this.newDashboardButton.click();
        await this.page.waitForURL('**/dashboards/new');

        // Fill the dashboard name input (look for a text input or the first input field)
        const nameInput = this.page.getByRole('textbox').first();
        await nameInput.fill(name);

        // Save the dashboard
        const saveButton = this.page.getByRole('button', { name: /save/i });
        await saveButton.click();

        // Wait for navigation back to the dashboard view or list
        await this.page.waitForLoadState('networkidle');
    }

    /** Return an array of dashboard names visible on the list page. */
    async getDashboardList(): Promise<string[]> {
        await this.page.waitForLoadState('networkidle');
        // Dashboard names are rendered as Title3 elements inside cards
        const cards = this.page.locator('[class*="card"]');
        const count = await cards.count();
        const names: string[] = [];
        for (let i = 0; i < count; i++) {
            const heading = cards.nth(i).locator('h3, [class*="title"]').first();
            const text = await heading.textContent();
            if (text && text !== 'Dashboard Preview') {
                names.push(text.trim());
            }
        }
        return names;
    }

    /** Open a specific dashboard by clicking its card. */
    async openDashboard(name: string): Promise<void> {
        const card = this.page.locator('[class*="card"]', {
            has: this.page.getByText(name, { exact: true }),
        });
        await card.click();
        await this.page.waitForURL('**/dashboards/*');
        await this.page.waitForLoadState('networkidle');
    }

    /** Assert that at least one chart/visualization element is visible on the current dashboard. */
    async expectChartVisible(): Promise<void> {
        // Recharts renders SVG elements with class "recharts-wrapper" or generic <svg> inside chart containers
        const chart = this.page.locator('.recharts-wrapper, svg.recharts-surface, [class*="chart"]').first();
        await expect(chart).toBeVisible({ timeout: 10_000 });
    }

    /** Delete a dashboard from the list by name (via context menu or delete button). */
    async deleteDashboard(name: string): Promise<void> {
        // Open the dashboard first
        await this.openDashboard(name);

        // Look for a delete button on the dashboard viewer/editor page
        const deleteButton = this.page.getByRole('button', { name: /delete/i });
        await deleteButton.click();

        // Confirm deletion in dialog if present
        const confirmButton = this.page.getByRole('button', { name: /confirm|yes|delete/i }).last();
        if (await confirmButton.isVisible({ timeout: 3_000 }).catch(() => false)) {
            await confirmButton.click();
        }

        // Wait for navigation back to the list
        await this.page.waitForURL('**/dashboards');
        await this.page.waitForLoadState('networkidle');
    }
}
