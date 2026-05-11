/**
 * AppPage – base Page Object for the RA frontend.
 * Wraps common UI patterns: sidebar, notifications, loading states, toasts.
 */
import { type Page, type Locator, expect } from '@playwright/test';
import { ROUTES, TIMEOUTS } from '../config/config';

export class AppPage {
  readonly page: Page;

  // ── Layout locators ────────────────────────────────────────────────────────
  readonly sidebar: Locator;
  readonly notificationBell: Locator;
  readonly notificationPanel: Locator;
  readonly globalSearch: Locator;
  readonly userMenu: Locator;
  readonly loadingOverlay: Locator;
  readonly errorAlert: Locator;
  readonly successToast: Locator;

  constructor(page: Page) {
    this.page             = page;
    this.sidebar          = page.locator('nav, aside, [role="navigation"]').first();
    this.notificationBell = page.locator('[data-testid="notification-bell"], button[aria-label*="notif" i]').first();
    this.notificationPanel= page.locator('[data-testid="notification-panel"], [role="dialog"]:has-text("Notification"), [role="dialog"]:has-text("Notif")').first();
    this.globalSearch     = page.locator('[data-testid="global-search"], input[placeholder*="Search" i], input[aria-label*="Search" i]').first();
    this.userMenu         = page.locator('[data-testid="user-menu"], button[aria-label*="user" i], button[aria-label*="account" i]').first();
    this.loadingOverlay   = page.locator('[role="progressbar"], [data-testid="spinner"], .loading-overlay').first();
    this.errorAlert       = page.locator('[role="alert"]:not([aria-hidden]), [data-testid="error-message"]').first();
    this.successToast     = page.locator('[role="status"], [data-testid="toast-success"], .Toastify__toast--success').first();
  }

  // ── Navigation helpers ─────────────────────────────────────────────────────

  async navigateTo(route: string): Promise<void> {
    await this.page.goto(route, { waitUntil: 'domcontentloaded' });
    await this.page.waitForLoadState('networkidle').catch(() => {});
  }

  async clickNavItem(label: string): Promise<void> {
    const link = this.page
      .locator(`nav a, aside a, [role="navigation"] a`)
      .filter({ hasText: label })
      .first();
    await link.click();
    await this.page.waitForLoadState('domcontentloaded');
  }

  // ── State helpers ──────────────────────────────────────────────────────────

  async waitForLoadingToFinish(): Promise<void> {
    // Wait for any spinner/progressbar to disappear
    await this.loadingOverlay.waitFor({ state: 'hidden', timeout: TIMEOUTS.default }).catch(() => {});
  }

  async expectNoErrorsVisible(): Promise<void> {
    const visible = await this.errorAlert.isVisible({ timeout: 2_000 }).catch(() => false);
    if (visible) {
      const text = await this.errorAlert.textContent();
      throw new Error(`Unexpected error on page: ${text}`);
    }
  }

  async expectSuccessToast(): Promise<void> {
    await expect(this.successToast).toBeVisible({ timeout: TIMEOUTS.default });
  }

  // ── Sidebar items ──────────────────────────────────────────────────────────

  async getSidebarLinks(): Promise<string[]> {
    await this.sidebar.waitFor({ state: 'visible', timeout: TIMEOUTS.default });
    const links = this.page.locator('nav a[href], aside a[href], [role="navigation"] a[href]');
    return links.allTextContents();
  }

  async isSidebarItemVisible(label: string): Promise<boolean> {
    return this.page
      .locator('nav, aside, [role="navigation"]')
      .locator(`a, button`)
      .filter({ hasText: new RegExp(label, 'i') })
      .first()
      .isVisible({ timeout: 3_000 })
      .catch(() => false);
  }

  // ── Notifications ──────────────────────────────────────────────────────────

  async openNotifications(): Promise<void> {
    await this.notificationBell.click();
    await this.notificationPanel.waitFor({ state: 'visible', timeout: TIMEOUTS.short });
  }

  async getUnreadNotificationCount(): Promise<number> {
    const badge = this.page.locator('[data-testid="notification-count"], .notification-badge').first();
    const text  = await badge.textContent({ timeout: 3_000 }).catch(() => '0');
    return parseInt(text ?? '0', 10) || 0;
  }

  // ── Page-level accessibility helpers ──────────────────────────────────────

  /** All buttons must have an accessible name. */
  async assertAllButtonsHaveNames(): Promise<void> {
    const buttons = this.page.locator('button:not([aria-hidden="true"])');
    const count   = await buttons.count();
    for (let i = 0; i < count; i++) {
      const btn  = buttons.nth(i);
      const name = await btn.getAttribute('aria-label')
        ?? await btn.textContent()
        ?? await btn.getAttribute('title')
        ?? '';
      expect(name.trim(), `Button #${i} missing accessible name`).toBeTruthy();
    }
  }

  /** All form inputs must have an associated label. */
  async assertAllInputsHaveLabels(): Promise<void> {
    const inputs = this.page.locator('input:not([type="hidden"]):not([aria-hidden="true"]), select, textarea');
    const count  = await inputs.count();
    for (let i = 0; i < count; i++) {
      const input  = inputs.nth(i);
      const id     = await input.getAttribute('id');
      const ariaLbl= await input.getAttribute('aria-label');
      const ariaBy = await input.getAttribute('aria-labelledby');
      const hasLabel = ariaLbl || ariaBy || (id && await this.page.locator(`label[for="${id}"]`).count() > 0);
      expect(hasLabel, `Input #${i} (id="${id}") missing label`).toBeTruthy();
    }
  }
}
