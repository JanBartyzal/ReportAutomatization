import { type Page, type Locator, expect } from '@playwright/test';
import { TIMEOUTS } from '../config/config';

export class FormsPage {
  readonly page: Page;
  readonly formList: Locator;
  readonly newFormBtn: Locator;
  readonly formCanvas: Locator;
  readonly fieldPalette: Locator;
  readonly autoSaveIndicator: Locator;
  readonly publishBtn: Locator;
  readonly previewBtn: Locator;
  readonly submitBtn: Locator;
  readonly validationErrors: Locator;

  constructor(page: Page) {
    this.page               = page;
    this.formList           = page.locator('table, [role="grid"], [data-testid="form-list"]').first();
    this.newFormBtn         = page.locator('button:has-text("New Form"), button:has-text("Create Form"), [data-testid="new-form-btn"]').first();
    this.formCanvas         = page.locator('[data-testid="form-canvas"], .form-builder-canvas, [data-testid="form-builder"]').first();
    this.fieldPalette       = page.locator('[data-testid="field-palette"], .field-palette, [aria-label*="field" i]').first();
    this.autoSaveIndicator  = page.locator('[data-testid="auto-save"], [aria-label*="auto-save" i], :has-text("Saved"), :has-text("Saving")').first();
    this.publishBtn         = page.locator('button:has-text("Publish"), button:has-text("Publikovat"), [data-testid="publish-btn"]').first();
    this.previewBtn         = page.locator('button:has-text("Preview"), button:has-text("Náhled"), [data-testid="preview-btn"]').first();
    this.submitBtn          = page.locator('button:has-text("Submit"), button:has-text("Odeslat"), [data-testid="form-submit-btn"]').first();
    this.validationErrors   = page.locator('[role="alert"], .field-error, [data-testid*="error"]');
  }

  async waitForFormList(): Promise<void> {
    await this.formList.waitFor({ state: 'visible', timeout: TIMEOUTS.default });
  }

  async openFormBuilder(): Promise<void> {
    await this.newFormBtn.click();
    await this.formCanvas.waitFor({ state: 'visible', timeout: TIMEOUTS.default });
  }

  /** Drag a field type from the palette onto the canvas. */
  async addFieldFromPalette(fieldType: string): Promise<void> {
    const field = this.fieldPalette.locator(`[data-field-type="${fieldType}"], :has-text("${fieldType}")`).first();
    await field.dragTo(this.formCanvas);
    // Wait for field to appear in canvas
    await this.page.waitForTimeout(500);
  }

  async fillFormField(label: string, value: string): Promise<void> {
    const input = this.page.locator(`label:has-text("${label}")`).locator('..').locator('input, textarea, select').first();
    await input.fill(value);
  }

  async expectValidationError(fieldLabel: string): Promise<void> {
    const errorNearField = this.page
      .locator(`label:has-text("${fieldLabel}")`)
      .locator('..')
      .locator('[role="alert"], .error, [class*="error"]')
      .first();
    await expect(errorNearField).toBeVisible({ timeout: TIMEOUTS.short });
  }

  async expectAutoSave(): Promise<void> {
    await expect(this.autoSaveIndicator).toBeVisible({ timeout: TIMEOUTS.default });
  }

  async getValidationErrorCount(): Promise<number> {
    return this.validationErrors.count();
  }
}
