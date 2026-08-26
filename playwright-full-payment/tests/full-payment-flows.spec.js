import { test } from '@playwright/test';
import { createMobileSession } from '../src/mobile-session.js';
import { HomePage } from '../src/pages/home.page.js';
import { FullPaymentPage } from '../src/pages/full-payment.page.js';
import { TravellersPage } from '../src/pages/travellers.page.js';

test.describe.configure({ mode: 'serial' });

const scenarios = [
  {
    name: 'Return London to Jeddah using Cheapest',
    tripType: 'Return', city: 'Jeddah', iata: 'JED', fareTab: 'Cheapest',
    departureLabel: 'Wed, 23 September 2026',
    returnLabel: 'Wed, 30 September 2026',
  },
  {
    name: 'Return London to Islamabad using Fastest',
    tripType: 'Return', city: 'Islamabad', iata: 'ISB', fareTab: 'Fastest',
    departureLabel: 'Wed, 23 September 2026',
    returnLabel: 'Wed, 30 September 2026',
  },
  {
    name: 'One Way London to Dammam using Fastest',
    tripType: 'One Way', city: 'Dammam', iata: 'DMM', fareTab: 'Fastest',
    departureLabel: 'Wed, 25 November 2026',
  },
];

for (const scenario of scenarios) {
  test(`${scenario.name} stops after Traveller Summary Continue`, async ({}, testInfo) => {
    const driver = await createMobileSession();
    try {
      const home = new HomePage(driver);
      const payment = new FullPaymentPage(driver);
      const travellers = new TravellersPage(driver);

      await home.openHome();
      await home.selectTripType(scenario.tripType);
      await home.selectDestination(scenario);
      await home.selectDates(scenario);
      await home.searchForFullPayment();
      await payment.selectFareAndPay(scenario.fareTab);
      await payment.acceptTermsAndContinue();
      await travellers.complete();
    } catch (error) {
      const png = await driver.takeScreenshot().catch(() => null);
      if (png) await testInfo.attach('android-failure.png', { body: Buffer.from(png, 'base64'), contentType: 'image/png' });
      throw error;
    } finally {
      await driver.deleteSession().catch(() => {});
    }
  });
}
