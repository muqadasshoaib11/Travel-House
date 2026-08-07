package com.travelhouse.tests;

import com.travelhouse.config.ConfigReader;
import com.travelhouse.pages.SearchResultsPage;
import com.travelhouse.utils.ExtentReportManager;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.List;

/**
 * Scenario B — Installments (departure ~3 months ahead):
 * Login → Return search → validate listings → discover all installment plans →
 * For EACH plan: Cheapest + plan, then Fastest + plan.
 *
 * Branch: feature/return-search-installments
 */
public class ReturnFlightInstallmentsSuiteTest extends AbstractReturnFlightSearchTest {

    private List<String> discoveredPlans;

    @Test(priority = 1, timeOut = 600_000, description = "Login (or continue if already on Home)")
    public void step01_loginOrHome() {
        Assert.assertTrue(ConfigReader.get("fare.selection.mode", "").toLowerCase().contains("install"),
                "Installments suite requires fare.selection.mode=installment");
        ExtentReportManager.logInfo("Scenario B — Installments (far-out dates, all plans)");
        loginOrHome();
    }

    @Test(priority = 2, timeOut = 600_000, dependsOnMethods = "step01_loginOrHome",
            description = "Return search with ~3 month dates and validate listings")
    public void step02_returnSearch_farOut_validateResults() {
        LocalDate departure = resolveDepartureDate();
        LocalDate returnDate = departure.plusDays(resolveReturnDaysAfter());
        ExtentReportManager.logInfo("Far-out dates for Installments: " + departure + " → " + returnDate);
        performReturnSearchWithDates(departure, returnDate);
        Assert.assertTrue(new SearchResultsPage().isPayInInstallmentVisible(),
                "Pay in Installment CTA must appear on far-out results");
    }

    @Test(priority = 3, timeOut = 600_000, dependsOnMethods = "step02_returnSearch_farOut_validateResults",
            description = "Discover all available installment plans dynamically")
    public void step03_discoverInstallmentPlans() {
        discoveredPlans = discoverInstallmentPlansFromResults();
        ExtentReportManager.logInfo("Will run Cheapest+Fastest for each of "
                + discoveredPlans.size() + " plan(s): " + discoveredPlans);
    }

    @Test(priority = 4, timeOut = 1_800_000, dependsOnMethods = "step03_discoverInstallmentPlans",
            description = "For each installment plan: Cheapest then Fastest")
    public void step04_allPlans_cheapestAndFastest() {
        Assert.assertNotNull(discoveredPlans, "Plans must be discovered first");
        Assert.assertFalse(discoveredPlans.isEmpty(), "No installment plans discovered");

        int planIndex = 0;
        for (String plan : discoveredPlans) {
            planIndex++;
            ExtentReportManager.logInfo("=== Plan " + planIndex + "/" + discoveredPlans.size()
                    + ": " + plan + " ===");

            selectInstallmentPlanPath("Cheapest", plan);
            navigateBackToResults();

            selectInstallmentPlanPath("Fastest", plan);
            if (planIndex < discoveredPlans.size()) {
                navigateBackToResults();
            }
        }
        ExtentReportManager.logInfo("Completed all installment plans × Cheapest/Fastest");
    }
}
