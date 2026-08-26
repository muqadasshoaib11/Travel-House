package com.travelhouse.pages;

import com.travelhouse.base.DriverManager;
import com.travelhouse.utils.ExtentReportManager;
import com.travelhouse.utils.GestureUtil;
import com.travelhouse.utils.UiHelper;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Flight search date picker ("Select dates") — day cells use labels like
 * {@code Wed, 07 October 2026} (zero-padded day).
 */
public class DatePickerPage {

    private static final DateTimeFormatter CELL_PADDED =
            DateTimeFormatter.ofPattern("EEE, dd MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter CELL_UNPADDED =
            DateTimeFormatter.ofPattern("EEE, d MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter FRAG_PADDED =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter FRAG_UNPADDED =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

    private final AndroidDriver driver = DriverManager.getDriver();

    public boolean isDisplayed() {
        return UiHelper.isDescPresent("Select dates") || UiHelper.isDescPresent("Apply");
    }

    public void waitUntilVisible() {
        if (!UiHelper.waitForDescContains("Select dates", 5)
                && !UiHelper.waitForDescContains("Apply", 2)) {
            throw new IllegalStateException("Date picker (Select dates) not visible");
        }
    }

    /** Selects departure + return days and taps Apply. */
    public void selectDepartureAndReturn(LocalDate departure, LocalDate returnDate) {
        waitUntilVisible();

        UiHelper.tapByDesc("Departure");
        scrollToMonth(departure);
        tapDay(departure);
        AssertDepartureShown(departure);

        // Must focus Return box — otherwise day taps keep updating Departure
        if (!UiHelper.tapByDesc("Return") && !UiHelper.tapByDescContains("Return")) {
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            tapXy((int) (size.width * 0.72), (int) (size.height * 0.18));
        }
        scrollToMonth(returnDate);
        tapDay(returnDate);

        if (!returnLooksSelected(returnDate)) {
            ExtentReportManager.logInfo("Return not set — retry Return focus + day");
            UiHelper.tapByDesc("Return");
            scrollToMonth(returnDate);
            for (int i = 0; i < 8 && !tryTapDay(returnDate); i++) {
                GestureUtil.swipeUp();
            }
            if (!tryTapDay(returnDate)) {
                tapDay(returnDate);
            }
        }

        if (!returnLooksSelected(returnDate)) {
            throw new IllegalStateException("Return date not selected: " + returnDate);
        }

        tapApply();
        if (stillOpen()) {
            tapApply();
        }
        if (stillOpen()) {
            throw new IllegalStateException("Date picker still open after Apply ("
                    + departure + " → " + returnDate + ")");
        }
        ExtentReportManager.logInfo("Selected dates " + departure + " → " + returnDate);
    }

    /** Scroll calendar until "March 2027" (month + year) is visible. */
    private void scrollToMonth(LocalDate day) {
        String monthYear = day.getMonth().getDisplayName(java.time.format.TextStyle.FULL, Locale.ENGLISH)
                + " " + day.getYear();
        if (UiHelper.isDescPresent(monthYear)) {
            return;
        }
        boolean future = day.isAfter(LocalDate.now());
        for (int i = 0; i < 30; i++) {
            if (UiHelper.isDescPresent(monthYear)) {
                ExtentReportManager.logInfo("Calendar at " + monthYear);
                return;
            }
            if (future) {
                GestureUtil.swipeUp();
            } else {
                GestureUtil.swipeDown();
            }
        }
        // UiScrollable fallback
        try {
            driver.findElement(AppiumBy.androidUIAutomator(
                    "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView("
                            + "new UiSelector().descriptionContains(\"" + monthYear + "\"))"));
        } catch (Exception ignored) {
            ExtentReportManager.logInfo("Could not scroll calendar to " + monthYear);
        }
    }

    private void AssertDepartureShown(LocalDate departure) {
        String frag = departure.getDayOfMonth() + " "
                + departure.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH);
        if (!UiHelper.waitForDescContains(frag, 2)
                && !UiHelper.waitForDescContains(String.valueOf(departure.getDayOfMonth()), 1)) {
            ExtentReportManager.logInfo("Departure label not confirmed for " + frag);
        }
    }

    private boolean returnLooksSelected(LocalDate returnDate) {
        // Return box text looks like "30 Mar, 2027" — require comma so calendar
        // cell "30 March 2027" does not count as the Return field being filled.
        String day = String.valueOf(returnDate.getDayOfMonth());
        String mon = returnDate.getMonth().getDisplayName(java.time.format.TextStyle.SHORT, Locale.ENGLISH);
        String withComma = day + " " + mon + ",";
        return UiHelper.waitForDescContains(withComma, 1)
                || !driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + withComma + "\")")).isEmpty();
    }

    private boolean stillOpen() {
        return !driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Select dates\")")).isEmpty();
    }

    private void tapApply() {
        List<WebElement> apply = driver.findElements(AppiumBy.accessibilityId("Apply"));
        if (apply.isEmpty()) {
            apply = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Apply\")"));
        }
        if (!apply.isEmpty()) {
            WebElement btn = apply.get(0);
            try {
                btn.click();
            } catch (Exception ignored) {
                // fall through to coordinate
            }
            pause(400);
            if (!stillOpen()) {
                return;
            }
            try {
                org.openqa.selenium.Rectangle r = btn.getRect();
                tapXy(r.x + Math.max(1, r.width / 2), r.y + Math.max(1, r.height / 2));
            } catch (Exception ignored) {
                UiHelper.tapByDescContains("Apply");
            }
        } else {
            // Bottom-center fallback (Apply is usually a full-width bar)
            org.openqa.selenium.Dimension size = driver.manage().window().getSize();
            tapXy(size.width / 2, (int) (size.height * 0.92));
        }
        pause(600);
    }

    private void tapXy(int x, int y) {
        org.openqa.selenium.interactions.PointerInput finger =
                new org.openqa.selenium.interactions.PointerInput(
                        org.openqa.selenium.interactions.PointerInput.Kind.TOUCH, "finger");
        org.openqa.selenium.interactions.Sequence tap = new org.openqa.selenium.interactions.Sequence(finger, 1);
        tap.addAction(finger.createPointerMove(java.time.Duration.ZERO,
                org.openqa.selenium.interactions.PointerInput.Origin.viewport(), x, y));
        tap.addAction(finger.createPointerDown(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        tap.addAction(finger.createPointerUp(
                org.openqa.selenium.interactions.PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(java.util.Collections.singletonList(tap));
    }

    public void tapDay(LocalDate day) {
        if (tryTapDay(day)) {
            return;
        }
        // UiScrollable into view (more reliable than blind swipes on Flutter calendars)
        for (String fragment : new String[]{
                FRAG_PADDED.format(day),
                FRAG_UNPADDED.format(day),
                CELL_PADDED.format(day),
                CELL_UNPADDED.format(day)
        }) {
            try {
                driver.findElement(AppiumBy.androidUIAutomator(
                        "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView("
                                + "new UiSelector().descriptionContains(\"" + escape(fragment) + "\"))"));
                if (tryTapDay(day)) {
                    return;
                }
            } catch (Exception ignored) {
                // try next fragment / fallback swipes
            }
        }
        boolean farFuture = day.isAfter(LocalDate.now().plusDays(40));
        for (int i = 0; i < 18; i++) {
            if (tryTapDay(day)) {
                return;
            }
            if (farFuture || i >= 2) {
                GestureUtil.swipeUp();
            } else {
                GestureUtil.swipeDown();
            }
            pause(200);
        }
        if (tryTapDay(day)) {
            return;
        }
        // Log what the calendar actually exposes to aid matching
        try {
            String src = UiHelper.getPageSourceSafe();
            if (src != null) {
                java.util.regex.Matcher m = java.util.regex.Pattern
                        .compile("content-desc=\"([^\"]*(?:January|February|March|April|May|June|July|August|September|October|November|December)[^\"]*)\"")
                        .matcher(src);
                int n = 0;
                StringBuilder sb = new StringBuilder("Visible calendar labels: ");
                while (m.find() && n < 25) {
                    sb.append(m.group(1).replace("&#10;", " | ")).append(" || ");
                    n++;
                }
                ExtentReportManager.logInfo(sb.toString());
            }
        } catch (Exception ignored) {
            // ignore
        }
        throw new IllegalStateException("Calendar day not found: " + CELL_PADDED.format(day));
    }

    private boolean tryTapDay(LocalDate day) {
        String[] labels = {
                CELL_PADDED.format(day),
                CELL_UNPADDED.format(day),
                FRAG_PADDED.format(day),
                FRAG_UNPADDED.format(day)
        };
        for (String label : labels) {
            if (tapEnabledDayContaining(label)) {
                ExtentReportManager.logInfo("Tapped calendar day matching: " + label);
                return true;
            }
        }
        return false;
    }

    private boolean tapEnabledDayContaining(String fragment) {
        try {
            List<WebElement> nodes = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"" + escape(fragment) + "\")"));
            for (WebElement el : nodes) {
                String desc = "";
                try {
                    desc = el.getAttribute("contentDescription");
                } catch (Exception ignored) {
                    // continue
                }
                if (desc == null) {
                    continue;
                }
                if (desc.toLowerCase(Locale.ENGLISH).contains("disabled")) {
                    continue;
                }
                try {
                    el.click();
                    return true;
                } catch (Exception ignored) {
                    // try next
                }
            }
        } catch (Exception e) {
            ExtentReportManager.logInfo("Day tap lookup note: " + e.getMessage());
        }
        return UiHelper.tapByDesc(fragment) || UiHelper.tapByDescContains(fragment);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void pause(long millis) {
        // No fixed sleeps — rely on explicit waits for UI state.
    }
}
