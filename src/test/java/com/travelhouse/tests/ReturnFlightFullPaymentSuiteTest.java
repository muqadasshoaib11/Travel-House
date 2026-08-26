package com.travelhouse.tests;

import com.travelhouse.config.ConfigReader;
import com.travelhouse.pages.AirportPickerPage;
import com.travelhouse.pages.DatePickerPage;
import com.travelhouse.pages.FullPaymentPage;
import com.travelhouse.pages.HomePage;
import com.travelhouse.pages.TravellerInfoPage;
import com.travelhouse.utils.ExtentReportManager;
import com.travelhouse.utils.PermissionDialog;
import com.travelhouse.utils.UiHelper;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Full Payment suite aligned with Travel-House-Complete.zip Playwright script:
 * <ol>
 *   <li>Return London→Jeddah, 23–30 Sep 2026, Cheapest</li>
 *   <li>Return London→Islamabad, 23–30 Sep 2026, Fastest</li>
 *   <li>One Way London→Dammam, 25 Nov 2026, Fastest</li>
 * </ol>
 * Each path: Pay £ → Proceed → Terms → My Travellers → Summary Continue → stop.
 *
 * Branch: feature/return-search-full-payment
 */
public class ReturnFlightFullPaymentSuiteTest extends AbstractReturnFlightSearchTest {

    @Test(priority = 1, timeOut = 300_000, description = "Login / reach Home")
    public void step01_loginOrHome() {
        Assert.assertFalse(ConfigReader.get("fare.selection.mode", "full").toLowerCase().contains("install"),
                "Full Payment suite requires fare.selection.mode=full");
        loginOrHome();
    }

    @Test(priority = 2, timeOut = 1_200_000, dependsOnMethods = "step01_loginOrHome",
            description = "Return London→Jeddah Cheapest full payment")
    public void step02_returnJeddah_cheapest() {
        runFullPaymentScenario("Return", "Jeddah", "JED", "Cheapest",
                "Wed, 23 September 2026", "Wed, 30 September 2026");
    }

    @Test(priority = 3, timeOut = 1_200_000, dependsOnMethods = "step02_returnJeddah_cheapest",
            description = "Return London→Islamabad Fastest full payment")
    public void step03_returnIslamabad_fastest() {
        navigateBackToHome();
        runFullPaymentScenario("Return", "Islamabad", "ISB", "Fastest",
                "Wed, 23 September 2026", "Wed, 30 September 2026");
    }

    @Test(priority = 4, timeOut = 1_200_000, dependsOnMethods = "step03_returnIslamabad_fastest",
            description = "One Way London→Dammam Fastest full payment")
    public void step04_oneWayDammam_fastest() {
        navigateBackToHome();
        runFullPaymentScenario("One Way", "Dammam", "DMM", "Fastest",
                "Wed, 25 November 2026", null);
    }

    private void runFullPaymentScenario(String tripType,
                                        String city,
                                        String iata,
                                        String fareTab,
                                        String departureLabel,
                                        String returnLabel) {
        ExtentReportManager.logInfo(tripType + " London → " + city + " (" + fareTab + ")");
        HomePage home = new HomePage();
        openHomeReady(home);
        home.scrollToFlightSearchForm();

        if ("One Way".equalsIgnoreCase(tripType)) {
            home.tapOneWay();
        } else {
            home.tapReturn();
        }

        home.openGoingTo();
        new AirportPickerPage().searchAndSelectByIata(city, iata);

        home.openDeparture();
        new DatePickerPage().selectByDayLabels(departureLabel, returnLabel);

        home.searchForFullPayment();
        PermissionDialog.dismissAll(1);

        FullPaymentPage payment = new FullPaymentPage();
        payment.selectFareAndPay(fareTab);
        payment.acceptTermsAndContinue();

        TravellerInfoPage traveller = new TravellerInfoPage();
        traveller.waitUntilVisible();
        traveller.completeOnceSelectNameAndFill();
        traveller.continueThroughSummaryOnceAndStop();
        ExtentReportManager.logInfo(tripType + " " + city + " / " + fareTab
                + " stopped after Summary Continue");
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
