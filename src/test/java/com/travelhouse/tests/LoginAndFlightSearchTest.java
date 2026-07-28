package com.travelhouse.tests;

import com.travelhouse.base.DriverManager;
import com.travelhouse.base.JourneyBaseTest;
import com.travelhouse.config.Credentials;
import com.travelhouse.config.TestDataReader;
import com.travelhouse.pages.AirportPickerPage;
import com.travelhouse.pages.HomePage;
import com.travelhouse.pages.LoginPage;
import com.travelhouse.pages.OnboardingPage;
import com.travelhouse.pages.SearchResultsPage;
import com.travelhouse.utils.ExtentReportManager;
import com.travelhouse.utils.PermissionDialog;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

/**
 * Login + Home content verification + One-Way / Return flight search only.
 * Routes: London → Jeddah (Return), London → Karachi (One Way).
 */
public class LoginAndFlightSearchTest extends JourneyBaseTest {

    @Test(priority = 1, description = "Login — allow notifications, cancel biometric, reach Home")
    public void step01_login() {
        if (!Credentials.isConfigured()) {
            throw new SkipException("Configure login credentials in config.properties");
        }

        HomePage home = new HomePage();
        home.waitForUiReady();
        PermissionDialog.dismissAll(6);

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
        Assert.assertTrue(home.isHomeDisplayed(), "Home Page with flight search should be displayed after login");
        ExtentReportManager.logInfo("Logged in successfully");
    }

    @Test(priority = 2, dependsOnMethods = "step01_login",
            description = "Verify Home Page contents (destinations, upcoming flights, search form)")
    public void step02_verifyHomePageContents() {
        HomePage home = new HomePage();
        openHomeReady(home);
        home.verifyHomePageContents();
        ExtentReportManager.logInfo("Home Page contents verified");
    }

    @Test(priority = 3, dependsOnMethods = "step02_verifyHomePageContents",
            description = "London → Jeddah Return flight search + validate results")
    public void step03_returnSearch_londonToJeddah() {
        String origin = TestDataReader.get("flight.origin", "London");
        String destination = TestDataReader.get("flight.return.destination", "Jeddah");
        String destinationQuery = TestDataReader.get("flight.return.destination.query", "Jeddah");

        HomePage home = new HomePage();
        openHomeReady(home);
        home.tapReturn();
        ensureOrigin(home, origin);
        home.openGoingTo();
        new AirportPickerPage().searchAndSelect(destinationQuery);
        home.tapSearchFlight();

        SearchResultsPage results = new SearchResultsPage();
        results.waitForResults();
        Assert.assertTrue(results.hasResults(), "Return search results (London → Jeddah) should be displayed");
        results.validateAllListingsHaveRequiredFields(destination);
        ExtentReportManager.logInfo("Return London→Jeddah results validated count="
                + results.getResultCountEstimate());
    }

    @Test(priority = 4, dependsOnMethods = "step03_returnSearch_londonToJeddah",
            description = "London → Karachi One Way flight search + validate results")
    public void step04_oneWaySearch_londonToKarachi() {
        String origin = TestDataReader.get("flight.origin", "London");
        String destination = TestDataReader.get("flight.oneway.destination", "Karachi");
        String destinationQuery = TestDataReader.get("flight.oneway.destination.query", "Karachi");

        navigateBackToHome();
        HomePage home = new HomePage();
        openHomeReady(home);
        home.tapOneWay();
        ensureOrigin(home, origin);
        home.openGoingTo();
        new AirportPickerPage().searchAndSelect(destinationQuery);
        home.tapSearchFlight();

        SearchResultsPage results = new SearchResultsPage();
        results.waitForResults();
        Assert.assertTrue(results.hasResults(), "One-way search results (London → Karachi) should be displayed");
        results.validateAllListingsHaveRequiredFields(destination);
        ExtentReportManager.logInfo("One-way London→Karachi results validated count="
                + results.getResultCountEstimate());
    }

    private void ensureOrigin(HomePage home, String origin) {
        // Origin often defaults to London; re-select if Flying From does not already show it
        try {
            if (!com.travelhouse.utils.UiHelper.waitForDescContains(origin, 2)) {
                home.openFlyingFrom();
                new AirportPickerPage().searchAndSelect(origin);
            }
        } catch (Exception e) {
            ExtentReportManager.logInfo("Origin selection skipped/fallback: " + e.getMessage());
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
            DriverManager.getDriver().navigate().back();
            pause(800);
        }
    }

    private void navigateBackToHome() {
        for (int i = 0; i < 8; i++) {
            HomePage home = new HomePage();
            if (home.isHomeDisplayed()) {
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
            if (home.isHomeDisplayed()) {
                return;
            }
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
