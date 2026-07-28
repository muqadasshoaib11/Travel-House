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
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Installment\")"));
    }

    public void selectFullPayment() {
        if (!UiHelper.tapByDescContains("Full Payment") && !UiHelper.tapByTextContains("Full Payment")) {
            throw new IllegalStateException("Full Payment option not found");
        }
    }

    public void selectInstallments() {
        if (!UiHelper.tapByDescContains("Installment") && !UiHelper.tapByTextContains("Installment")
                && !UiHelper.tapByDescContains("Instalment") && !UiHelper.tapByTextContains("Instalment")) {
            throw new IllegalStateException("Installments option not found");
        }
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
