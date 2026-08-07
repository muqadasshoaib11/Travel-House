package com.travelhouse.tests;

import com.travelhouse.base.DriverManager;
import com.travelhouse.base.JourneyBaseTest;
import com.travelhouse.config.ConfigReader;
import com.travelhouse.config.Credentials;
import com.travelhouse.config.TestDataReader;
import com.travelhouse.pages.AirportPickerPage;
import com.travelhouse.pages.DatePickerPage;
import com.travelhouse.pages.FareSelectionPage;
import com.travelhouse.pages.HomePage;
import com.travelhouse.pages.InstallmentPlansPage;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Shared Return flight search helpers for Full Payment and Installments suites.
 */
abstract class AbstractReturnFlightSearchTest extends JourneyBaseTest {

    protected void loginOrHome() {
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

    protected void performReturnSearchWithDates(LocalDate departure, LocalDate returnDate) {
        String origin = TestDataReader.get("flight.origin", "London");
        String destination = TestDataReader.get("flight.return.destination", "Jeddah");
        String destinationQuery = TestDataReader.get("flight.return.destination.query", "Jeddah");

        HomePage home = new HomePage();
        openHomeReady(home);
        home.scrollToFlightSearchForm();
        home.tapReturn();
        ensureOrigin(home, origin);
        home.openGoingTo();
        new AirportPickerPage().searchAndSelect(destinationQuery);

        ExtentReportManager.logInfo("Selecting travel dates " + departure + " → " + returnDate);
        home.openDeparture();
        pause(1500);
        new DatePickerPage().selectDepartureAndReturn(departure, returnDate);

        home.tapSearchFlight();
        pause(6000);

        SearchResultsPage results = new SearchResultsPage();
        if (UiHelper.waitForDescContains("Search Flight", 2) && !results.isResultsScreen()) {
            home.scrollToFlightSearchForm();
            home.tapSearchFlight();
            pause(8000);
        }

        results.waitForResults();
        Assert.assertTrue(results.hasResults(), "Return search results should display");
        results.validateAllListingsHaveRequiredFields(destination);
        ExtentReportManager.logInfo("Return results validated for " + origin + " → " + destination);
    }

    protected void navigateBackToResults() {
        for (int i = 0; i < 12; i++) {
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
        ExtentReportManager.logInfo("Results not restored via Back — re-running Return search");
        LocalDate departure = resolveDepartureDate();
        LocalDate returnDate = departure.plusDays(resolveReturnDaysAfter());
        performReturnSearchWithDates(departure, returnDate);
        Assert.assertTrue(new SearchResultsPage().hasResults(), "Could not return to search results");
    }

    protected LocalDate resolveDepartureDate() {
        String daysAhead = TestDataReader.get("flight.departure.days.ahead", "").trim();
        if (!daysAhead.isBlank()) {
            return LocalDate.now().plusDays(Integer.parseInt(daysAhead));
        }
        int monthsAhead = Integer.parseInt(TestDataReader.get("flight.departure.months.ahead", "3").trim());
        return LocalDate.now().plusMonths(monthsAhead);
    }

    protected int resolveReturnDaysAfter() {
        return Integer.parseInt(TestDataReader.get("flight.return.days.after.departure", "7").trim());
    }

    protected void confirmPastFareToSummary() {
        PriceSummaryPage summary = new PriceSummaryPage();
        pause(2000);
        if (summary.hasProceedWithPayment()) {
            Assert.assertFalse(summary.getDisplayedTextsSnapshot().isEmpty(),
                    "Itinerary / price content should be visible");
            summary.proceedWithPayment();
            pause(2500);
        }
        Assert.assertTrue(
                summary.isDisplayed() || summary.hasTotalPrice() || summary.hasProceedWithPayment()
                        || UiHelper.waitForDescContains("My Travellers", 2)
                        || UiHelper.waitForDescContains("Terms", 2)
                        || UiHelper.waitForDescContains("Installment", 2),
                "Expected price summary / next booking step");
    }

    protected void selectFullPaymentPathFromResults(String filterLabel) {
        SearchResultsPage results = new SearchResultsPage();
        results.scrollResultsToTop();
        if (filterLabel.toLowerCase().contains("fast")) {
            results.applyFastestFilter();
        } else {
            results.applyCheapestFilter();
        }
        Assert.assertTrue(results.hasResults(), "Listings should display after " + filterLabel);

        // Near-term dates: Full Payment path — use priced Pay CTA (not Pay in Installment)
        if (results.isPayInInstallmentVisible()) {
            ExtentReportManager.logInfo("Pay in Installment visible on near-term results — still selecting Full Payment Pay CTA");
        }

        results.selectPayAtIndex(0);
        pause(3000);
        PermissionDialog.dismissAll(2);

        FareSelectionPage fare = new FareSelectionPage();
        if (fare.waitUntilDisplayed(10)) {
            Assert.assertTrue(fare.isFullPaymentVisible(), "Full Payment option must be visible");
            fare.selectFullPayment();
            fare.continueIfPresent();
            pause(1500);
            ExtentReportManager.logInfo("Selected Full Payment on fare screen");
        } else {
            ExtentReportManager.logInfo("No separate fare sheet — continuing Full Payment itinerary");
        }
        confirmPastFareToSummary();
        ExtentReportManager.logInfo(filterLabel + " + Full Payment completed");
    }

    /**
     * Opens first installment CTA, discovers all plans, returns to results.
     */
    protected List<String> discoverInstallmentPlansFromResults() {
        SearchResultsPage results = new SearchResultsPage();
        Assert.assertTrue(results.isPayInInstallmentVisible(),
                "Pay in Installment must appear for far-out departure dates (~3 months)");

        results.scrollResultsToTop();
        results.applyCheapestFilter();
        results.selectPayInInstallmentAtIndex(0);
        pause(3000);
        PermissionDialog.dismissAll(2);

        InstallmentPlansPage plansPage = new InstallmentPlansPage();
        Assert.assertTrue(plansPage.waitUntilDisplayed(20),
                "Installment plans screen must appear after Pay in Installment");
        List<String> plans = plansPage.discoverAvailablePlans();
        Assert.assertFalse(plans.isEmpty(),
                "Expected at least one installment plan after Pay in Installment");

        // Return to results for Cheapest/Fastest × plan loops
        navigateBackToResults();
        return new ArrayList<>(plans);
    }

    protected void selectInstallmentPlanPath(String filterLabel, String planLabel) {
        SearchResultsPage results = new SearchResultsPage();
        if (!results.isResultsScreen()) {
            navigateBackToResults();
        }
        results.scrollResultsToTop();
        if (filterLabel.toLowerCase().contains("fast")) {
            results.applyFastestFilter();
        } else {
            results.applyCheapestFilter();
        }
        Assert.assertTrue(results.isPayInInstallmentVisible(),
                "Pay in Installment must remain visible for Installments scenario");

        results.selectPayInInstallmentAtIndex(0);
        pause(3000);
        PermissionDialog.dismissAll(2);

        InstallmentPlansPage plansPage = new InstallmentPlansPage();
        Assert.assertTrue(plansPage.waitUntilDisplayed(20),
                "Installment plans must show before selecting: " + planLabel);
        plansPage.selectPlan(planLabel);
        confirmPastFareToSummary();
        ExtentReportManager.logInfo(filterLabel + " + Installment plan [" + planLabel + "] completed");
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

    protected static void adbKeyEvent(int keyCode) {
        try {
            String udid = ConfigReader.get("device.udid");
            ProcessBuilder pb = (udid == null || udid.isBlank())
                    ? new ProcessBuilder("adb", "shell", "input", "keyevent", String.valueOf(keyCode))
                    : new ProcessBuilder("adb", "-s", udid, "shell", "input", "keyevent",
                    String.valueOf(keyCode));
            pb.redirectErrorStream(true).start().waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // ignore
        }
    }

    protected static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
