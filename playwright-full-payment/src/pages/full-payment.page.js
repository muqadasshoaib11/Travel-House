import { BasePage } from './base.page.js';

export class FullPaymentPage extends BasePage {
  async selectFareAndPay(fareTab) {
    await this.tapA11y(fareTab);
    for (let attempt = 1; attempt <= 2; attempt += 1) {
      const pay = await this.driver.$('android=new UiSelector().descriptionContains("Pay £")');
      await pay.waitForDisplayed({ timeout: 30_000 });
      const { x, y } = await pay.getLocation();
      const { width, height } = await pay.getSize();
      if (height < 250) {
        // One Way exposes the red Pay button as its own semantics node.
        await this.tapElement(pay);
      } else {
        // Return merges the whole card; Pay is at the lower-right edge.
        await this.tapPoint(Math.round(x + width * 0.82), Math.round(y + height * 0.9));
      }
      const proceed = await this.byA11y('Proceed with payment', 10_000).catch(() => null);
      if (proceed) {
        await this.tapElement(proceed);
        return;
      }
    }
    throw new Error('The red full-payment Pay button did not open fare details');
  }

  async acceptTermsAndContinue() {
    const termsLabel = await this.byA11y('I accept ', 30_000);
    const { x, y } = await termsLabel.getLocation();
    const { height } = await termsLabel.getSize();
    await this.tapPoint(x - 45, Math.round(y + height / 2));

    const continueButton = await this.driver.$('~Continue');
    await this.driver.waitUntil(
      async () => (await continueButton.getAttribute('clickable')) === 'true',
      { timeout: 15_000, interval: 250, timeoutMsg: 'Full-payment terms did not enable Continue' },
    );
    await this.tapElement(continueButton);
    await this.byA11y('Please Select a Saved Traveller', 60_000);
  }
}
