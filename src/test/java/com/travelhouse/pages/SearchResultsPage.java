package com.travelhouse.pages;

import com.travelhouse.base.DriverManager;
import com.travelhouse.utils.ExtentReportManager;
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
        // Flight search is network-bound — must wait for results chrome (pause() is a no-op).
        if (UiHelper.waitForDescContains("Cheapest", 60)
                || UiHelper.waitForDescContains("Fastest", 5)
                || UiHelper.waitForDescContains("Pay in Installment", 5)
                || UiHelper.waitForDescContains("Pay £", 5)) {
            return;
        }
        // Still loading / empty — one more short poll for any Pay CTA on listings
        UiHelper.waitForDescContains("Pay", 10);
    }

    /**
     * True when flight results chrome is visible (Cheapest/Fastest filters).
     * Do NOT treat bare "Pay" as results — Price Summary's "Proceed with payment"
     * matches that and previously made navigateBackToResults stop too early.
     */
    public boolean isResultsScreen() {
        if (isBookingSummaryOrTravellerScreen()) {
            return false;
        }
        return UiHelper.isDescPresent("Cheapest")
                || UiHelper.isDescPresent("Fastest")
                || UiHelper.isDescPresent("Pay in Installment");
    }

    /** Post-results booking screens that must not be treated as search results. */
    public boolean isBookingSummaryOrTravellerScreen() {
        return UiHelper.isDescPresent("Proceed with payment")
                || UiHelper.isDescPresent("Proceed With Query")
                || UiHelper.isDescPresent("Price Summary")
                || UiHelper.isDescPresent("Price Detail")
                || UiHelper.isDescPresent("My Travellers")
                || UiHelper.isDescPresent("Who's Going");
    }

    public boolean hasResults() {
        waitForResults();
        if (isBookingSummaryOrTravellerScreen()) {
            return false;
        }
        return isResultsScreen()
                || UiHelper.isDescPresent("Pay in Installment")
                || !findResultCards().isEmpty();
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

    /** Taps the Nth visible "Pay in Installment" CTA on search results. */
    public void selectPayInInstallmentAtIndex(int index) {
        scrollResultsToTop();
        pause(800);
        for (int swipe = 0; swipe < index; swipe++) {
            GestureUtil.swipeUp();
            pause(700);
        }
        List<WebElement> buttons = driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Pay in Installment\")"));
        if (buttons.isEmpty()) {
            buttons = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Pay in Instalment\")"));
        }
        Assert.assertFalse(buttons.isEmpty(),
                "Pay in Installment not found — departure must be ~2 months ahead");
        // Prefer on-screen mid/lower CTAs; skip off-screen or tiny stubs
        List<WebElement> usable = new ArrayList<>();
        for (WebElement el : buttons) {
            Rectangle r = el.getRect();
            if (r.height >= 40 && r.y >= 200 && r.y < 2200) {
                usable.add(el);
            }
        }
        if (usable.isEmpty()) {
            usable = buttons;
        }
        WebElement target = usable.get(Math.min(index % usable.size(), usable.size() - 1));
        Rectangle rect = target.getRect();
        try {
            target.click();
        } catch (Exception ignored) {
            // fall through
        }
        if (waitUntilLeftResults(8)) {
            ExtentReportManager.logInfo("Selected Pay in Installment on card index " + index);
            return;
        }
        adbTap(rect.x + rect.width / 2, rect.y + rect.height / 2);
        Assert.assertTrue(waitUntilLeftResults(10),
                "Flight details should open after Pay in Installment index " + index);
        ExtentReportManager.logInfo("Selected Pay in Installment on card index " + index);
    }

    public boolean isPayInInstallmentVisible() {
        return waitForPayInInstallmentVisible(15);
    }

    public boolean waitForPayInInstallmentVisible(int seconds) {
        scrollResultsToTop();
        long deadline = System.currentTimeMillis() + Math.max(3, seconds) * 1000L;
        while (System.currentTimeMillis() < deadline) {
            if (payInInstallmentPresent()) {
                return true;
            }
            GestureUtil.swipeUp();
            if (payInInstallmentPresent()) {
                return true;
            }
            GestureUtil.swipeDown();
        }
        scrollResultsToTop();
        return payInInstallmentPresent();
    }

    private boolean payInInstallmentPresent() {
        return UiHelper.isDescPresent("Pay in Installment")
                || UiHelper.isDescPresent("Pay in Instalment")
                || UiHelper.isDescPresent("Pay in Installments")
                || UiHelper.isDescPresent("Pay in Instalments")
                || !driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Pay in Install\")")).isEmpty()
                || !driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Pay in Instal\")")).isEmpty();
    }

    /** Selects the Pay button at the given index (0-based) among visible priced flights. */
    public void selectPayAtIndex(int index) {
        scrollResultsToTop();
        pause(800);
        for (int swipe = 0; swipe < index; swipe++) {
            GestureUtil.swipeUp();
            pause(700);
        }
        if (index == 0 && tapPayButtonAndWait()) {
            return;
        }
        List<WebElement> payButtons = driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Pay\")"));
        List<WebElement> usable = new ArrayList<>();
        for (WebElement el : payButtons) {
            String desc = safeDesc(el);
            String lower = desc.toLowerCase();
            if (lower.contains("proceed") || lower.contains("pay in installment")
                    || lower.contains("pay in instalment")) {
                continue;
            }
            if (desc.contains("Pay") || desc.contains("£") || desc.contains("\u00a3")) {
                usable.add(el);
            }
        }
        Assert.assertFalse(usable.isEmpty(), "No Pay buttons found to select flight index " + index);
        WebElement target = usable.get(Math.min(index % usable.size(), usable.size() - 1));
        Rectangle rect = target.getRect();
        try {
            target.click();
        } catch (Exception ignored) {
            // Flutter often needs coordinate taps
        }
        if (waitUntilLeftResults(8)) {
            return;
        }
        // Pink Pay CTA sits at bottom of card — try lower Y ratios + adb
        for (double ratio : new double[]{0.92, 0.85, 0.75, 0.60}) {
            tapAt(rect, ratio);
            if (waitUntilLeftResults(6)) {
                return;
            }
            adbTap(rect.x + rect.width / 2, rect.y + (int) (rect.height * ratio));
            if (waitUntilLeftResults(6)) {
                return;
            }
        }
        Assert.fail("Flight details should open after selecting Pay index " + index);
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
        // Skip short Pay CTAs that are not full itinerary rows
        if ((lower.equals("pay in installment") || lower.startsWith("pay £") || lower.matches("pay\\s*£?\\d.*"))
                && !lower.contains("departure") && !lower.contains("london")) {
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
            // filter re-sort — brief poll for Pay CTA
            UiHelper.waitForDescContains("Pay", 5);
        } else {
            UiHelper.waitForDescContains("Pay", 3);
        }
    }

    /**
     * Prefer priced flight rows. Do NOT match bare "Departure" — that hits the Home date field.
     */
    private List<WebElement> findResultCards() {
        // Prefer full itinerary rows (Departure\nLondon\n...) over tiny Pay buttons
        List<WebElement> cards = driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Departure\").descriptionContains(\"Pay\")"));
        cards = filterOutHomeNodes(cards);
        if (!cards.isEmpty()) {
            return cards;
        }
        cards = driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Pay\")"));
        cards = filterOutNonFlightPayNodes(cards);
        if (!cards.isEmpty()) {
            return cards;
        }
        cards = driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"£\")"));
        if (!cards.isEmpty()) {
            return cards;
        }
        cards = driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"h \").descriptionContains(\"m\")"));
        if (!cards.isEmpty()) {
            return filterOutHomeNodes(cards);
        }
        return Collections.emptyList();
    }

    private List<WebElement> filterOutNonFlightPayNodes(List<WebElement> candidates) {
        List<WebElement> filtered = new ArrayList<>();
        for (WebElement el : candidates) {
            String desc = safeDesc(el).toLowerCase();
            if (desc.contains("pay in installment") || desc.contains("pay in instalment")
                    || desc.contains("proceed")) {
                continue;
            }
            if (desc.contains("search flight") || desc.contains("flying from") || desc.contains("going to")) {
                continue;
            }
            filtered.add(el);
        }
        return filtered;
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
                        "new UiSelector().descriptionContains(\"Pay\")"));
            }
            WebElement pay = pickBestPayTarget(payButtons);
            if (pay != null) {
                Rectangle rect = pay.getRect();
                System.out.println("[Results] Tapping Pay target h=" + rect.height + " y=" + rect.y
                        + " desc=" + snippet(safeDesc(pay)));
                try {
                    pay.click();
                } catch (Exception ignored) {
                    // coordinate fallbacks below
                }
                if (waitUntilLeftResults(8)) {
                    return true;
                }
                // Pink CTA is near the bottom of the flight card
                for (double ratio : new double[]{0.92, 0.85, 0.78}) {
                    adbTap(rect.x + rect.width / 2, rect.y + (int) (rect.height * ratio));
                    if (waitUntilLeftResults(6)) {
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[Results] Pay tap failed: " + e.getMessage());
        }
        return UiHelper.tapByDescContains("Pay £") && waitUntilLeftResults(10);
    }

    /** Prefer a full-height flight card with Pay; skip clipped header stubs (h &lt; ~120). */
    private WebElement pickBestPayTarget(List<WebElement> payButtons) {
        WebElement best = null;
        int bestHeight = 0;
        for (WebElement el : payButtons) {
            String lower = safeDesc(el).toLowerCase();
            if (lower.contains("proceed") || lower.contains("pay in installment")
                    || lower.contains("pay in instalment")) {
                continue;
            }
            if (!lower.contains("pay") && !lower.contains("£") && !lower.contains("\u00a3")) {
                continue;
            }
            Rectangle rect = el.getRect();
            if (rect.height < 120) {
                continue; // clipped stub above the first full card
            }
            if (rect.height > bestHeight) {
                bestHeight = rect.height;
                best = el;
            }
        }
        return best;
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
        // No fixed sleeps — rely on explicit waits for UI state.
    }
}
