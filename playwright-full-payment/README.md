# Travel House Android — Full Payment (correct script)

Playwright + Appium UiAutomator2. Source: Travel-House-Complete.zip full-payment flow.

## Scenarios
1. Return London→Jeddah, 23–30 September 2026, Cheapest
2. Return London→Islamabad, 23–30 September 2026, Fastest
3. One Way London→Dammam, 25 November 2026, Fastest

Each: red Pay £ → Proceed with payment → Terms → My Travellers (same details as installments) → Summary Continue → stop.

## Run
1. Copy .env.example to .env
2. `npm ci` then `npm run setup:android` once
3. `npm run appium` then `npm run test:full-payment`
