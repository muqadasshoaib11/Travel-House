package com.travelhouse.pages;

import com.travelhouse.utils.UiHelper;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;

/**
 * Fare type — Full Payment / Installments when the screen is shown.
 */
public class FareSelectionPage {

    private final By fullPaymentDesc = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Full Payment\")");
    private final By installmentsDesc = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Installment\")");

    public boolean isDisplayed() {
        return UiHelper.isAnyDisplayed(fullPaymentDesc, installmentsDesc)
                || UiHelper.isAnyDisplayed(
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Full Payment\")"),
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Installment\")"),
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Pay in full\")"),
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Pay monthly\")"),
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Instalment\")"),
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Select Fare\")"));
    }

    /** Polls (and gently scrolls) until Full Payment / Installments options appear. */
    public boolean waitUntilDisplayed(int timeoutSeconds) {
        long deadline = System.currentTimeMillis() + timeoutSeconds * 1000L;
        int tick = 0;
        while (System.currentTimeMillis() < deadline) {
            if (isDisplayed()) {
                return true;
            }
            // Some fare cards sit below the itinerary fold
            if (tick > 0 && tick % 3 == 0) {
                try {
                    com.travelhouse.utils.GestureUtil.swipeUp();
                } catch (Exception ignored) {
                    // ignore
                }
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return isDisplayed();
            }
            tick++;
        }
        return isDisplayed();
    }

    public boolean isFullPaymentVisible() {
        return UiHelper.waitForDescContains("Full Payment", 3)
                || UiHelper.isAnyDisplayed(
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Full Payment\")"));
    }

    public boolean isInstallmentsVisible() {
        return UiHelper.waitForDescContains("Installment", 3)
                || UiHelper.waitForDescContains("Instalment", 2)
                || UiHelper.isAnyDisplayed(
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Installment\")"),
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Instalment\")"));
    }

    /** Asserts both Full Payment and Installments are offered when the fare screen appears. */
    public void assertBothFareOptionsVisible() {
        if (!isDisplayed()) {
            throw new IllegalStateException("Fare selection screen is not displayed");
        }
        if (!isFullPaymentVisible()) {
            throw new IllegalStateException("Full Payment option not visible on fare screen");
        }
        if (!isInstallmentsVisible()) {
            throw new IllegalStateException("Installments option not visible on fare screen");
        }
    }

    public void selectFullPayment() {
        if (!UiHelper.tapByDescContains("Full Payment") && !UiHelper.tapByTextContains("Full Payment")) {
            throw new IllegalStateException("Full Payment option not found");
        }
    }

    public void selectInstallments() {
        if (UiHelper.tapByDescContains("Installment") || UiHelper.tapByTextContains("Installment")
                || UiHelper.tapByDescContains("Instalment") || UiHelper.tapByTextContains("Instalment")
                || UiHelper.tapByDescContains("Pay monthly") || UiHelper.tapByTextContains("Pay monthly")
                || UiHelper.tapByDescContains("monthly") || UiHelper.tapByDescContains("instalment")) {
            return;
        }
        throw new IllegalStateException("Installments option not found");
    }

    public void selectFareType(String fareType) {
        if (fareType != null && fareType.toLowerCase().contains("install")) {
            selectInstallments();
        } else {
            selectFullPayment();
        }
    }

    public void continueIfPresent() {
        UiHelper.tapByDesc("Continue");
        UiHelper.tapByDescContains("Continue");
        UiHelper.tapByDescContains("Next");
        UiHelper.tapByDescContains("Proceed");
    }
}
