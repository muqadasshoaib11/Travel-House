import { BasePage } from './base.page.js';

export class InstallmentPage extends BasePage {
  async selectPlan(months) {
    await this.tapContains(`${months} month`);
    // Flutter exposes this checkbox as an unlabeled View immediately to the
    // left of the label, so address it by a stable offset from that label.
    const termsLabel = await this.byA11y('I accept ');
    const { x, y } = await termsLabel.getLocation();
    const { height } = await termsLabel.getSize();
    await this.tapPoint(x - 45, Math.round(y + height / 2));
    const continueButton = await this.driver.$('~Continue');
    await this.driver.waitUntil(
      async () => (await continueButton.getAttribute('clickable')) === 'true',
      { timeout: 15_000, interval: 250, timeoutMsg: 'Terms checkbox did not enable Continue' },
    );
    await this.tapElement(continueButton);
    await this.tapContains('Proceed with payment', 90_000);
  }

  async confirmPaymentTerms(months) {
    await this.tapA11y(`Pay in ${months} Installments`);
    const checkbox = await this.driver.$('android=new UiSelector().className("android.widget.CheckBox")');
    await checkbox.waitForDisplayed({ timeout: 30_000 });
    if (!(await checkbox.isSelected())) await this.tapElement(checkbox);
    const continueButton = await this.driver.$('~Continue');
    await continueButton.waitForEnabled({ timeout: 15_000 });
    await this.tapElement(continueButton);
  }
}
