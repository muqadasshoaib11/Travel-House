import { test, expect } from '@playwright/test';
import { createMobileSession } from '../src/mobile-session.js';
import { HomePage } from '../src/pages/home.page.js';
import { InstallmentPage } from '../src/pages/installment.page.js';
import { TravellersPage } from '../src/pages/travellers.page.js';

test.describe.configure({ mode: 'serial' });

for (const months of [1, 2, 3, 4, 5, 6]) {
  test(`${months}-month installment flow stops after Summary Continue`, async ({}, testInfo) => {
    const driver = await createMobileSession();
    try {
      const home = new HomePage(driver);
      const installment = new InstallmentPage(driver);
      const travellers = new TravellersPage(driver);

      await home.openHome();
      await home.chooseExactRecentSearch();
      await home.searchFlight();
      await installment.selectPlan(months);
      await installment.confirmPaymentTerms(months);
      await travellers.complete();

      expect(testInfo.status).toBe('passed');
    } catch (error) {
      const png = await driver.takeScreenshot().catch(() => null);
      if (png) await testInfo.attach('android-failure.png', { body: Buffer.from(png, 'base64'), contentType: 'image/png' });
      throw error;
    } finally {
      await driver.deleteSession().catch(() => {});
    }
  });
}
