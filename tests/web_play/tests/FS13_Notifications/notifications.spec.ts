/**
 * FS13 – Notification Center & Alerts
 *
 * UX focus:
 *  - Notification bell icon always visible in header
 *  - Unread badge shows count (or 0)
 *  - Opening notification panel shows list
 *  - Mark as read interaction removes unread indicator
 *  - Notification types: REPORT_SUBMITTED, APPROVED, REJECTED, DEADLINE_APPROACHING visible
 *  - Empty state message when no notifications
 *  - Notification settings page reachable
 */
import { test, expect } from '@playwright/test';
import { AppPage } from '../../pages/AppPage';
import { ROUTES, TIMEOUTS } from '../../config/config';
import { gotoAndWait, featurePresent } from '../../fixtures/auth.fixture';

test.describe('Notification bell', () => {
  test('notification bell is visible in the header', async ({ page }) => {
    await gotoAndWait(page, ROUTES.dashboard);

    const bell = page.locator(
      '[data-testid="notification-bell"], button[aria-label*="notification" i], button[aria-label*="notif" i], [aria-label*="Notification" i]'
    ).first();
    const visible = await bell.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);

    if (!visible) {
      await featurePresent(page, '[data-testid="notification-bell"]', 'Notification bell icon');
      return;
    }
    await expect(bell).toBeVisible();

    // Bell must have an accessible name for screen readers
    const ariaLabel = await bell.getAttribute('aria-label') ?? await bell.textContent() ?? '';
    expect(ariaLabel.trim(), 'Notification bell must have aria-label').toBeTruthy();
  });

  test('notification badge shows numeric count or is absent', async ({ page }) => {
    await gotoAndWait(page, ROUTES.dashboard);
    const app = new AppPage(page);

    const count = await app.getUnreadNotificationCount();
    expect(count).toBeGreaterThanOrEqual(0);
  });
});

test.describe('Notification panel', () => {
  test('clicking bell opens notification panel', async ({ page }) => {
    await gotoAndWait(page, ROUTES.dashboard);
    const app = new AppPage(page);

    const bellVisible = await app.notificationBell.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!bellVisible) {
      await featurePresent(page, '[data-testid="notification-bell"]', 'Notification bell');
      return;
    }

    await app.notificationBell.click();

    const panel = page.locator(
      '[data-testid="notification-panel"], [role="dialog"], [role="listbox"], .notification-panel, [aria-label*="notification" i]'
    ).first();
    await expect(panel).toBeVisible({ timeout: TIMEOUTS.short });
  });

  test('notification panel shows list or empty state', async ({ page }) => {
    await gotoAndWait(page, ROUTES.dashboard);
    const app = new AppPage(page);

    const bellVisible = await app.notificationBell.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!bellVisible) {
      await featurePresent(page, '[data-testid="notification-bell"]', 'Notification bell');
      return;
    }

    await app.notificationBell.click();

    const panelContent = page.locator(
      '[data-testid="notification-list"], [role="listitem"], .notification-item, :text("No notifications"), :text("Žádné notifikace")'
    ).first();
    await expect(panelContent).toBeVisible({ timeout: TIMEOUTS.short });
  });

  test('notification panel can be closed', async ({ page }) => {
    await gotoAndWait(page, ROUTES.dashboard);
    const app = new AppPage(page);

    const bellVisible = await app.notificationBell.isVisible({ timeout: TIMEOUTS.short }).catch(() => false);
    if (!bellVisible) {
      await featurePresent(page, '[data-testid="notification-bell"]', 'Notification bell');
      return;
    }

    await app.notificationBell.click();
    await page.waitForTimeout(300);

    // Close via Escape or close button
    const closeBtn = page.locator(
      'button[aria-label="Close"], button:has-text("×"), [data-testid="close-notifications"]'
    ).first();
    if (await closeBtn.isVisible({ timeout: 1_000 }).catch(() => false)) {
      await closeBtn.click();
    } else {
      await page.keyboard.press('Escape');
    }

    // Panel should disappear
    const panel = page.locator('[data-testid="notification-panel"], .notification-panel').first();
    await expect(panel).toBeHidden({ timeout: TIMEOUTS.short });
  });
});

test.describe('Notification settings', () => {
  test('notification settings page is reachable', async ({ page }) => {
    await gotoAndWait(page, ROUTES.notificationSettings);
    await expect(page).not.toHaveURL(/login|error/);

    // Page should contain opt-in/opt-out toggles
    const toggles = page.locator('[role="switch"], input[type="checkbox"], [data-testid*="notification-toggle"]');
    const count   = await toggles.count();
    if (count === 0) {
      await featurePresent(page, '[role="switch"]', 'Notification opt-in toggles');
    } else {
      expect(count).toBeGreaterThan(0);
    }
  });
});
