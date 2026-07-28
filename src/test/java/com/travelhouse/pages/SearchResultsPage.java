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
        for (int i = 0; i < 12; i++) {
            if (!findResultCards().isEmpty()) {
                ready = true;
                break;
            }
            if (UiHelper.waitForDescContains("Cheapest", 3)
                    || UiHelper.waitForDescContains("Fastest", 2)
                    || UiHelper.waitForDescContains("Pay", 2)) {
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

    public boolean hasResults() {
        waitForResults();
        return !findResultCards().isEmpty()
                || !driver.findElements(AppiumBy.accessibilityId("Cheapest")).isEmpty()
                || !driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Cheapest\")")).isEmpty();
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

    public void selectCheapest() {
        tapFilter("Cheapest");
        selectFirstResultCard();
    }

    public void selectFastest() {
        tapFilter("Fastest");
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
        List<String> missing = new ArrayList<>();

        boolean hasRoute = lower.contains("departure")
                || lower.contains("london")
                || lower.contains("islamabad")
                || lower.contains("jeddah")
                || lower.contains("karachi")
                || lower.contains("airport")
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

    private List<WebElement> findResultCards() {
        List<WebElement> cards = driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Pay\")"));
        if (!cards.isEmpty()) {
            return cards;
        }
        cards = driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Departure\")"));
        if (!cards.isEmpty()) {
            return cards;
        }
        cards = driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"£\")"));
        if (!cards.isEmpty()) {
            return cards;
        }
        // Some builds expose flight rows as clickable nodes with airport codes / times
        return driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"h \").descriptionContains(\"m\")"));
    }

    private void selectFirstResultCard() {
        List<WebElement> cards = findResultCards();
        if (cards.isEmpty()) {
            GestureUtil.swipeUp();
            pause(1000);
            cards = findResultCards();
        }
        Assert.assertFalse(cards.isEmpty(), "Could not find a flight result card to select");

        WebElement card = cards.get(0);
        Rectangle rect = card.getRect();
        try {
            card.click();
            pause(2500);
            if (reachedDetails()) {
                return;
            }
        } catch (Exception ignored) {
            // coordinate tap
        }
        for (double ratio : new double[]{0.90, 0.80, 0.70}) {
            tapAt(rect, ratio);
            pause(2500);
            if (reachedDetails()) {
                return;
            }
            cards = findResultCards();
            if (!cards.isEmpty()) {
                rect = cards.get(0).getRect();
            }
        }
        Assert.fail("Selected flight card but details / price screen did not open");
    }

    private boolean reachedDetails() {
        return !driver.findElements(AppiumBy.accessibilityId("Proceed with payment")).isEmpty()
                || !driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Proceed with payment\")")).isEmpty()
                || !driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Price Summary\")")).isEmpty()
                || !driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Outbound\")")).isEmpty()
                || !driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Full Payment\")")).isEmpty()
                || !driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Installment\")")).isEmpty();
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
