import { BasePage } from './base.page.js';
import { config } from '../config.js';

export class TravellersPage extends BasePage {
  async selectDropdown(currentLabel, option) {
    await this.tapA11y(currentLabel);
    let optionElement = await this.byA11y(option, 5_000).catch(() => null);
    if (!optionElement) {
      await this.tapA11y(currentLabel);
      optionElement = await this.byA11y(option, 30_000);
    }
    await this.tapElement(optionElement);
    await this.byA11y(option, 10_000);
  }

  async complete() {
    await this.selectDropdown('Please Select a Saved Traveller', config.traveller.savedName);
    await this.tapA11y('Add Special Request');
    await this.selectDropdown('Any', 'Cot');
    await this.selectDropdown('Any meal', 'Fruit Meal');
    await this.selectDropdown('No Special Service Requested', 'Blind Passenger');

    await this.scrollToDescription('Contact Information');
    const contactHeading = await this.byA11y('Contact Information');
    const { y: contactY } = await contactHeading.getLocation();
    const editTexts = await this.driver.$$('android=new UiSelector().className("android.widget.EditText")');
    let frequentField;
    for (const item of editTexts) {
      if (!(await item.isDisplayed())) continue;
      const value = await item.getText();
      const { y } = await item.getLocation();
      if (!value && y < contactY) {
        frequentField = item;
        break;
      }
    }
    if (!frequentField) throw new Error('Frequent Flyer Information field was not found');
    await this.fillAndVerify(frequentField, config.traveller.frequentFlyerName);
    await this.driver.hideKeyboard().catch(() => {});

    await this.scrollToDescription('Mobile Number');
    const currentCountry = await this.driver.$('android=new UiSelector().className("android.widget.ImageView").descriptionStartsWith("+")');
    await currentCountry.waitForDisplayed({ timeout: 15_000 });
    await this.tapElement(currentCountry);
    // Use the country picker's search control as requested. The result's name
    // can be localized, so identify Pakistan by its stable +92 calling code.
    const { width, height } = await this.driver.getWindowSize();
    await this.tapPoint(Math.round(width * 0.5), Math.round(height * 0.17));
    const countrySearch = await this.driver.$('android=new UiSelector().className("android.widget.EditText")');
    await countrySearch.waitForDisplayed({ timeout: 10_000 });
    await this.fillAndVerify(countrySearch, 'Pakistan');
    const pakistan = await this.driver.$('android=new UiSelector().descriptionContains("+92")');
    await pakistan.waitForDisplayed({ timeout: 30_000 });
    await this.tapElement(pakistan);

    await this.driver.hideKeyboard().catch(() => {});
    await this.scrollToDescription('Mobile Number');
    const mobileLabel = await this.byA11y('Mobile Number *');
    const { y: mobileLabelY } = await mobileLabel.getLocation();
    const fields = await this.driver.$$('android=new UiSelector().className("android.widget.EditText")');
    let mobileField;
    for (const field of fields) {
      if (await field.isDisplayed()) {
        const value = await field.getText();
        const { y } = await field.getLocation();
        if ((!value || /mobile/i.test(value)) && y > mobileLabelY) {
          mobileField = field;
          break;
        }
      }
    }
    if (!mobileField) throw new Error('Mobile number field was not found');
    if (!/^3\d{9}$/.test(config.traveller.mobile)) {
      throw new Error('MOBILE_NUMBER must contain 10 digits and start with 3');
    }
    await this.fillAndVerify(mobileField, config.traveller.mobile);
    await this.driver.hideKeyboard().catch(() => {});

    await this.scrollToDescription('How to Contact');
    await this.selectDropdown('Any (Phone + Email)', 'Phone');
    await this.selectDropdown('Any Time', '09:00 to 12:00');

    // Continue from My Travellers, verify Summary, continue once, then stop.
    await this.tapA11y('Continue');
    await this.driver.waitUntil(async () => {
      const source = await this.driver.getPageSource();
      return /summary/i.test(source);
    }, { timeout: 30_000, interval: 500, timeoutMsg: 'Summary screen did not appear after Continue' });
    await this.tapA11y('Continue', 30_000);
  }
}
