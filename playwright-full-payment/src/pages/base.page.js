export class BasePage {
  constructor(driver) { this.driver = driver; }

  async byA11y(label, timeout = 30_000) {
    const el = await this.driver.$(`~${label}`);
    await el.waitForDisplayed({ timeout });
    return el;
  }

  async tapA11y(label, timeout) {
    await this.tapElement(await this.byA11y(label, timeout));
  }

  async tapElement(el) {
    // Flutter semantics nodes sometimes report clickable but ignore WebDriver's
    // element click. A short W3C touch gesture at the node centre is reliable
    // on physical Android devices while remaining independent of screen size.
    const { x, y } = await el.getLocation();
    const { width, height } = await el.getSize();
    await this.tapPoint(Math.round(x + width / 2), Math.round(y + height / 2));
  }

  async tapPoint(x, y) {
    await this.driver.performActions([{
      type: 'pointer',
      id: 'finger',
      parameters: { pointerType: 'touch' },
      actions: [
        { type: 'pointerMove', duration: 0, x, y },
        { type: 'pointerDown', button: 0 },
        { type: 'pause', duration: 100 },
        { type: 'pointerUp', button: 0 },
      ],
    }]);
    await this.driver.releaseActions();
  }

  async tapContains(text, timeout = 30_000) {
    const selector = `android=new UiSelector().descriptionContains(${JSON.stringify(text)})`;
    const el = await this.driver.$(selector);
    await el.waitForDisplayed({ timeout });
    await this.tapElement(el);
  }

  async scrollToDescription(text) {
    const selector = `android=new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().descriptionContains(${JSON.stringify(text)}))`;
    return this.driver.$(selector);
  }

  async fillFirstEmptyEditText(value) {
    const fields = await this.driver.$$('android=new UiSelector().className("android.widget.EditText")');
    for (const field of fields) {
      if (await field.isDisplayed()) {
        const current = await field.getText();
        if (!current || /enter|search/i.test(current)) {
          await field.click();
          await field.setValue(value);
          return;
        }
      }
    }
    throw new Error(`No visible empty text field found for ${value}`);
  }

  async fillAndVerify(field, value) {
    await this.tapElement(field);
    await field.clearValue().catch(() => {});
    await field.setValue(value);
    await this.driver.waitUntil(async () => {
      const actual = await field.getText();
      return actual.replace(/\s/g, '').includes(value.replace(/\s/g, ''));
    }, { timeout: 10_000, interval: 250, timeoutMsg: `Field did not retain value: ${value}` });
  }
}
