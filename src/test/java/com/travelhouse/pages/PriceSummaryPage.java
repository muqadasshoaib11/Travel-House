package com.travelhouse.pages;

import com.travelhouse.base.DriverManager;
import com.travelhouse.utils.UiHelper;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Price summary steps:
 * 1) Flight detail summary with "Proceed with payment"
 * 2) Totals summary with "Total Price", T&amp;C checkbox, "Continue"
 */
public class PriceSummaryPage {

    private final By summaryHint = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Price Summary\")");
    private final By totalHint = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Total Price\")");
    private final By netPrice = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Net Price\")");
    private final By proceed = AppiumBy.accessibilityId("Proceed with payment");
    private final By continueBtn = AppiumBy.accessibilityId("Continue");

    public boolean isDisplayed() {
        return UiHelper.isAnyDisplayed(summaryHint, totalHint, netPrice, proceed)
                || UiHelper.isAnyDisplayed(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Price\")"));
    }

    public boolean hasProceedWithPayment() {
        return !DriverManager.getDriver().findElements(proceed).isEmpty()
                || !DriverManager.getDriver().findElements(
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Proceed with payment\")")).isEmpty();
    }

    public boolean hasTotalPrice() {
        return UiHelper.isAnyDisplayed(totalHint, netPrice)
                || !DriverManager.getDriver().findElements(
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Total Price\")")).isEmpty();
    }

    public List<String> getDisplayedTextsSnapshot() {
        List<String> texts = new ArrayList<>();
        List<By> locators = List.of(
                summaryHint, totalHint, netPrice, proceed,
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Pay\")"),
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Adult\")"),
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"ATOL\")"),
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Insurance\")")
        );
        for (By locator : locators) {
            for (WebElement node : DriverManager.getDriver().findElements(locator)) {
                try {
                    String desc = node.getAttribute("contentDescription");
                    if (desc != null && !desc.isBlank()) {
                        texts.add(desc.trim());
                    }
                } catch (Exception ignored) {
                    // skip
                }
            }
        }
        return texts.stream().distinct().collect(Collectors.toList());
    }

    public void proceedWithPayment() {
        if (!UiHelper.tapByDesc("Proceed with payment")
                && !UiHelper.tapByDescContains("Proceed with payment")) {
            throw new IllegalStateException("Proceed with payment not found");
        }
    }

    public void acceptTermsAndConditions() {
        List<WebElement> checkboxes = DriverManager.getDriver().findElements(
                AppiumBy.className("android.widget.CheckBox"));
        if (!checkboxes.isEmpty()) {
            WebElement box = checkboxes.get(0);
            String checked = box.getAttribute("checked");
            if (!"true".equalsIgnoreCase(checked)) {
                box.click();
            }
            return;
        }
        // Fallback: tap "I accept" label (not the Terms link)
        UiHelper.tapByDescContains("I accept");
    }

    public void continueIfPresent() {
        if (hasProceedWithPayment()) {
            proceedWithPayment();
            return;
        }
        if (!DriverManager.getDriver().findElements(continueBtn).isEmpty()
                || UiHelper.waitForDescContains("Continue", 2)) {
            acceptTermsAndConditions();
            UiHelper.tapByDesc("Continue");
            UiHelper.tapByDescContains("Continue");
        }
    }
}
