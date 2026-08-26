package com.travelhouse.pages;

import com.travelhouse.base.DriverManager;
import com.travelhouse.utils.GestureUtil;
import com.travelhouse.utils.UiHelper;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;

import java.util.List;

/**
 * Home / flight search entry points for Travel House.
 * Flutter UI: prefer content-desc / accessibilityId.
 */
public class HomePage {

    private final AndroidDriver driver;

    private final By homeTab = AppiumBy.accessibilityId("Home\nTab 1 of 4");
    private final By oneWay = AppiumBy.accessibilityId("One Way");
    private final By returnTrip = AppiumBy.accessibilityId("Return");
    private final By flyingFrom = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Flying From\")");
    private final By goingTo = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Going to\")");
    private final By departure = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Departure\")");
    private final By passengerClass = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Select Passenger\")");
    private final By searchFlight = AppiumBy.accessibilityId("Search Flight");
    private final By greeting = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Hi \")");

    public HomePage() {
        this.driver = DriverManager.getDriver();
    }

    public boolean isAppInForeground(String expectedPackage) {
        String current = driver.getCurrentPackage();
        return current != null && current.equalsIgnoreCase(expectedPackage);
    }

    public String getCurrentPackage() {
        return driver.getCurrentPackage();
    }

    public String getCurrentActivity() {
        return driver.currentActivity();
    }

    public boolean isAnyPrimaryUiVisible() {
        List<By> candidates = List.of(
                homeTab,
                AppiumBy.accessibilityId("Home"),
                AppiumBy.accessibilityId("Search"),
                AppiumBy.accessibilityId("Search Flight"),
                AppiumBy.accessibilityId("Log In"),
                greeting,
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Travel\")"),
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Search\")"),
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Book\")"),
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Login\")"),
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Sign\")"),
                AppiumBy.className("android.widget.FrameLayout")
        );

        for (By locator : candidates) {
            List<WebElement> found = driver.findElements(locator);
            if (!found.isEmpty() && found.get(0).isDisplayed()) {
                return true;
            }
        }
        return false;
    }

    public void waitForUiReady() {
        DriverManager.getWait().until(ExpectedConditions.presenceOfElementLocated(
                AppiumBy.className("android.widget.FrameLayout")));
    }

    public void tapIfVisible(By locator) {
        List<WebElement> elements = driver.findElements(locator);
        if (!elements.isEmpty() && elements.get(0).isDisplayed()) {
            elements.get(0).click();
        }
    }

    public void bringAppToForeground() {
        try {
            driver.activateApp(com.travelhouse.config.ConfigReader.get("app.package", "com.travelhouse.uk.app"));
        } catch (Exception e) {
            // Fallback launch
            driver.executeScript("mobile: shell",
                    java.util.Map.of("command", "monkey",
                            "args", java.util.List.of("-p",
                                    com.travelhouse.config.ConfigReader.get("app.package", "com.travelhouse.uk.app"),
                                    "-c", "android.intent.category.LAUNCHER", "1")));
        }
        // No fixed sleep after bring-to-foreground
    }

    public void openHomeTab() {
        bringAppToForeground();
        // Prefer exact Home tab desc; avoid broad "Home" contains search (hangs when UiAutomator freezes)
        if (!UiHelper.tapByDesc("Home\nTab 1 of 4")) {
            UiHelper.tapByDescContains("Tab 1 of 4");
        }
        UiHelper.waitForDescContains("Search Flight", 5);
    }

    public boolean isHomeDisplayed() {
        return UiHelper.isAnyDisplayed(searchFlight, oneWay, flyingFrom, goingTo)
                || UiHelper.waitForDescContains("Search Flight", 1)
                || UiHelper.waitForDescContains("Hi ", 1)
                || UiHelper.waitForDescContains("Flying From", 1);
    }

    /**
     * Scrolls Home and verifies search form + key content sections are present and non-empty.
     */
    public void verifyHomePageContents() {
        openHomeTab();
        scrollToFlightSearchForm();
        Assert.assertTrue(isHomeDisplayed(), "Flight search form should be visible on Home");

        assertVisibleNonEmpty("One Way");
        assertVisibleNonEmpty("Return");
        assertVisibleNonEmpty("Flying From");
        assertVisibleNonEmpty("Going to");
        assertVisibleNonEmpty("Departure");
        assertVisibleNonEmpty("Search Flight");

        Assert.assertTrue(
                UiHelper.waitForDescContains("Home", 3)
                        || !driver.findElements(homeTab).isEmpty(),
                "Home tab should be visible");
        Assert.assertTrue(
                UiHelper.waitForDescContains("Bookings", 3)
                        || UiHelper.waitForDescContains("Explore", 2)
                        || UiHelper.waitForDescContains("Profile", 2),
                "At least one other bottom tab (Bookings/Explore/Profile) should be visible");

        // Scroll down through Home content
        boolean foundDestinations = scrollUntilVisible(
                "Most travelled",
                "Most Travelled",
                "Travelled Flight Destinations",
                "Flight Destinations",
                "Popular Destinations",
                "Destinations");
        Assert.assertTrue(foundDestinations,
                "Most travelled Flight Destinations (or equivalent) should be visible");

        boolean foundUpcoming = scrollUntilVisible(
                "Your Upcoming Flights",
                "Upcoming Flights",
                "Upcoming",
                "My Flights");
        Assert.assertTrue(foundUpcoming,
                "Your Upcoming Flights (or equivalent) should be visible");

        // Scroll up again and confirm search form still intact
        for (int i = 0; i < 6; i++) {
            GestureUtil.swipeDown();
            pause(400);
        }
        scrollToFlightSearchForm();
        assertVisibleNonEmpty("Search Flight");
        assertVisibleNonEmpty("One Way");
        assertVisibleNonEmpty("Return");
        Assert.assertTrue(isHomeDisplayed(), "Home search form should remain complete after scroll up/down");
    }

    /** Swipe to the top until One Way / Return / Search Flight are available. */
    public void scrollToFlightSearchForm() {
        openHomeTab();
        for (int i = 0; i < 8; i++) {
            if (UiHelper.waitForDesc("One Way", 1) && UiHelper.waitForDesc("Return", 1)
                    && UiHelper.isAnyDisplayed(searchFlight)) {
                return;
            }
            GestureUtil.swipeDown();
            pause(500);
        }
        // One more Home tab tap in case we were on another section
        openHomeTab();
        pause(800);
    }

    private boolean scrollUntilVisible(String... labels) {
        for (int i = 0; i < 8; i++) {
            for (String label : labels) {
                if (UiHelper.waitForDescContains(label, 1)
                        || !driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().textContains(\"" + label.replace("\"", "") + "\")")).isEmpty()) {
                    System.out.println("[Home] Found section: " + label);
                    return true;
                }
            }
            GestureUtil.swipeUp();
            pause(700);
        }
        return false;
    }

    private void assertVisibleNonEmpty(String label) {
        Assert.assertTrue(
                UiHelper.waitForDescContains(label, 8)
                        || UiHelper.waitForDesc(label, 2),
                "Home content missing or empty: " + label);
        List<WebElement> nodes = driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + label.replace("\"", "") + "\")"));
        if (!nodes.isEmpty()) {
            String desc = nodes.get(0).getAttribute("contentDescription");
            Assert.assertTrue(desc != null && !desc.isBlank(),
                    "Home content description should not be empty for: " + label);
        }
    }

    private static void pause(long ms) {
        // No fixed sleeps — rely on explicit waits for UI state.
    }

    public void tapOneWay() {
        scrollToFlightSearchForm();
        if (!UiHelper.waitForDesc("One Way", 10)) {
            openHomeTab();
            scrollToFlightSearchForm();
        }
        if (!UiHelper.tapByDesc("One Way")) {
            tapRequired(oneWay, "One Way");
        }
    }

    public void tapReturn() {
        scrollToFlightSearchForm();
        if (!UiHelper.waitForDesc("Return", 8)) {
            openHomeTab();
            scrollToFlightSearchForm();
        }
        // Prefer exact trip-type "Return" over date field "Return\n..."
        if (UiHelper.tapByDesc("Return")) {
            return;
        }
        List<WebElement> options = driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().description(\"Return\")"));
        if (!options.isEmpty()) {
            options.get(0).click();
            return;
        }
        // Last resort: description equals Return among clickables near One Way
        List<WebElement> near = driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().clickable(true).descriptionContains(\"Return\")"));
        for (WebElement el : near) {
            try {
                String desc = el.getAttribute("contentDescription");
                if (desc != null && desc.trim().equals("Return")) {
                    el.click();
                    return;
                }
            } catch (Exception ignored) {
                // next
            }
        }
        throw new IllegalStateException("Return trip type control not found");
    }

    public void openFlyingFrom() {
        tapRequired(flyingFrom, "Flying From");
    }

    public void openGoingTo() {
        tapRequired(goingTo, "Going to");
    }

    /**
     * Picks the seeded recent search London–Islamabad for 2–30 Mar 2027
     * (same approach as the Playwright installment chunk — avoids calendar navigation).
     */
    public void chooseExactRecentSearch(String fromCity, String toCity, String dateRangeHint) {
        openHomeTab();
        scrollToFlightSearchForm();
        boolean opened = false;
        for (int attempt = 1; attempt <= 3 && !opened; attempt++) {
            UiHelper.tapByDescContains("Going to");
            long deadline = System.currentTimeMillis() + 7_000L;
            while (System.currentTimeMillis() < deadline) {
                if (!driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().descriptionContains(\"" + dateRangeHint + "\")")).isEmpty()) {
                    opened = true;
                    break;
                }
            }
        }
        Assert.assertTrue(opened, "Recent-search page did not open after 3 attempts");

        List<WebElement> candidates = driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + dateRangeHint + "\")"));
        WebElement recent = null;
        for (WebElement candidate : candidates) {
            String label = safeDesc(candidate);
            if (label.contains(fromCity) && label.contains(toCity)) {
                recent = candidate;
                break;
            }
        }
        Assert.assertNotNull(recent,
                "Exact recent search unavailable. Seed " + fromCity + "–" + toCity
                        + ", " + dateRangeHint + " once before running the suite.");
        recent.click();
        Assert.assertTrue(
                UiHelper.waitForDescContains("Going to", 8)
                        && UiHelper.waitForDescContains(toCity, 8),
                "Home should show Going to " + toCity + " after recent search");
    }

    /**
     * Search Flight until Cheapest/Fastest results appear (full-payment path).
     */
    public void searchForFullPayment() {
        scrollToFlightSearchForm();
        for (int attempt = 1; attempt <= 3; attempt++) {
            tapSearchFlight();
            long deadline = System.currentTimeMillis() + 120_000L;
            while (System.currentTimeMillis() < deadline) {
                if (!driver.findElements(AppiumBy.accessibilityId("Cheapest")).isEmpty()
                        || UiHelper.isDescPresent("Cheapest")) {
                    return;
                }
                if (UiHelper.isDescPresent("unable to process")) {
                    UiHelper.tapByDesc("OK");
                    UiHelper.waitForDesc("Search Flight", 15);
                    break;
                }
            }
        }
        throw new IllegalStateException(
                "Flight search did not show Cheapest/Fastest after 3 attempts");
    }

    /**
     * Search Flight until Pay in Installment is ready (retry on backend error).
     */
    public void searchUntilPayInInstallment() {
        scrollToFlightSearchForm();
        for (int attempt = 1; attempt <= 3; attempt++) {
            tapSearchFlight();
            long deadline = System.currentTimeMillis() + 120_000L;
            while (System.currentTimeMillis() < deadline) {
                if (!driver.findElements(AppiumBy.accessibilityId("Pay in Installment")).isEmpty()
                        || UiHelper.isDescPresent("Pay in Installment")) {
                    UiHelper.tapByDesc("Pay in Installment");
                    UiHelper.tapByDescContains("Pay in Installment");
                    return;
                }
                if (UiHelper.isDescPresent("unable to process")) {
                    UiHelper.tapByDesc("OK");
                    UiHelper.waitForDesc("Search Flight", 15);
                    break;
                }
            }
        }
        throw new IllegalStateException(
                "Flight search did not reach Pay in Installment after 3 attempts");
    }

    private static String safeDesc(WebElement el) {
        try {
            String d = el.getAttribute("contentDescription");
            return d == null ? "" : d.trim();
        } catch (Exception e) {
            return "";
        }
    }

    public void openDeparture() {
        // Prefer date field "Departure\n2026-08-09" over bare "Departure" tab inside the picker
        List<WebElement> candidates = driver.findElements(
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Departure\")"));
        for (WebElement el : candidates) {
            try {
                String desc = el.getAttribute("contentDescription");
                if (desc != null && desc.contains("\n") && desc.length() > "Departure".length()) {
                    el.click();
                    return;
                }
            } catch (Exception ignored) {
                // try next
            }
        }
        tapRequired(departure, "Departure");
    }

    public void openReturnDate() {
        // Date field content-desc is like "Return\n2026-07-31" (newline + date), not trip-type "Return"
        List<WebElement> candidates = driver.findElements(
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Return\")"));
        for (WebElement el : candidates) {
            try {
                String desc = el.getAttribute("contentDescription");
                if (desc != null && desc.contains("\n") && desc.length() > "Return".length()) {
                    el.click();
                    return;
                }
            } catch (Exception ignored) {
                // try next
            }
        }
        throw new IllegalStateException("Return date field not found");
    }

    public void openPassengerClass() {
        tapRequired(passengerClass, "Select Passenger/Seat Class");
    }

    public void tapSearchFlight() {
        // After date/airport pickers the CTA can be off-screen or momentarily missing
        for (int attempt = 0; attempt < 6; attempt++) {
            List<WebElement> elements = driver.findElements(searchFlight);
            if (elements.isEmpty()) {
                elements = driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().descriptionContains(\"Search Flight\")"));
            }
            if (!elements.isEmpty()) {
                try {
                    if (elements.get(0).isDisplayed()) {
                        elements.get(0).click();
                        return;
                    }
                } catch (Exception ignored) {
                    // try coordinate / next attempt
                }
                try {
                    tapCenter(elements.get(0));
                    return;
                } catch (Exception ignored) {
                    // continue
                }
            }
            if (attempt % 2 == 0) {
                GestureUtil.swipeUp();
            } else {
                GestureUtil.swipeDown();
            }
            pause(600);
            if (attempt == 3) {
                openHomeTab();
                scrollToFlightSearchForm();
            }
        }
        throw new IllegalStateException("Search Flight not found on Home");
    }

    private void tapCenter(WebElement element) {
        org.openqa.selenium.Rectangle rect = element.getRect();
        int x = rect.x + Math.max(1, rect.width / 2);
        int y = rect.y + Math.max(1, rect.height / 2);
        org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence tap =
                new org.openqa.selenium.interactions.Sequence(finger, 1);
        tap.addAction(finger.createPointerMove(java.time.Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        tap.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        tap.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(java.util.Collections.singletonList(tap));
    }

    private void tapRequired(By locator, String label) {
        List<WebElement> elements = driver.findElements(locator);
        if (elements.isEmpty() || !elements.get(0).isDisplayed()) {
            throw new IllegalStateException(label + " not found on Home");
        }
        elements.get(0).click();
    }
}
