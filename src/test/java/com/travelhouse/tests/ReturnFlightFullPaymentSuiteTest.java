package com.travelhouse.tests;

import com.travelhouse.config.ConfigReader;
import com.travelhouse.utils.ExtentReportManager;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDate;

/**
 * Scenario A — Full Payment (departure less than 2 months, default ~7 days):
 * Login → Return search → validate listings →
 * A1 Cheapest + Full Payment → back →
 * A2 Fastest + Full Payment.
 *
 * Branch: feature/return-search-full-payment
 */
public class ReturnFlightFullPaymentSuiteTest extends AbstractReturnFlightSearchTest {

    @Test(priority = 1, timeOut = 600_000, description = "Login (or continue if already on Home)")
    public void step01_loginOrHome() {
        Assert.assertFalse(ConfigReader.get("fare.selection.mode", "full").toLowerCase().contains("install"),
                "Full Payment suite requires fare.selection.mode=full");
        ExtentReportManager.logInfo("Scenario A — Full Payment (near-term dates)");
        loginOrHome();
    }

    @Test(priority = 2, timeOut = 600_000, dependsOnMethods = "step01_loginOrHome",
            description = "Return search with near-term dates and validate listings")
    public void step02_returnSearch_nearTerm_validateResults() {
        LocalDate departure = resolveDepartureDate();
        LocalDate returnDate = departure.plusDays(resolveReturnDaysAfter());
        ExtentReportManager.logInfo("Near-term dates for Full Payment: " + departure + " → " + returnDate);
        performReturnSearchWithDates(departure, returnDate);
    }

    @Test(priority = 3, timeOut = 600_000, dependsOnMethods = "step02_returnSearch_nearTerm_validateResults",
            description = "A1: Cheapest + Full Payment")
    public void step03_cheapest_fullPayment() {
        selectFullPaymentPathFromResults("Cheapest");
    }

    @Test(priority = 4, timeOut = 300_000, dependsOnMethods = "step03_cheapest_fullPayment",
            description = "Return to search results after Full Payment path")
    public void step04_backToResults() {
        navigateBackToResults();
        Assert.assertTrue(new com.travelhouse.pages.SearchResultsPage().hasResults(),
                "Should be back on search results");
    }

    @Test(priority = 5, timeOut = 600_000, dependsOnMethods = "step04_backToResults",
            description = "A2: Fastest + Full Payment")
    public void step05_fastest_fullPayment() {
        selectFullPaymentPathFromResults("Fastest");
    }
}
