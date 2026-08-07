package com.travelhouse.pages;

import com.travelhouse.base.DriverManager;
import com.travelhouse.utils.GestureUtil;
import com.travelhouse.utils.UiHelper;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.testng.Assert;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Search results — listing validation + Cheapest/Fastest selection.
 */
public class SearchResultsPage {

    private final AndroidDriver driver;

    public SearchResultsPage() {
        this.driver = DriverManager.getDriver();
    }

    public void waitForResults() {
        boolean ready = false;
        for (int i = 0; i < 18; i++) {
            if (isResultsScreen() && !findResultCards().isEmpty()) {
                ready = true;
                break;
            }
            // Still on Home search form — wait / retry scroll only after leaving Home
            if (UiHelper.waitForDescContains("Search Flight", 1)
                    && !UiHelper.waitForDescContains("Cheapest", 1)) {
                pause(2000);
                continue;
            }
            if (UiHelper.waitForDescContains("Cheapest", 3)
                    || UiHelper.waitForDescContains("Fastest", 2)
                    || UiHelper.waitForDescContains("Pay", 2)
                    || UiHelper.waitForDescContains("£", 2)) {
                pause(1500);
                if (!findResultCards().isEmpty()) {
                    ready = true;
                    break;
                }
                GestureUtil.swipeUp();
                pause(1000);
            } else {
                pause(2000);
            }
        }
        if (!ready) {
            pause(3000);
        }
    }

    /** True when flight results chrome is visible (not the Home search form). */
    public boolean isResultsScreen() {
        return UiHelper.waitForDescContains("Cheapest", 1)
                || UiHelper.waitForDescContains("Fastest", 1)
                || (!driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Pay\")")).isEmpty()
                && driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Search Flight\")")).isEmpty());
    }

    public boolean hasResults() {
        waitForResults();
        return isResultsScreen() && (!findResultCards().isEmpty()
                || !driver.findElements(AppiumBy.accessibilityId("Cheapest")).isEmpty()
                || !driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Cheapest\")")).isEmpty());
    }

    public int getResultCountEstimate() {
        return findResultCards().size();
    }

    /**
     * Scrolls results top → bottom and asserts each listing has required fields.
     */
    public void validateAllListingsHaveRequiredFields() {
        validateAllListingsHaveRequiredFields(null);
    }

    /**
     * @param expectedDestination city name used in route checks (e.g. Jeddah, Karachi); optional
     */
    public void validateAllListingsHaveRequiredFields(String expectedDestination) {
        waitForResults();
        List<String> seen = new ArrayList<>();
        int stableRounds = 0;
        int maxScrolls = 12;

        for (int scroll = 0; scroll <= maxScrolls; scroll++) {
            List<WebElement> cards = findResultCards();
            Assert.assertFalse(cards.isEmpty() && scroll == 0,
                    "No flight listings found on search results");

            for (WebElement card : cards) {
                String desc = safeDesc(card);
                if (desc.isBlank() || seen.contains(desc)) {
                    continue;
                }
                seen.add(desc);
                assertListingComplete(desc, seen.size(), expectedDestination);
            }

            String before = seen.isEmpty() ? "" : seen.get(seen.size() - 1);
            GestureUtil.swipeUp();
            pause(1200);
            List<WebElement> afterCards = findResultCards();
            String after = afterCards.isEmpty() ? "" : safeDesc(afterCards.get(afterCards.size() - 1));
            if (before.equals(after)) {
                stableRounds++;
                if (stableRounds >= 2) {
                    break;
                }
            } else {
                stableRounds = 0;
            }
        }

        Assert.assertFalse(seen.isEmpty(), "Expected at least one validated flight listing");
        System.out.println("[Results] Validated listings count=" + seen.size()
                + (expectedDestination == null ? "" : " for destination=" + expectedDestination));
    }

    /** After validating listings, return toward the top (Cheapest / first cards). */
    public void scrollResultsToTop() {
        for (int i = 0; i < 8; i++) {
            if (UiHelper.waitForDesc("Cheapest", 1) || UiHelper.waitForDescContains("Cheapest", 1)) {
                GestureUtil.swipeDown();
                pause(400);
                return;
            }
            GestureUtil.swipeDown();
            pause(500);
        }
    }

    /** Applies Cheapest sort without selecting a flight. */
    public void applyCheapestFilter() {
        tapFilter("Cheapest");
        Assert.assertTrue(hasResults(), "Results should remain visible after selecting Cheapest");
    }

    /** Applies Fastest sort without selecting a flight. */
    public void applyFastestFilter() {
        tapFilter("Fastest");
        Assert.assertTrue(hasResults(), "Results should remain visible after selecting Fastest");
    }

    /** Selects the Pay button at the given index (0-based) among visible priced flights. */
    public void selectPayAtIndex(int index) {
        scrollResultsToTop();
        pause(800);
        for (int swipe = 0; swipe < index; swipe++) {
            GestureUtil.swipeUp();
            pause(700);
        }
        List<WebElement> payButtons = driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Pay\")"));
        List<WebElement> usable = new ArrayList<>();
        for (WebElement el : payButtons) {
            String desc = safeDesc(el);
            if (desc.toLowerCase().contains("proceed")) {
                continue;
            }
            if (desc.contains("Pay") || desc.contains("£")) {
                usable.add(el);
            }
        }
        Assert.assertFalse(usable.isEmpty(), "No Pay buttons found to select flight index " + index);
        WebElement target = usable.get(Math.min(index % usable.size(), usable.size() - 1));
        Rectangle rect = target.getRect();
        try {
            target.click();
        } catch (Exception ignored) {
            adbTap(rect.x + rect.width / 2, rect.y + rect.height / 2);
        }
        Assert.assertTrue(waitUntilLeftResults(12),
                "Flight details should open after selecting Pay index " + index);
    }

    /** Selects the first visible priced flight (Pay). */
    public void selectFirstFlight() {
        selectFirstResultCard();
    }

    public void selectCheapest() {
        applyCheapestFilter();
        selectFirstResultCard();
    }

    public void selectFastest() {
        applyFastestFilter();
        selectFirstResultCard();
    }

    public void selectByMode(String mode) {
        if (mode != null && mode.toLowerCase().contains("fast")) {
            selectFastest();
        } else {
            selectCheapest();
        }
    }

    private void assertListingComplete(String desc, int index, String expectedDestination) {
        String lower = desc.toLowerCase();
        // Ignore Home-form nodes accidentally collected (Departure date / Search Flight)
        if (lower.contains("search flight") || (lower.contains("passenger") && lower.contains("economy"))) {
            return;
        }
        List<String> missing = new ArrayList<>();

        boolean hasRoute = lower.contains("departure")
                || lower.contains("london")
                || lower.contains("islamabad")
                || lower.contains("jeddah")
                || lower.contains("karachi")
                || lower.contains("airport")
                || lower.contains("lhr")
                || lower.contains("jed")
                || lower.contains("khi")
                || (expectedDestination != null && !expectedDestination.isBlank()
                && lower.contains(expectedDestination.toLowerCase()));
        if (!hasRoute) {
            missing.add("route/departure info");
        }
        if (!desc.contains("Pay") && !desc.contains("£") && !desc.matches("(?s).*\\d+\\.\\d{2}.*")) {
            missing.add("price (Pay / amount)");
        }
        if (!desc.matches("(?s).*\\d{1,2}:\\d{2}.*") && !lower.contains("am") && !lower.contains("pm")) {
            missing.add("departure/arrival time");
        }
        if (!desc.matches("(?s).*\\d+h.*")
                && !lower.contains("duration")
                && !(lower.contains("h") && lower.contains("m"))) {
            missing.add("duration");
        }
        Assert.assertTrue(missing.isEmpty(),
                "Flight listing #" + index + " has empty/missing fields: " + missing
                        + " | content-desc snippet: " + snippet(desc));
    }

    private void tapFilter(String name) {
        if (UiHelper.tapByDesc(name) || UiHelper.tapByDescContains(name)) {
            pause(2500);
        }
        UiHelper.waitForDescContains("Pay", 20);
    }

    /**
     * Prefer priced flight rows. Do NOT match bare "Departure" — that hits the Home date field.
     */
    private List<WebElement> findResultCards() {
        List<WebElement> cards = driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Pay\")"));
        if (!cards.isEmpty()) {
            return cards;
        }
        cards = driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"£\")"));
        if (!cards.isEmpty()) {
            return cards;
        }
        // Flight rows often embed duration like "7h 30m" with airport codes
        cards = driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"h \").descriptionContains(\"m\")"));
        if (!cards.isEmpty()) {
            return filterOutHomeNodes(cards);
        }
        return Collections.emptyList();
    }

    private List<WebElement> filterOutHomeNodes(List<WebElement> candidates) {
        List<WebElement> filtered = new ArrayList<>();
        for (WebElement el : candidates) {
            String desc = safeDesc(el).toLowerCase();
            if (desc.contains("search flight") || desc.contains("flying from") || desc.contains("going to")) {
                continue;
            }
            filtered.add(el);
        }
        return filtered;
    }

    private void selectFirstResultCard() {
        scrollResultsToTop();
        pause(1000);

        if (tapPayButtonAndWait()) {
            return;
        }

        List<WebElement> cards = findResultCards();
        if (cards.isEmpty()) {
            GestureUtil.swipeUp();
            pause(1000);
            cards = findResultCards();
        }
        Assert.assertFalse(cards.isEmpty(), "Could not find a flight result card to select");

        int attempts = Math.min(cards.size(), 3);
        for (int i = 0; i < attempts; i++) {
            List<WebElement> current = findResultCards();
            if (current.isEmpty()) {
                break;
            }
            WebElement card = current.get(Math.min(i, current.size() - 1));
            Rectangle rect = card.getRect();
            try {
                card.click();
            } catch (Exception ignored) {
                // fall through
            }
            if (waitUntilLeftResults(8)) {
                return;
            }
            for (double ratio : new double[]{0.92, 0.85, 0.70}) {
                tapAt(rect, ratio);
                if (waitUntilLeftResults(6)) {
                    return;
                }
            }
            adbTap(rect.x + rect.width / 2, rect.y + (int) (rect.height * 0.90));
            if (waitUntilLeftResults(8)) {
                return;
            }
        }
        Assert.fail("Selected flight card but details / price screen did not open");
    }

    private boolean tapPayButtonAndWait() {
        try {
            List<WebElement> payButtons = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Pay\").clickable(true)"));
            if (payButtons.isEmpty()) {
                payButtons = driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().descriptionContains(\"Pay £\")"));
            }
            if (!payButtons.isEmpty()) {
                WebElement pay = payButtons.get(0);
                Rectangle rect = pay.getRect();
                pay.click();
                if (waitUntilLeftResults(10)) {
                    return true;
                }
                adbTap(rect.x + rect.width / 2, rect.y + rect.height / 2);
                return waitUntilLeftResults(10);
            }
        } catch (Exception e) {
            System.out.println("[Results] Pay tap failed: " + e.getMessage());
        }
        if (UiHelper.tapByDescContains("Pay £") || UiHelper.tapByDescContains("Pay")) {
            return waitUntilLeftResults(10);
        }
        return false;
    }

    private boolean waitUntilLeftResults(int seconds) {
        long deadline = System.currentTimeMillis() + seconds * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (reachedDetails()) {
                return true;
            }
            if (!isResultsChromeVisible()) {
                pause(1500);
                return reachedDetails() || !isResultsChromeVisible();
            }
            pause(500);
        }
        return reachedDetails();
    }

    private boolean isResultsChromeVisible() {
        var previous = driver.manage().timeouts().getImplicitWaitTimeout();
        try {
            driver.manage().timeouts().implicitlyWait(Duration.ZERO);
            return !driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Cheapest\")")).isEmpty()
                    && !driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Fastest\")")).isEmpty();
        } finally {
            try {
                driver.manage().timeouts().implicitlyWait(previous);
            } catch (Exception ignored) {
                // ignore
            }
        }
    }

    private void adbTap(int x, int y) {
        try {
            String udid = com.travelhouse.config.ConfigReader.get("device.udid");
            ProcessBuilder pb = (udid == null || udid.isBlank())
                    ? new ProcessBuilder("adb", "shell", "input", "tap", String.valueOf(x), String.valueOf(y))
                    : new ProcessBuilder("adb", "-s", udid, "shell", "input", "tap",
                    String.valueOf(x), String.valueOf(y));
            pb.redirectErrorStream(true).start().waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
            System.out.println("[Results] adb tap at " + x + "," + y);
        } catch (Exception e) {
            System.out.println("[Results] adb tap failed: " + e.getMessage());
        }
    }

    private boolean reachedDetails() {
        String[] markers = {
                "Proceed with payment",
                "Price Summary",
                "Outbound",
                "Full Payment",
                "Installment",
                "Select Fare",
                "Total Price",
                "Terms and Conditions",
                "My Travellers",
                "Traveller Information",
                "Payment method",
                "Review your trip",
                "Flight details"
        };
        var previous = driver.manage().timeouts().getImplicitWaitTimeout();
        try {
            driver.manage().timeouts().implicitlyWait(Duration.ZERO);
            for (String marker : markers) {
                if (!driver.findElements(AppiumBy.androidUIAutomator(
                        "new UiSelector().descriptionContains(\"" + marker + "\")")).isEmpty()) {
                    return true;
                }
            }
            return !driver.findElements(AppiumBy.accessibilityId("Proceed with payment")).isEmpty();
        } finally {
            try {
                driver.manage().timeouts().implicitlyWait(previous);
            } catch (Exception ignored) {
                // session may have moved on
            }
        }
    }

    private void tapAt(Rectangle rect, double yRatio) {
        int x = rect.x + rect.width / 2;
        int y = rect.y + (int) (rect.height * yRatio);
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 1);
        tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
        tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(tap));
    }

    private static String safeDesc(WebElement el) {
        try {
            String d = el.getAttribute("contentDescription");
            return d == null ? "" : d.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private static String snippet(String desc) {
        return desc.length() > 160 ? desc.substring(0, 160) + "…" : desc;
    }

    private static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
