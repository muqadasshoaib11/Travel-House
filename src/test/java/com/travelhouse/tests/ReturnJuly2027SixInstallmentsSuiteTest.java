package com.travelhouse.tests;

import com.travelhouse.pages.InstallmentPlansPage;
import com.travelhouse.pages.PriceDetailsPage;
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
 * London → Islamabad Return — Pay in Installment plans 1–6 months.
 * <p>
 * Search dates stay in a 4–6 month window so the blue Pay in Installment CTA is available.
 * Each available plan (1→6) is run on Cheapest through My Travellers (fill + Continue), then
 * one Fastest flow for the 6-month plan. Flow ends after Traveller Continue — Price Details
 * is never automated.
 */
public class ReturnJuly2027SixInstallmentsSuiteTest extends AbstractReturnFlightSearchTest {

    private static final String ORIGIN = "London";
    private static final String DESTINATION = "Islamabad";
    private static final String DESTINATION_QUERY = "Islamabad";
    private static final int MIN_PLAN_MONTHS = 1;
    private static final int MAX_PLAN_MONTHS = 6;
    /** Departure must be far enough for Pay in Installment (4–6 months). */
    private static final int MIN_SEARCH_MONTHS = 4;
    private static final int MAX_SEARCH_MONTHS = 6;
    private static final int RETURN_DAYS_AFTER = 29;
    private static final Pattern MONTHS_IN_PLAN = Pattern.compile("(\\d+)\\s*months?");

    /** Ordered plan labels for months 1..6 (e.g. "1 month"). */
    private List<String> installmentPlans = new ArrayList<>();

    @Test(priority = 1, timeOut = 600_000, description = "Login / reach Home")
    public void step01_loginOrHome() {
        loginOrHome();
    }

    @Test(priority = 2, timeOut = 600_000, dependsOnMethods = "step01_loginOrHome",
            description = "Discover installment plans 1–6 (search ~6 months ahead)")
    public void step02_search_discoverSixPlans() {
        searchReturnForMonthsAhead(MAX_SEARCH_MONTHS);
        Assert.assertTrue(new SearchResultsPage().waitForPayInInstallmentVisible(20),
                "Blue Pay in Installment must appear for " + MAX_SEARCH_MONTHS + " months ahead");

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
        ExtentReportManager.logInfo("Installment plans to run (search window "
                + MIN_SEARCH_MONTHS + "–" + MAX_SEARCH_MONTHS + " months): " + installmentPlans);
        navigateBackToHomeForNewSearch();
    }

    @Test(priority = 3, timeOut = 4_200_000, dependsOnMethods = "step02_search_discoverSixPlans",
            description = "Cheapest: each 1–6 month plan through My Travellers (stop after Continue)")
    public void step03_cheapest_eachInstallmentToTraveller() {
        Assert.assertFalse(installmentPlans.isEmpty(), "Plans must be discovered first");

        for (int i = 0; i < installmentPlans.size(); i++) {
            String plan = installmentPlans.get(i);
            int planMonths = monthsFromPlan(plan);
            Assert.assertTrue(planMonths >= MIN_PLAN_MONTHS && planMonths <= MAX_PLAN_MONTHS,
                    "Unexpected plan months for: " + plan);

            int searchMonths = searchMonthsForPlan(planMonths);
            LocalDate departure = LocalDate.now().plusMonths(searchMonths);
            LocalDate returnDate = departure.plusDays(RETURN_DAYS_AFTER);
            ExtentReportManager.logInfo("=== Cheapest " + (i + 1) + "/" + installmentPlans.size()
                    + ": plan [" + plan + "] dates " + departure + " → " + returnDate
                    + " (search " + searchMonths + " months ahead) ===");

            searchReturnWithDates(departure, returnDate);
            bookInstallmentThroughMyTravellersOnly("Cheapest", plan);

            if (i < installmentPlans.size() - 1) {
                navigateBackToHomeForNewSearch();
                pause(2000);
            }
        }
        ExtentReportManager.logInfo(
                "Completed Cheapest installment plans 1–6 through My Travellers (no Price Details)");
    }

    @Test(priority = 4, timeOut = 900_000, dependsOnMethods = "step03_cheapest_eachInstallmentToTraveller",
            description = "One Fastest + Installment (6-month plan) through My Travellers only")
    public void step04_fastest_oneInstallmentFlow() {
        Assert.assertFalse(installmentPlans.isEmpty(), "Plans must be discovered first");
        String plan = pickPlanForMonths(installmentPlans, MAX_PLAN_MONTHS);
        if (plan == null) {
            plan = installmentPlans.get(installmentPlans.size() - 1);
        }
        int searchMonths = searchMonthsForPlan(monthsFromPlan(plan));
        LocalDate departure = LocalDate.now().plusMonths(searchMonths);
        LocalDate returnDate = departure.plusDays(RETURN_DAYS_AFTER);

        navigateBackToHomeForNewSearch();
        searchReturnWithDates(departure, returnDate);
        ExtentReportManager.logInfo("=== Fastest one-flow: plan [" + plan + "] dates "
                + departure + " → " + returnDate + " ===");
        bookInstallmentThroughMyTravellersOnly("Fastest", plan);
        ExtentReportManager.logInfo("Fastest + Installment ended after My Travellers Continue");
    }

    /**
     * Keep departure in the 4–6 month window so Pay in Installment stays available.
     * Plan length 1–3 months still uses a 4-month search; 4–6 use matching months.
     */
    private static int searchMonthsForPlan(int planMonths) {
        if (planMonths < MIN_SEARCH_MONTHS) {
            return MIN_SEARCH_MONTHS;
        }
        return Math.min(planMonths, MAX_SEARCH_MONTHS);
    }

    private void searchReturnForMonthsAhead(int monthsAhead) {
        LocalDate departure = LocalDate.now().plusMonths(monthsAhead);
        LocalDate returnDate = departure.plusDays(RETURN_DAYS_AFTER);
        searchReturnWithDates(departure, returnDate);
    }

    private void searchReturnWithDates(LocalDate departure, LocalDate returnDate) {
        performReturnSearchWithDates(ORIGIN, DESTINATION, DESTINATION_QUERY, departure, returnDate);
        Assert.assertTrue(new SearchResultsPage().waitForPayInInstallmentVisible(20),
                "Pay in Installment (blue) must be available for " + departure + " → " + returnDate);
    }

    /**
     * Pay in Installment → select plan → Price Summary (to reach travellers) →
     * My Travellers fill + Continue → stop. Never opens or fills Price Details.
     */
    private void bookInstallmentThroughMyTravellersOnly(String filterLabel, String planLabel) {
        ensureResultsReadyForInstallment();
        SearchResultsPage results = new SearchResultsPage();
        results.scrollResultsToTop();
        if (filterLabel.toLowerCase(Locale.ENGLISH).contains("fast")) {
            results.applyFastestFilter();
        } else {
            results.applyCheapestFilter();
        }
        Assert.assertTrue(results.waitForPayInInstallmentVisible(15),
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

        advanceFromFareOrSummaryToMyTravellers();

        TravellerInfoPage traveller = new TravellerInfoPage();
        traveller.waitUntilVisible();
        Assert.assertTrue(traveller.isMyTravellersScreen() || traveller.isDisplayed(),
                "Should reach My Travellers / Traveller Information");
        Assert.assertFalse(new PriceDetailsPage().isDisplayed(),
                "Must not be on Price Details before completing My Travellers");

        traveller.completeOnceSelectNameAndFill();
        traveller.continueOnceAndEnd();

        // End of automation: Continue was tapped; do not interact with Price Details
        if (new PriceDetailsPage().isDisplayed()) {
            ExtentReportManager.logInfo(
                    "App may show Price Details after Continue — leaving without automating it");
        }
        ExtentReportManager.logInfo(filterLabel + " + plan [" + planLabel
                + "] completed My Travellers (Continue) — flow ended (no Price Details)");
    }

    /**
     * Reach My Travellers via itinerary / Price Summary CTAs only.
     * Does not open Price Details.
     */
    private void advanceFromFareOrSummaryToMyTravellers() {
        PriceSummaryPage summary = new PriceSummaryPage();
        long deadline = System.currentTimeMillis() + 55_000L;
        while (System.currentTimeMillis() < deadline) {
            PermissionDialog.dismissAll(1);
            if (UiHelper.waitForDescContains("Who's Going", 1)
                    || UiHelper.waitForDescContains("My Travellers", 1)
                    || UiHelper.waitForDescContains("Contact Information", 1)) {
                return;
            }
            // Never tap into Price Details
            if (new PriceDetailsPage().isDisplayed()
                    && !summary.hasProceedCta()
                    && !UiHelper.waitForDescContains("Who's Going", 1)) {
                ExtentReportManager.logInfo("Saw Price Details unexpectedly — backing up");
                try {
                    com.travelhouse.base.DriverManager.getDriver().navigate().back();
                } catch (Exception ignored) {
                    adbKeyEvent(4);
                }
                pause(1000);
                continue;
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
            if (UiHelper.waitForDescContains("Book Now Pay Later", 1)
                    || (UiHelper.waitForDescContains("month", 1)
                    && UiHelper.waitForDescContains("I accept", 1))) {
                new InstallmentPlansPage().acceptTermsAndContinue(2);
            }
            pause(1000);
        }
        Assert.assertTrue(
                UiHelper.waitForDescContains("Who's Going", 3)
                        || UiHelper.waitForDescContains("My Travellers", 3)
                        || UiHelper.waitForDescContains("Contact Information", 2),
                "Expected My Travellers after installment / summary steps (not Price Details)");
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
