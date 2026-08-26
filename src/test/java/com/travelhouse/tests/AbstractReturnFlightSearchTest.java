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
import com.travelhouse.pages.PriceDetailsPage;
import com.travelhouse.pages.PriceSummaryPage;
import com.travelhouse.pages.SearchResultsPage;
import com.travelhouse.pages.TravellerInfoPage;
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
        performReturnSearchWithDates(origin, destination, destinationQuery, departure, returnDate);
    }

    protected void performReturnSearchWithDates(String origin,
                                                String destination,
                                                String destinationQuery,
                                                LocalDate departure,
                                                LocalDate returnDate) {
        HomePage home = new HomePage();
        openHomeReady(home);
        home.scrollToFlightSearchForm();
        home.tapReturn();
        ensureOrigin(home, origin);
        home.openGoingTo();
        new AirportPickerPage().searchAndSelect(destinationQuery);

        boolean forceDates = Boolean.parseBoolean(TestDataReader.get("flight.force.date.selection", "true"));
        if (forceDates) {
            ExtentReportManager.logInfo("Selecting travel dates " + departure + " → " + returnDate);
            home.openDeparture();
            new DatePickerPage().selectDepartureAndReturn(departure, returnDate);
            // Confirm far-out year stuck on Home (installments need ~5+ months ahead)
            Assert.assertTrue(
                    UiHelper.waitForDescContains(String.valueOf(departure.getYear()), 3)
                            || UiHelper.isDescPresent(String.valueOf(returnDate.getYear())),
                    "Home must show year " + departure.getYear()
                            + " after date Apply (otherwise Full Payment shows instead of Installment)");
        }

        if (UiHelper.isDescPresent("Select dates")) {
            throw new IllegalStateException("Date picker still open before Search Flight");
        }
        home.scrollToFlightSearchForm();
        home.tapSearchFlight();

        SearchResultsPage results = new SearchResultsPage();
        results.waitForResults();
        Assert.assertTrue(results.hasResults(),
                "Return search results should display (Cheapest / Pay in Installment)");
        ExtentReportManager.logInfo("Return results ready for " + origin + " → " + destination);
    }

    protected void navigateBackToResults() {
        for (int i = 0; i < 12; i++) {
            SearchResultsPage results = new SearchResultsPage();
            try {
                // Require real results chrome — not Price Summary ("Proceed with payment")
                if (results.isResultsScreen() && !results.isBookingSummaryOrTravellerScreen()) {
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

    /**
     * After a plan path we often sit on Price Summary; Back until Cheapest/Fastest
     * (or re-search) so the next Cheapest/Fastest × plan loop can find installments.
     */
    protected void ensureResultsReadyForInstallment() {
        SearchResultsPage results = new SearchResultsPage();
        if (!results.isResultsScreen() || results.isBookingSummaryOrTravellerScreen()
                || !results.waitForPayInInstallmentVisible(5)) {
            navigateBackToResults();
            results = new SearchResultsPage();
        }
        if (!results.waitForPayInInstallmentVisible(10)) {
            ExtentReportManager.logInfo("Installment CTA still missing — re-running Return search");
            LocalDate departure = resolveDepartureDate();
            LocalDate returnDate = departure.plusDays(resolveReturnDaysAfter());
            performReturnSearchWithDates(departure, returnDate);
            results = new SearchResultsPage();
        }
        Assert.assertTrue(results.isResultsScreen(),
                "Must be on search results before selecting an installment plan");
        Assert.assertTrue(results.waitForPayInInstallmentVisible(15),
                "Pay in Installment must be visible before selecting an installment plan");
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

    protected void ensureTravelHouseForeground() {
        HomePage home = new HomePage();
        home.bringAppToForeground();
        pause(1000);
        try {
            String src = UiHelper.getPageSourceSafe();
            if (src != null) {
                String lower = src.toLowerCase();
                if (lower.contains("portable hotspot") || lower.contains("usb tethering")
                        || lower.contains("com.android.settings")
                        || (!lower.contains("travelhouse") && !lower.contains("search flight")
                        && !lower.contains("cheapest") && !lower.contains("proceed with payment")
                        && !lower.contains("installment") && !lower.contains("my travellers"))) {
                    ExtentReportManager.logInfo("Left Travel House UI — recovering with Back + activateApp");
                    adbKeyEvent(4);
                    pause(600);
                    home.bringAppToForeground();
                    pause(1200);
                }
            }
        } catch (Exception e) {
            ExtentReportManager.logInfo("Foreground check note: " + e.getMessage());
            home.bringAppToForeground();
        }
    }

    protected void confirmPastFareToSummary() {
        ensureTravelHouseForeground();
        PriceSummaryPage summary = new PriceSummaryPage();
        long deadline = System.currentTimeMillis() + 55_000L;
        boolean advanced = false;
        while (System.currentTimeMillis() < deadline) {
            PermissionDialog.dismissAll(1);
            if (summary.hasProceedWithPayment() || summary.hasProceedCta()) {
                Assert.assertFalse(summary.getDisplayedTextsSnapshot().isEmpty(),
                        "Itinerary / price content should be visible");
                summary.proceedPastSummary();
                pause(2500);
                advanced = true;
                break;
            }
            boolean stillOnInstallmentSheet = UiHelper.waitForDescContains("Book Now Pay Later", 1)
                    || (UiHelper.waitForDescContains("month", 1)
                    && UiHelper.waitForDescContains("I accept", 1)
                    && !summary.hasProceedWithPayment());
            if (!stillOnInstallmentSheet && (summary.isDisplayed() || summary.hasTotalPrice()
                    || UiHelper.waitForDescContains("My Travellers", 1)
                    || UiHelper.waitForDescContains("Price Summary", 1)
                    || UiHelper.waitForDescContains("Total Price", 1)
                    || UiHelper.waitForDescContains("Proceed with payment", 1))) {
                advanced = true;
                break;
            }
            if (stillOnInstallmentSheet
                    || UiHelper.waitForDescContains("month", 1)
                    || UiHelper.waitForDescContains("Installment", 1)) {
                new InstallmentPlansPage().acceptTermsAndContinue(2);
            }
            pause(1000);
        }
        ensureTravelHouseForeground();
        Assert.assertTrue(
                advanced
                        || summary.isDisplayed() || summary.hasTotalPrice() || summary.hasProceedWithPayment()
                        || UiHelper.waitForDescContains("My Travellers", 3)
                        || UiHelper.waitForDescContains("Price Summary", 2)
                        || UiHelper.waitForDescContains("Proceed with payment", 2)
                        || UiHelper.waitForDescContains("Total Price", 2),
                "Expected price summary / next booking step");
    }

    protected void selectFullPaymentPathFromResults(String filterLabel) {
        SearchResultsPage results = new SearchResultsPage();
        results.scrollResultsToTop();

        // Near-term: Full Payment uses priced Pay CTA (not Pay in Installment)
        if (results.isPayInInstallmentVisible()) {
            ExtentReportManager.logInfo("Pay in Installment visible — still selecting Full Payment Pay CTA");
        }

        // selectCheapest/selectFastest use robust Pay-tap + coordinate fallbacks
        if (filterLabel.toLowerCase().contains("fast")) {
            results.selectFastest();
        } else {
            results.selectCheapest();
        }
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
        completeThroughMyTravellersAndStop();
        ExtentReportManager.logInfo(filterLabel + " + Full Payment ended at My Travellers Continue");
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
            results = new SearchResultsPage();
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
        pause(1500);
        PermissionDialog.dismissAll(1);

        InstallmentPlansPage plansPage = new InstallmentPlansPage();
        Assert.assertTrue(plansPage.waitUntilDisplayed(15),
                "Installment plans must show before selecting: " + planLabel);
        plansPage.selectPlan(planLabel);
        plansPage.acceptTermsAndContinue(3);
        completeThroughMyTravellersAndStop();
        ExtentReportManager.logInfo(filterLabel + " + Installment plan [" + planLabel
                + "] ended at My Travellers Continue (no Price Details)");
    }

    /**
     * From itinerary / proceed CTAs → My Travellers: fill required details, tap Continue once, stop.
     * Does not automate or assert on the Price Details screen.
     */
    protected void completeThroughMyTravellersAndStop() {
        confirmPastFareToSummary();
        PriceSummaryPage summary = new PriceSummaryPage();
        long deadline = System.currentTimeMillis() + 45_000L;
        while (System.currentTimeMillis() < deadline) {
            PermissionDialog.dismissAll(1);
            if (UiHelper.waitForDescContains("Who's Going", 2)
                    || UiHelper.waitForDescContains("My Travellers", 2)
                    || UiHelper.waitForDescContains("Contact Information", 1)) {
                break;
            }
            if (summary.hasProceedCta() || summary.hasProceedWithPayment()) {
                summary.proceedPastSummary();
                pause(1200);
                continue;
            }
            if (summary.isDisplayed() || summary.hasTotalPrice()
                    || UiHelper.waitForDescContains("I accept", 1)
                    || UiHelper.waitForDescContains("Total Price", 1)) {
                summary.acceptTermsAndConditions();
                pause(200);
                summary.continueIfPresent();
                pause(1200);
                continue;
            }
            pause(400);
        }

        TravellerInfoPage traveller = new TravellerInfoPage();
        traveller.waitUntilVisible();
        Assert.assertTrue(traveller.isMyTravellersScreen() || traveller.isDisplayed(),
                "Should reach My Travellers / Traveller Information");
        ExtentReportManager.logInfo("On My Travellers — completing required details");
        traveller.completeOnceSelectNameAndFill();
        traveller.continueOnceAndEnd();

        // Continue may leave Traveller; never drive Price Details automation
        if (new PriceDetailsPage().isDisplayed()) {
            ExtentReportManager.logInfo(
                    "Stopped after My Travellers Continue (Price Details visible but not automated)");
        } else {
            ExtentReportManager.logInfo("Stopped after My Travellers Continue");
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
                // keep trying recovery without killing UiAutomator2 mid-session
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
        // No fixed sleeps — rely on explicit waits for UI state.
    }
}
