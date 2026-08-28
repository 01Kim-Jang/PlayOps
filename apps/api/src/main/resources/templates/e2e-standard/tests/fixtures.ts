import { test as base } from '@playwright/test';

export const test = base.extend({
  // 공통 fixture 확장 지점
});

export { expect } from '@playwright/test';
