package com.travelhouse.pages;

import com.travelhouse.utils.ExtentReportManager;
import com.travelhouse.utils.GestureUtil;
import com.travelhouse.utils.UiHelper;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    private static final DateTimeFormatter CELL_FRAGMENT_PADDED =
            DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);
    private static final DateTimeFormatter CELL_FRAGMENT_UNPADDED =
            DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH);

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
        pause(800);
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
        // Prefer scrolling toward the target month from "today"
        boolean farFuture = day.isAfter(LocalDate.now().plusDays(40));
        for (int i = 0; i < 12; i++) {
            if (farFuture) {
                GestureUtil.swipeUp();
            } else {
                // Near-term dates are usually already on-screen or just below — nudge both ways
                if (i % 2 == 0) {
                    GestureUtil.swipeDown();
                } else {
                    GestureUtil.swipeUp();
                }
            }
            pause(700);
            if (tryTapDay(day)) {
                return;
            }
        }
        throw new IllegalStateException("Calendar day not found: " + CELL_PADDED.format(day)
                + " (also tried unpadded / fragment labels)");
    }

    private boolean tryTapDay(LocalDate day) {
        String[] labels = {
                CELL_PADDED.format(day),
                CELL_UNPADDED.format(day),
                CELL_FRAGMENT_PADDED.format(day),
                CELL_FRAGMENT_UNPADDED.format(day)
        };
        for (String label : labels) {
            if (UiHelper.tapByDesc(label) || UiHelper.tapByDescContains(label)) {
                ExtentReportManager.logInfo("Tapped calendar day: " + label);
                return true;
            }
        }
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
