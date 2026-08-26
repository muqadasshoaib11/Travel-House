# Travel House Android — Installments (correct script)

Playwright + Appium UiAutomator2. Source: Travel-House-Complete.zip installment flow.

## Flow
Six serial tests (1–6 months). London→Islamabad, 2–30 March 2027 via Recent Search.
Select plan → both Terms → My Travellers (Muqadas Shoaib, Cot, Fruit Meal, Blind Passenger, FF Muqadas, +92, Phone, 09:00–12:00) → Summary Continue → stop.

Seat on current build: **Cot** (no Code).

## Run
1. Copy .env.example to .env
2. `npm ci` then `npm run setup:android` once
3. `npm run appium` then `npm run test:installments`
