import { BasePage } from './base.page.js';
import { config } from '../config.js';

export class HomePage extends BasePage {
  async openHome() {
    await this.driver.terminateApp(config.android.appPackage).catch(() => {});
    await this.driver.activateApp(config.android.appPackage);
    await this.tapA11y('Home\nTab 1 of 4').catch(() => {});
    await this.byA11y('Flying From\nLondon, United Kingdom', 60_000);
  }

  async chooseExactRecentSearch() {
    // A cold Flutter launch can expose Home semantics just before its tap
    // handler is ready. Retry only this navigation gesture, never the plan.
    let opened = false;
    for (let attempt = 1; attempt <= 3 && !opened; attempt += 1) {
      await this.tapContains('Going to');
      opened = await this.driver.waitUntil(async () => {
        const matches = await this.driver.$$(`android=new UiSelector().descriptionContains("2 Mar 2027 - 30 Mar 2027")`);
        return matches.length > 0;
      }, { timeout: 7_000, interval: 300 }).catch(() => false);
    }
    if (!opened) throw new Error('Recent-search page did not open after 3 attempts');
    const candidates = await this.driver.$$(`android=new UiSelector().descriptionContains("2 Mar 2027 - 30 Mar 2027")`);
    let recent;
    for (const candidate of candidates) {
      const label = await candidate.getAttribute('content-desc');
      if (label?.includes(config.flight.from) && label?.includes(config.flight.to)) {
        recent = candidate;
        break;
      }
    }
    if (!recent) {
      throw new Error('The exact recent search is unavailable. Seed London–Islamabad, 2–30 March 2027 once before running the suite.');
    }
    await this.tapElement(recent);
    await this.byA11y('Going to\nIslamabad, Pakistan', 15_000);
  }

  async selectTripType(type) {
    await this.tapA11y(type);
    await this.byA11y(type);
  }

  async selectDestination({ city, iata }) {
    await this.tapContains('Going to');
    const search = await this.driver.$('android=new UiSelector().className("android.widget.EditText")');
    await search.waitForDisplayed({ timeout: 15_000 });
    await this.tapElement(search);
    await search.clearValue();
    await search.addValue(city);
    const result = await this.driver.$(`android=new UiSelector().descriptionContains(${JSON.stringify(`${iata} – ${city}`)})`);
    await result.waitForDisplayed({ timeout: 30_000 });
    await this.tapElement(result);
    await this.driver.hideKeyboard().catch(() => {});
    const selected = await this.driver.$(`android=new UiSelector().descriptionContains(${JSON.stringify(`${city} – ${iata}`)})`);
    await selected.waitForDisplayed({ timeout: 15_000 });
  }

  async selectDates({ departureLabel, returnLabel }) {
    await this.tapContains('Departure');
    const departure = await this.scrollToDescription(departureLabel);
    await departure.waitForDisplayed({ timeout: 30_000 });
    await this.tapElement(departure);
    if (returnLabel) {
      const returning = await this.scrollToDescription(returnLabel);
      await returning.waitForDisplayed({ timeout: 30_000 });
      await this.tapElement(returning);
    }
    for (let attempt = 1; attempt <= 2; attempt += 1) {
      await this.tapA11y('Apply');
      const closed = await this.byA11y('Search Flight', 5_000).catch(() => null);
      if (closed) return;
    }
    throw new Error('Date picker did not close after Apply');
  }

  async searchForFullPayment() {
    await this.scrollToDescription('Search Flight');
    for (let attempt = 1; attempt <= 3; attempt += 1) {
      await this.tapA11y('Search Flight');
      const outcome = await this.driver.waitUntil(async () => {
        if ((await this.driver.$$('~Cheapest')).length) return 'ready';
        if ((await this.driver.$$(`android=new UiSelector().descriptionContains("unable to process")`)).length) return 'backend-error';
        return false;
      }, { timeout: 120_000, interval: 500 }).catch(() => false);
      if (outcome === 'ready') return;
      if (outcome === 'backend-error') {
        await this.tapA11y('OK');
        await this.byA11y('Search Flight', 15_000);
      }
    }
    throw new Error('Flight search did not show Cheapest/Fastest after 3 attempts');
  }

  async searchFlight() {
    await this.scrollToDescription('Search Flight');
    for (let attempt = 1; attempt <= 3; attempt += 1) {
      await this.tapA11y('Search Flight');
      const outcome = await this.driver.waitUntil(async () => {
        if ((await this.driver.$$('~Pay in Installment')).length) return 'ready';
        if ((await this.driver.$$(`android=new UiSelector().descriptionContains("unable to process")`)).length) return 'backend-error';
        return false;
      }, { timeout: 120_000, interval: 500 }).catch(() => false);

      if (outcome === 'ready') {
        await this.tapA11y('Pay in Installment');
        return;
      }
      if (outcome === 'backend-error') {
        await this.tapA11y('OK');
        await this.byA11y('Search Flight', 15_000);
      }
    }
    throw new Error('Flight search did not reach Pay in Installment after 3 attempts');
  }
}
