package com.travelhouse.tests;

import com.travelhouse.pages.InstallmentPlansPage;
import com.travelhouse.pages.PriceSummaryPage;
import com.travelhouse.pages.SearchResultsPage;
import com.travelhouse.pages.TravellerInfoPage;
import com.travelhouse.utils.ExtentReportManager;
import com.travelhouse.utils.PermissionDialog;
import com.travelhouse.utils.UiHelper;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * London → Islamabad Return installments (1–6 month plans):
 * For an N-month installment plan, departure is set N months ahead (return ~29 days later).
 * Runs Cheapest for each plan 1→6 through Traveller Information (select name once, fill once),
 * then one Fastest + Installment flow (6-month plan / 6 months ahead).
 *
 * Local suite only — do not push unless explicitly requested.
 */
public class ReturnJuly2027SixInstallmentsSuiteTest extends AbstractReturnFlightSearchTest {

    private static final String ORIGIN = "London";
    private static final String DESTINATION = "Islamabad";
    private static final String DESTINATION_QUERY = "Islamabad";
    private static final int MIN_PLAN_MONTHS = 1;
    private static final int MAX_PLAN_MONTHS = 6;
    private static final int RETURN_DAYS_AFTER = 29;
    private static final Pattern MONTHS_IN_PLAN = Pattern.compile("(\\d+)\\s*months?");

    /** Ordered plan labels for months 1..6 (e.g. "1 month"). */
    private List<String> installmentPlans = new ArrayList<>();

    @Test(priority = 1, timeOut = 600_000, description = "Login / reach Home")
    public void step01_loginOrHome() {
        loginOrHome();
    }

    @Test(priority = 2, timeOut = 600_000, dependsOnMethods = "step01_loginOrHome",
            description = "Discover installment plans 1–6 months (search 6 months ahead)")
    public void step02_search_discoverSixPlans() {
        // Far-out search so the full plan list (incl. 6 month) is available
        searchReturnForMonthsAhead(MAX_PLAN_MONTHS);
        Assert.assertTrue(new SearchResultsPage().isPayInInstallmentVisible(),
                "Blue Pay in Installment must appear for " + MAX_PLAN_MONTHS + " months ahead");

        SearchResultsPage results = new SearchResultsPage();
        results.scrollResultsToTop();
        results.applyCheapestFilter();
        results.selectPayInInstallmentAtIndex(0);
        pause(3000);
        PermissionDialog.dismissAll(2);

        InstallmentPlansPage plansPage = new InstallmentPlansPage();
        Assert.assertTrue(plansPage.waitUntilDisplayed(25),
                "Installment plans sheet must open after Pay in Installment");
        List<String> discovered = plansPage.discoverAvailablePlans();
        Assert.assertFalse(discovered.isEmpty(), "Expected installment plans");

        installmentPlans = filterPlansByMonths(discovered, MIN_PLAN_MONTHS, MAX_PLAN_MONTHS);
        Assert.assertFalse(installmentPlans.isEmpty(),
                "Expected plans for " + MIN_PLAN_MONTHS + "–" + MAX_PLAN_MONTHS + " months, got: " + discovered);
        ExtentReportManager.logInfo("Installment plans to run (date = N months ahead): " + installmentPlans);
        navigateBackToHomeForNewSearch();
    }

    @Test(priority = 3, timeOut = 4_200_000, dependsOnMethods = "step02_search_discoverSixPlans",
            description = "Cheapest: each 1–6 month plan with matching departure months ahead")
    public void step03_cheapest_eachInstallmentToTraveller() {
        Assert.assertFalse(installmentPlans.isEmpty(), "Plans must be discovered first");

        for (int i = 0; i < installmentPlans.size(); i++) {
            String plan = installmentPlans.get(i);
            int months = monthsFromPlan(plan);
            Assert.assertTrue(months >= MIN_PLAN_MONTHS && months <= MAX_PLAN_MONTHS,
                    "Unexpected plan months for: " + plan);

            LocalDate departure = LocalDate.now().plusMonths(months);
            LocalDate returnDate = departure.plusDays(RETURN_DAYS_AFTER);
            ExtentReportManager.logInfo("=== Cheapest " + (i + 1) + "/" + installmentPlans.size()
                    + ": plan [" + plan + "] dates " + departure + " → " + returnDate
                    + " (" + months + " months ahead) ===");

            searchReturnWithDates(departure, returnDate);
            bookInstallmentPlanThroughTraveller("Cheapest", plan);

            if (i < installmentPlans.size() - 1) {
                navigateBackToHomeForNewSearch();
                // Extra settle time so the next date-matched search is not flaky
                pause(2000);
            }
        }
        ExtentReportManager.logInfo("Completed Cheapest installment plans 1–6 months through Traveller");
    }

    @Test(priority = 4, timeOut = 900_000, dependsOnMethods = "step03_cheapest_eachInstallmentToTraveller",
            description = "One Fastest + Installment (6 months ahead / 6-month plan)")
    public void step04_fastest_oneInstallmentFlow() {
        Assert.assertFalse(installmentPlans.isEmpty(), "Plans must be discovered first");
        String plan = pickPlanForMonths(installmentPlans, MAX_PLAN_MONTHS);
        if (plan == null) {
            plan = installmentPlans.get(installmentPlans.size() - 1);
        }
        int months = monthsFromPlan(plan);
        LocalDate departure = LocalDate.now().plusMonths(months);
        LocalDate returnDate = departure.plusDays(RETURN_DAYS_AFTER);

        navigateBackToHomeForNewSearch();
        searchReturnWithDates(departure, returnDate);
        ExtentReportManager.logInfo("=== Fastest one-flow: plan [" + plan + "] dates "
                + departure + " → " + returnDate + " ===");
        bookInstallmentPlanThroughTraveller("Fastest", plan);
        ExtentReportManager.logInfo("Fastest + Installment single flow completed");
    }

    private void searchReturnForMonthsAhead(int monthsAhead) {
        LocalDate departure = LocalDate.now().plusMonths(monthsAhead);
        LocalDate returnDate = departure.plusDays(RETURN_DAYS_AFTER);
        searchReturnWithDates(departure, returnDate);
    }

    private void searchReturnWithDates(LocalDate departure, LocalDate returnDate) {
        performReturnSearchWithDates(ORIGIN, DESTINATION, DESTINATION_QUERY, departure, returnDate);
        Assert.assertTrue(new SearchResultsPage().isPayInInstallmentVisible(),
                "Pay in Installment (blue) must be available for " + departure + " → " + returnDate);
    }

    private void bookInstallmentPlanThroughTraveller(String filterLabel, String planLabel) {
        SearchResultsPage results = new SearchResultsPage();
        results.scrollResultsToTop();
        if (filterLabel.toLowerCase(Locale.ENGLISH).contains("fast")) {
            results.applyFastestFilter();
        } else {
            results.applyCheapestFilter();
        }
        Assert.assertTrue(results.isPayInInstallmentVisible(),
                "Pay in Installment must remain visible before booking");

        results.selectPayInInstallmentAtIndex(0);
        pause(3000);
        PermissionDialog.dismissAll(2);
        ensureTravelHouseForeground();

        InstallmentPlansPage plansPage = new InstallmentPlansPage();
        Assert.assertTrue(plansPage.waitUntilDisplayed(25),
                "Installment plans must show before selecting: " + planLabel);
        plansPage.selectPlan(planLabel);
        Assert.assertTrue(plansPage.acceptTermsAndContinue(5),
                "Could not Continue after accepting Terms for plan: " + planLabel);
        ensureTravelHouseForeground();
        confirmPastFareToSummary();

        PriceSummaryPage summary = new PriceSummaryPage();
        // Installment path may show "Proceed With Query" instead of "Proceed with payment"
        for (int i = 0; i < 4; i++) {
            if (UiHelper.waitForDescContains("Who's Going", 2)
                    || UiHelper.waitForDescContains("My Travellers", 2)
                    || UiHelper.waitForDescContains("Contact Information", 1)) {
                break;
            }
            if (summary.hasProceedCta()) {
                summary.proceedPastSummary();
                pause(2500);
                continue;
            }
            if (summary.isDisplayed() || summary.hasTotalPrice()
                    || UiHelper.waitForDescContains("I accept", 1)
                    || UiHelper.waitForDescContains("Total Price", 1)) {
                summary.acceptTermsAndConditions();
                pause(400);
                summary.continueIfPresent();
                pause(2500);
                continue;
            }
            pause(1000);
        }

        TravellerInfoPage traveller = new TravellerInfoPage();
        traveller.waitUntilVisible();
        Assert.assertTrue(traveller.isMyTravellersScreen() || traveller.isDisplayed(),
                "Should reach Traveller Information");
        traveller.completeOnceSelectNameAndFill();
        traveller.continueOnceAndEnd();
        ExtentReportManager.logInfo(filterLabel + " + plan [" + planLabel
                + "] reached end of Traveller flow");
    }

    private void navigateBackToHomeForNewSearch() {
        for (int i = 0; i < 14; i++) {
            if (UiHelper.waitForDescContains("Search Flight", 2)
                    || UiHelper.waitForDescContains("Flying From", 1)) {
                ExtentReportManager.logInfo("Back on Home / search form");
                return;
            }
            try {
                com.travelhouse.base.DriverManager.getDriver().navigate().back();
            } catch (Exception ignored) {
                adbKeyEvent(4);
            }
            pause(800);
            PermissionDialog.dismissAll(1);
        }
        com.travelhouse.pages.HomePage home = new com.travelhouse.pages.HomePage();
        home.bringAppToForeground();
        home.openHomeTab();
        pause(1200);
        Assert.assertTrue(home.isHomeDisplayed() || UiHelper.waitForDescContains("Search Flight", 5),
                "Could not return to Home for the next search");
    }

    private static List<String> filterPlansByMonths(List<String> plans, int minMonths, int maxMonths) {
        List<String> filtered = new ArrayList<>();
        for (int months = minMonths; months <= maxMonths; months++) {
            String match = pickPlanForMonths(plans, months);
            if (match != null) {
                filtered.add(match);
            } else {
                ExtentReportManager.logInfo("No plan found for " + months + " month(s) in: " + plans);
            }
        }
        return filtered;
    }

    private static String pickPlanForMonths(List<String> plans, int months) {
        for (String plan : plans) {
            if (monthsFromPlan(plan) == months) {
                return plan;
            }
        }
        return null;
    }

    private static int monthsFromPlan(String planLabel) {
        if (planLabel == null) {
            return -1;
        }
        Matcher m = MONTHS_IN_PLAN.matcher(planLabel.toLowerCase(Locale.ENGLISH));
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return -1;
    }
}
