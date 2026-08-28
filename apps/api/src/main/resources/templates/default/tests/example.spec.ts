import { test, expect } from '@playwright/test';

test.describe('{{projectName}}', () => {
  test('example.com 타이틀 확인', async ({ page }) => {
    await page.goto('/');
    await expect(page).toHaveTitle(/Example Domain/);
  });
});
