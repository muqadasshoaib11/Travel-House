package com.travelhouse.tests;

import com.travelhouse.pages.HomePage;
import com.travelhouse.pages.InstallmentPlansPage;
import com.travelhouse.pages.PriceSummaryPage;
import com.travelhouse.pages.SearchResultsPage;
import com.travelhouse.pages.TravellerInfoPage;
import com.travelhouse.utils.ExtentReportManager;
import com.travelhouse.utils.PermissionDialog;
import com.travelhouse.utils.UiHelper;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Installment flow aligned with the Playwright "Travel House" chunk:
 * <ol>
 *   <li>Home → exact Recent Search London–Islamabad 2–30 Mar 2027 → Search</li>
 *   <li>Pay in Installment → plans 1–6 months</li>
 *   <li>For each plan: select → Terms → Proceed → second Terms → My Travellers
 *       (Cot / Fruit Meal / Blind Passenger / FF Muqadas / +92 / Phone / 09:00–12:00)
 *       → Continue → Summary Continue once → stop</li>
 *   <li>Back Home → next plan</li>
 * </ol>
 */
public class ReturnJuly2027SixInstallmentsSuiteTest extends AbstractReturnFlightSearchTest {

    private static final String ORIGIN = "London";
    private static final String DESTINATION = "Islamabad";
    private static final String DATE_RANGE = "2 Mar 2027 - 30 Mar 2027";

    @Test(priority = 1, timeOut = 300_000, description = "Login / reach Home")
    public void step01_loginOrHome() {
        loginOrHome();
    }

    @Test(priority = 2, timeOut = 4_200_000, dependsOnMethods = "step01_loginOrHome",
            description = "Plans 1–6: recent search → My Travellers → Summary Continue")
    public void step02_runEachInstallmentPlan() {
        for (int months = 1; months <= 6; months++) {
            ExtentReportManager.logInfo("=== " + months + "/6 month installment ===");
            searchViaRecentMarch2027();
            runOnePlanThroughSummary(months);
            if (months < 6) {
                navigateBackToHome();
            }
        }
        ExtentReportManager.logInfo("All 6 installment flows stopped after Summary Continue");
    }

    private void searchViaRecentMarch2027() {
        HomePage home = new HomePage();
        openHomeReady(home);
        home.chooseExactRecentSearch(ORIGIN, DESTINATION, DATE_RANGE);
        home.searchUntilPayInInstallment();
        PermissionDialog.dismissAll(1);

        SearchResultsPage results = new SearchResultsPage();
        // searchUntilPayInInstallment already opens the CTA; plans sheet may be visible
        if (!new InstallmentPlansPage().waitUntilDisplayed(8)) {
            Assert.assertTrue(results.waitForPayInInstallmentVisible(20),
                    "Pay in Installment required for " + DATE_RANGE);
            results.scrollResultsToTop();
            results.applyCheapestFilter();
            results.selectPayInInstallmentAtIndex(0);
            PermissionDialog.dismissAll(1);
        }
    }

    private void runOnePlanThroughSummary(int months) {
        InstallmentPlansPage plans = new InstallmentPlansPage();
        Assert.assertTrue(plans.waitUntilDisplayed(12), "Installment plans must show");
        plans.selectPlanByMonths(months);
        Assert.assertTrue(plans.acceptTermsAndContinue(3),
                "First Terms + Continue failed for " + months + " month");

        PriceSummaryPage summary = new PriceSummaryPage();
        long deadline = System.currentTimeMillis() + 90_000L;
        while (System.currentTimeMillis() < deadline) {
            PermissionDialog.dismissAll(1);
            if (UiHelper.isDescPresent("Who's Going")
                    || UiHelper.isDescPresent("My Travellers")
                    || UiHelper.isDescPresent("Contact Information")) {
                break;
            }
            if (UiHelper.isDescPresent("Pay in " + months + " Installments")
                    || UiHelper.isDescPresent("Book Now Pay Later")
                    || (UiHelper.isDescPresent("I accept")
                    && UiHelper.isDescPresent("month")
                    && !summary.hasProceedCta())) {
                plans.confirmPaymentTerms(months);
                continue;
            }
            if (summary.hasProceedCta() || summary.hasProceedWithPayment()) {
                summary.proceedPastSummary();
                continue;
            }
            if (summary.isDisplayed() || summary.hasTotalPrice()
                    || UiHelper.isDescPresent("Total Price")) {
                summary.acceptTermsAndConditions();
                summary.continueIfPresent();
            }
        }

        TravellerInfoPage traveller = new TravellerInfoPage();
        traveller.waitUntilVisible();
        Assert.assertTrue(traveller.isMyTravellersScreen() || traveller.isDisplayed(),
                "Should be on My Travellers");
        traveller.completeOnceSelectNameAndFill();
        traveller.continueThroughSummaryOnceAndStop();
        ExtentReportManager.logInfo(months + "-month plan stopped after Summary Continue");
    }

    private void navigateBackToHome() {
        for (int i = 0; i < 12; i++) {
            if (UiHelper.isDescPresent("Search Flight") || UiHelper.isDescPresent("Flying From")) {
                return;
            }
            try {
                com.travelhouse.base.DriverManager.getDriver().navigate().back();
            } catch (Exception ignored) {
                adbKeyEvent(4);
            }
            PermissionDialog.dismissAll(1);
        }
        HomePage home = new HomePage();
        home.bringAppToForeground();
        home.openHomeTab();
        Assert.assertTrue(home.isHomeDisplayed() || UiHelper.waitForDescContains("Search Flight", 3),
                "Could not return to Home");
    }
}
