package com.travelhouse.pages;

import com.travelhouse.utils.ExtentReportManager;
import com.travelhouse.utils.GestureUtil;
import com.travelhouse.utils.UiHelper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Flight search date picker ("Select dates") — day cells use labels like
 * {@code Wed, 07 October 2026}.
 */
public class DatePickerPage {

    private static final DateTimeFormatter CELL =
            DateTimeFormatter.ofPattern("EEE, dd MMMM yyyy", Locale.ENGLISH);

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
        tapDay(departure);
        pause(600);
        tapDay(returnDate);
        pause(600);
        if (!UiHelper.tapByDesc("Apply") && !UiHelper.tapByDescContains("Apply")) {
            throw new IllegalStateException("Apply not found on date picker");
        }
        ExtentReportManager.logInfo("Selected dates " + departure + " → " + returnDate);
        pause(1200);
    }

    public void tapDay(LocalDate day) {
        String label = CELL.format(day);
        if (tapDayLabel(label)) {
            return;
        }
        // Scroll calendar until the day cell is visible (future months)
        for (int i = 0; i < 10; i++) {
            GestureUtil.swipeUp();
            pause(700);
            if (tapDayLabel(label)) {
                return;
            }
        }
        // Try scrolling back if we overshot
        for (int i = 0; i < 6; i++) {
            GestureUtil.swipeDown();
            pause(700);
            if (tapDayLabel(label)) {
                return;
            }
        }
        throw new IllegalStateException("Calendar day not found: " + label);
    }

    private boolean tapDayLabel(String label) {
        if (UiHelper.tapByDesc(label) || UiHelper.tapByDescContains(label)) {
            ExtentReportManager.logInfo("Tapped calendar day: " + label);
            return true;
        }
        // Disabled dates include ", Disabled date" — never select those
        return false;
    }

    private static void pause(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
