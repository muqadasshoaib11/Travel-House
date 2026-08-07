package com.travelhouse.tests;

import com.travelhouse.base.DriverManager;
import com.travelhouse.base.JourneyBaseTest;
import com.travelhouse.config.Credentials;
import com.travelhouse.config.TestDataReader;
import com.travelhouse.pages.AirportPickerPage;
import com.travelhouse.pages.FareSelectionPage;
import com.travelhouse.pages.HomePage;
import com.travelhouse.pages.LoginPage;
import com.travelhouse.pages.OnboardingPage;
import com.travelhouse.pages.PriceSummaryPage;
import com.travelhouse.pages.SearchResultsPage;
import com.travelhouse.utils.DevicePrep;
import com.travelhouse.utils.ExtentReportManager;
import com.travelhouse.utils.PermissionDialog;
import com.travelhouse.utils.UiHelper;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Return flight search CI suite:
 * Login → Return search → validate listings →
 * Cheapest + Full Payment → back →
 * Fastest + Installments.
 */
public class ReturnFlightSearchSuiteTest extends JourneyBaseTest {

    @Test(priority = 1, timeOut = 600_000, description = "Login (or continue if already on Home)")
    public void step01_loginOrHome() {
        if (!Credentials.isConfigured()) {
            throw new SkipException("Configure login.email / login.password in config.properties");
        }

        HomePage home = new HomePage();
        home.waitForUiReady();
        PermissionDialog.dismissAll(6);
        recoverToHomeOrLogin(home);

        if (home.isHomeDisplayed()) {
            PermissionDialog.handlePostLoginDialogs();
            openHomeReady(home);
            Assert.assertTrue(home.isHomeDisplayed(), "Home should be visible when already logged in");
            ExtentReportManager.logInfo("Already logged in — Home visible");
            return;
        }

        OnboardingPage onboarding = new OnboardingPage();
        if (onboarding.isVisible()) {
            onboarding.dismissToLogin();
        }
        PermissionDialog.dismissAll(3);

        LoginPage login = new LoginPage();
        if (login.isLoginScreenVisible()) {
            login.waitUntilVisible();
            login.login(Credentials.email(), Credentials.password());
            pause(4000);
            PermissionDialog.handlePostLoginDialogs();
            pause(2000);
            if (login.isStillOnLogin()) {
                login.login(Credentials.email(), Credentials.password());
                pause(4000);
                PermissionDialog.handlePostLoginDialogs();
            }
            Assert.assertFalse(login.isStillOnLogin(), "Login should leave the login screen");
        }

        PermissionDialog.handlePostLoginDialogs();
        recoverToHomeOrLogin(home);
        openHomeReady(home);
        Assert.assertTrue(home.isHomeDisplayed(), "Home should be displayed after login");
        ExtentReportManager.logInfo("Logged in — Home visible");
    }

    @Test(priority = 2, timeOut = 600_000, dependsOnMethods = "step01_loginOrHome",
            description = "Return search London → Jeddah and validate flight listings")
    public void step02_returnSearch_validateResults() {
        String origin = TestDataReader.get("flight.origin", "London");
        String destination = TestDataReader.get("flight.return.destination", "Jeddah");
        String destinationQuery = TestDataReader.get("flight.return.destination.query", "Jeddah");

        performReturnSearch(origin, destination, destinationQuery);

        SearchResultsPage results = new SearchResultsPage();
        results.waitForResults();
        Assert.assertTrue(results.hasResults(), "Return search results should display");
        Assert.assertTrue(results.getResultCountEstimate() > 0 || results.hasResults(),
                "Flight listings should be visible for " + origin + " → " + destination);
        results.validateAllListingsHaveRequiredFields(destination);
        ExtentReportManager.logInfo("Return results validated for " + origin + " → " + destination);
    }

    @Test(priority = 3, timeOut = 600_000, dependsOnMethods = "step02_returnSearch_validateResults",
            description = "Select Cheapest flight and choose Full Payment fare")
    public void step03_cheapest_fullPayment() {
        SearchResultsPage results = new SearchResultsPage();
        Assert.assertTrue(results.isResultsScreen(), "Should still be on search results");

        results.scrollResultsToTop();
        results.applyCheapestFilter();
        Assert.assertTrue(results.getResultCountEstimate() > 0 || results.hasResults(),
                "Listings should display after Cheapest filter");
        results.selectFirstFlight();
        pause(3000);
        PermissionDialog.dismissAll(2);

        chooseFareAndConfirm("full");
        ExtentReportManager.logInfo("Cheapest + Full Payment path completed");
    }

    @Test(priority = 4, timeOut = 300_000, dependsOnMethods = "step03_cheapest_fullPayment",
            description = "Return to search results after Full Payment path")
    public void step04_backToResults() {
        navigateBackToResults();
        SearchResultsPage results = new SearchResultsPage();
        Assert.assertTrue(results.hasResults(), "Should be back on search results");
        ExtentReportManager.logInfo("Back on Return search results");
    }

    @Test(priority = 5, timeOut = 600_000, dependsOnMethods = "step04_backToResults",
            description = "Select Fastest flight and choose Installments fare")
    public void step05_fastest_installments() {
        SearchResultsPage results = new SearchResultsPage();
        results.scrollResultsToTop();
        results.applyFastestFilter();
        Assert.assertTrue(results.getResultCountEstimate() > 0 || results.hasResults(),
                "Listings should display after Fastest filter");
        results.selectFirstFlight();
        pause(3000);
        PermissionDialog.dismissAll(2);

        chooseFareAndConfirm("installment");
        ExtentReportManager.logInfo("Fastest + Installments path completed");
    }

    private void chooseFareAndConfirm(String fareType) {
        FareSelectionPage fare = new FareSelectionPage();
        PriceSummaryPage summary = new PriceSummaryPage();

        // Some flights skip fare screen and land on price summary directly
        if (fare.isDisplayed()) {
            fare.assertBothFareOptionsVisible();
            ExtentReportManager.logInfo("Fare screen shows Full Payment and Installments");
            if (fareType.toLowerCase().contains("install")) {
                fare.selectInstallments();
            } else {
                fare.selectFullPayment();
            }
            fare.continueIfPresent();
            pause(2000);
        } else {
            ExtentReportManager.logInfo("Fare screen not shown for this flight — continuing to summary");
        }

        pause(2000);
        if (summary.hasProceedWithPayment()) {
            Assert.assertFalse(summary.getDisplayedTextsSnapshot().isEmpty(),
                    "Outbound / price content should be visible");
            summary.proceedWithPayment();
            pause(2500);
        }

        Assert.assertTrue(
                summary.isDisplayed() || summary.hasTotalPrice() || summary.hasProceedWithPayment()
                        || fare.isDisplayed()
                        || UiHelper.waitForDescContains("My Travellers", 2)
                        || UiHelper.waitForDescContains("Terms", 2),
                "Expected price summary / next booking step after fare selection (" + fareType + ")");
        ExtentReportManager.logInfo("Advanced past fare selection (" + fareType + ")");
    }

    private void performReturnSearch(String origin, String destination, String destinationQuery) {
        HomePage home = new HomePage();
        openHomeReady(home);
        home.scrollToFlightSearchForm();
        home.tapReturn();
        ensureOrigin(home, origin);
        home.openGoingTo();
        new AirportPickerPage().searchAndSelect(destinationQuery);
        home.tapSearchFlight();
        pause(5000);

        SearchResultsPage results = new SearchResultsPage();
        if (UiHelper.waitForDescContains("Search Flight", 2) && !results.isResultsScreen()) {
            home.scrollToFlightSearchForm();
            home.tapSearchFlight();
            pause(8000);
        }
    }

    private void ensureOrigin(HomePage home, String origin) {
        try {
            if (!UiHelper.waitForDescContains(origin, 2)) {
                home.openFlyingFrom();
                new AirportPickerPage().searchAndSelect(origin);
            }
        } catch (Exception e) {
            ExtentReportManager.logInfo("Origin selection note: " + e.getMessage());
        }
    }

    private void navigateBackToResults() {
        for (int i = 0; i < 10; i++) {
            SearchResultsPage results = new SearchResultsPage();
            try {
                if (results.isResultsScreen()) {
                    return;
                }
            } catch (Exception ignored) {
                // recovering
            }
            try {
                DriverManager.getDriver().navigate().back();
            } catch (Exception ignored) {
                adbKeyEvent(4);
            }
            pause(900);
            PermissionDialog.dismissAll(1);
        }
        // Last resort: re-run Return search so Fastest path can continue
        ExtentReportManager.logInfo("Results not restored via Back — re-running Return search");
        performReturnSearch(
                TestDataReader.get("flight.origin", "London"),
                TestDataReader.get("flight.return.destination", "Jeddah"),
                TestDataReader.get("flight.return.destination.query", "Jeddah"));
        SearchResultsPage results = new SearchResultsPage();
        results.waitForResults();
        Assert.assertTrue(results.hasResults(), "Could not return to search results for Fastest path");
    }

    private void openHomeReady(HomePage home) {
        for (int i = 0; i < 3; i++) {
            home.openHomeTab();
            pause(1200);
            PermissionDialog.handlePostLoginDialogs();
            if (home.isHomeDisplayed()) {
                return;
            }
            try {
                DriverManager.getDriver().navigate().back();
            } catch (Exception ignored) {
                // continue
            }
            pause(800);
        }
    }

    private void recoverToHomeOrLogin(HomePage home) {
        LoginPage login = new LoginPage();
        for (int i = 0; i < 8; i++) {
            try {
                if (home.isHomeDisplayed() || login.isLoginScreenVisible()) {
                    if (home.isHomeDisplayed()) {
                        home.scrollToFlightSearchForm();
                    }
                    return;
                }
            } catch (Exception e) {
                DevicePrep.restartUiAutomator2();
            }
            if (!UiHelper.tapByDesc("Home\nTab 1 of 4") && !UiHelper.tapByDescContains("Tab 1 of 4")) {
                adbKeyEvent(4);
            }
            pause(800);
        }
        home.bringAppToForeground();
        try {
            home.openHomeTab();
        } catch (Exception e) {
            DevicePrep.restartUiAutomator2();
            adbKeyEvent(4);
        }
        pause(1200);
    }

    private static void adbKeyEvent(int keyCode) {
        try {
            String udid = com.travelhouse.config.ConfigReader.get("device.udid");
            ProcessBuilder pb = (udid == null || udid.isBlank())
                    ? new ProcessBuilder("adb", "shell", "input", "keyevent", String.valueOf(keyCode))
                    : new ProcessBuilder("adb", "-s", udid, "shell", "input", "keyevent",
                    String.valueOf(keyCode));
            pb.redirectErrorStream(true).start().waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // ignore
        }
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
