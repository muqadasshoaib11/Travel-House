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
        return UiHelper.waitForDescContains("Select dates", 3)
                || UiHelper.waitForDescContains("Apply", 2);
    }

    public void waitUntilVisible() {
        if (!UiHelper.waitForDescContains("Select dates", 8)
                && !UiHelper.waitForDescContains("Apply", 3)) {
            throw new IllegalStateException("Date picker (Select dates) not visible");
        }
    }

    /** Selects departure + return days and taps Apply. */
    public void selectDepartureAndReturn(LocalDate departure, LocalDate returnDate) {
        waitUntilVisible();
        // Ensure Departure tab active for first pick
        UiHelper.tapByDesc("Departure");
        pause(400);
        tapDay(departure);
        pause(800);
        UiHelper.tapByDesc("Return");
        pause(400);
        tapDay(returnDate);
        pause(600);
        if (!UiHelper.tapByDesc("Apply") && !UiHelper.tapByDescContains("Apply")) {
            throw new IllegalStateException("Apply not found on date picker");
        }
        ExtentReportManager.logInfo("Selected dates " + departure + " → " + returnDate);
        pause(1200);
    }

    public void tapDay(LocalDate day) {
        if (tryTapDay(day)) {
            return;
        }
        boolean farFuture = day.isAfter(LocalDate.now().plusDays(40));
        for (int i = 0; i < 14; i++) {
            if (farFuture) {
                GestureUtil.swipeUp();
            } else if (i < 3) {
                GestureUtil.swipeDown();
            } else {
                GestureUtil.swipeUp();
            }
            pause(650);
            if (tryTapDay(day)) {
                return;
            }
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
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
