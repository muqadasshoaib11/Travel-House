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
import com.travelhouse.pages.PriceDetailsPage;
import com.travelhouse.pages.PriceSummaryPage;
import com.travelhouse.pages.SearchResultsPage;
import com.travelhouse.pages.TravellerInfoPage;
import com.travelhouse.utils.ExtentReportManager;
import com.travelhouse.utils.PermissionDialog;
import com.travelhouse.utils.UiHelper;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Full suite on feature/login-flight-search:
 * Login (or already Home) → verify Home content →
 * Return London→Jeddah: search, validate results, book through to My Travellers →
 * Home again →
 * One Way London→Karachi: same booking process.
 * Uses login.email / login.password from config.properties (same as main).
 */
public class DualRouteBookingSuiteTest extends JourneyBaseTest {

    @Test(priority = 1, description = "Login flow — or continue if already on Home")
    public void step01_loginOrHome() {
        if (!Credentials.isConfigured()) {
            throw new SkipException("Configure login.email / login.password in config.properties");
        }

        HomePage home = new HomePage();
        home.waitForUiReady();
        PermissionDialog.dismissAll(6);

        // Already logged in → Home
        if (home.isHomeDisplayed()) {
            PermissionDialog.handlePostLoginDialogs();
            openHomeReady(home);
            Assert.assertTrue(home.isHomeDisplayed(), "Home should be visible when already logged in");
            ExtentReportManager.logInfo("Already logged in — Home screen visible");
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
        openHomeReady(home);
        Assert.assertTrue(home.isHomeDisplayed(), "Home Page should be displayed after login");
        ExtentReportManager.logInfo("Logged in with configured email — Home visible");
    }

    @Test(priority = 2, dependsOnMethods = "step01_loginOrHome",
            description = "Scroll Home up/down and verify all content is visible")
    public void step02_verifyHomeScreenContent() {
        HomePage home = new HomePage();
        openHomeReady(home);
        home.verifyHomePageContents();
        home.scrollToFlightSearchForm();
        ExtentReportManager.logInfo("Home screen content verified (scroll up/down)");
    }

    @Test(priority = 3, dependsOnMethods = "step02_verifyHomeScreenContent",
            description = "Return: London → Jeddah — search, validate, book to My Travellers")
    public void step03_returnLondonJeddah_bookToMyTravellers() {
        runSearchValidateAndBook(
                true,
                TestDataReader.get("flight.origin", "London"),
                TestDataReader.get("flight.return.destination", "Jeddah"),
                TestDataReader.get("flight.return.destination.query", "Jeddah"));
    }

    @Test(priority = 4, dependsOnMethods = "step03_returnLondonJeddah_bookToMyTravellers",
            description = "Return to Home after Return booking path")
    public void step04_backToHomeAfterReturn() {
        navigateBackToHome();
        HomePage home = new HomePage();
        openHomeReady(home);
        home.scrollToFlightSearchForm();
        Assert.assertTrue(home.isHomeDisplayed(), "Home should be visible before One Way search");
        ExtentReportManager.logInfo("Returned to Home after Jeddah Return path");
    }

    @Test(priority = 5, dependsOnMethods = "step04_backToHomeAfterReturn",
            description = "One Way: London → Karachi — search, validate, book to My Travellers")
    public void step05_oneWayLondonKarachi_bookToMyTravellers() {
        runSearchValidateAndBook(
                false,
                TestDataReader.get("flight.origin", "London"),
                TestDataReader.get("flight.oneway.destination", "Karachi"),
                TestDataReader.get("flight.oneway.destination.query", "Karachi"));
    }

    private void runSearchValidateAndBook(boolean returnTrip, String origin,
                                          String destination, String destinationQuery) {
        String tripLabel = returnTrip ? "Return" : "One Way";
        ExtentReportManager.logInfo(tripLabel + " search: " + origin + " → " + destination);

        HomePage home = new HomePage();
        openHomeReady(home);
        home.scrollToFlightSearchForm();

        if (returnTrip) {
            home.tapReturn();
        } else {
            home.tapOneWay();
        }
        ensureOrigin(home, origin);
        home.openGoingTo();
        new AirportPickerPage().searchAndSelect(destinationQuery);
        home.tapSearchFlight();

        SearchResultsPage results = new SearchResultsPage();
        results.waitForResults();
        Assert.assertTrue(results.hasResults(),
                tripLabel + " results (" + origin + " → " + destination + ") should display");
        results.validateAllListingsHaveRequiredFields(destination);
        ExtentReportManager.logInfo(tripLabel + " results validated");

        // Book first (Cheapest) after scrolling back to top of results
        results.scrollResultsToTop();
        results.selectCheapest();
        pause(4000);
        PermissionDialog.dismissAll(2);

        FareSelectionPage fare = new FareSelectionPage();
        if (fare.isDisplayed()) {
            fare.selectFullPayment();
            fare.continueIfPresent();
            pause(2000);
        }

        PriceSummaryPage summary = new PriceSummaryPage();
        pause(2000);
        if (summary.hasProceedWithPayment()) {
            Assert.assertFalse(summary.getDisplayedTextsSnapshot().isEmpty(),
                    "Price / outbound summary should show content");
            summary.proceedWithPayment();
            pause(3000);
        }

        Assert.assertTrue(summary.isDisplayed() || summary.hasTotalPrice() || summary.hasProceedWithPayment(),
                "Price Summary should be displayed");
        Assert.assertFalse(summary.getDisplayedTextsSnapshot().isEmpty(),
                "Price Summary content should not be empty");
        summary.acceptTermsAndConditions();
        pause(500);
        summary.continueIfPresent();
        pause(3000);
        ExtentReportManager.logInfo("Price Summary completed for " + tripLabel);

        TravellerInfoPage traveller = new TravellerInfoPage();
        traveller.waitUntilVisible();
        Assert.assertTrue(traveller.isMyTravellersScreen(),
                "Should be on My Travellers / Traveller Information after booking steps");
        ExtentReportManager.logInfo("On My Travellers screen");

        // Complete traveller using Sign-In email from config
        traveller.completeTravellerFormUsingSignInEmail();
        try {
            traveller.continueAndVerifyNextScreen();
            PriceDetailsPage details = new PriceDetailsPage();
            pause(2000);
            if (details.isDisplayed() || summary.hasTotalPrice()) {
                ExtentReportManager.logInfo("Advanced past My Travellers toward Price Details");
            }
        } catch (AssertionError e) {
            // Still on My Travellers is acceptable end-of-path for this suite step
            Assert.assertTrue(traveller.isMyTravellersScreen() || traveller.isDisplayed(),
                    "Expected My Travellers (or next screen). Continue issue: " + e.getMessage());
            ExtentReportManager.logInfo("Remained on My Travellers after Continue attempt: " + e.getMessage());
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

    private void navigateBackToHome() {
        for (int i = 0; i < 10; i++) {
            HomePage home = new HomePage();
            if (home.isHomeDisplayed()) {
                home.scrollToFlightSearchForm();
                return;
            }
            try {
                DriverManager.getDriver().navigate().back();
            } catch (Exception ignored) {
                // continue
            }
            pause(1000);
            home.bringAppToForeground();
            home.openHomeTab();
            PermissionDialog.dismissAll(2);
            if (home.isHomeDisplayed()) {
                home.scrollToFlightSearchForm();
                return;
            }
        }
        Assert.fail("Could not navigate back to Home");
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
