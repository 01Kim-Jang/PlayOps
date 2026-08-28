import type { Page } from '@playwright/test';

export class HomePage {
  constructor(private readonly page: Page) {}

  async goto() {
    await this.page.goto('/');
  }

  async expectTitle() {
    await this.page.waitForLoadState('domcontentloaded');
  }
}
