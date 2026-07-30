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
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void openHomeTab() {
        bringAppToForeground();
        // Leave overlay screens (e.g. Recent Searches) if present — never let findElements hang forever
        try {
            if (UiHelper.waitForDescContains("Recent Searches", 2)
                    && !UiHelper.waitForDescContains("Search Flight", 1)) {
                DriverManager.getDriver().navigate().back();
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } catch (Exception ignored) {
            // continue — try tapping Home tab anyway
        }

        if (!UiHelper.tapByDesc("Home\nTab 1 of 4")) {
            if (!UiHelper.tapByDescContains("Tab 1 of 4")) {
                UiHelper.tapByDescContains("Home");
            }
        }

        // Wait briefly for flight search form
        UiHelper.waitForDescContains("Search Flight", 8);
    }

    public boolean isHomeDisplayed() {
        return UiHelper.isAnyDisplayed(searchFlight, oneWay, flyingFrom, goingTo);
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
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
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

    public void openDeparture() {
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
        tapRequired(searchFlight, "Search Flight");
    }

    private void tapRequired(By locator, String label) {
        List<WebElement> elements = driver.findElements(locator);
        if (elements.isEmpty() || !elements.get(0).isDisplayed()) {
            throw new IllegalStateException(label + " not found on Home");
        }
        elements.get(0).click();
    }
}
