# Travel House Android installment automation

This project uses **Playwright Test (JavaScript)** for test definition, parameterization, assertions, reporting, retries, and artifacts. The uploaded build is a native Android APK, so native element interaction is performed through **Appium + UiAutomator2**; Playwright cannot directly control APK widgets.

## Covered flow

The suite runs six serial tests (1–6 months), exactly once per plan. Each test searches London to Islamabad for 2–30 March 2027, chooses the relevant installment plan, accepts both sets of terms, proceeds to My Travellers, selects Muqadas Shoaib, Cot, Fruit Meal, Blind Passenger, enters Muqadas under Frequent Flyer Information, searches for Pakistan in the country picker, selects +92, enters and verifies a 10-digit mobile number beginning with 3, then selects Phone and 09:00–12:00. It clicks My Travellers Continue, verifies that the Summary screen appears, clicks Continue once on Summary, and performs no further action.

The current build exposes the seat value as **Cot**. There is no **Code** option.

## Setup

1. Copy `.env.example` to `.env` and place the supplied test credentials in `.env`.
2. Run `npm ci` (or `npm install` when no lockfile exists).
3. Run `npm run setup:android` once. The included startup script configures the local Android SDK and JDK paths automatically.
4. Start Appium in a separate terminal with `npm run appium`.
5. Run `npm run test:installments`.
6. Open the HTML report with `npm run report`.

The exact requested route is selected from Recent Searches because the connected test account already contains it. This avoids fragile month-by-month calendar navigation and preserves the dates exactly.
