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
                "new UiSelector().descriptionContains(\"Price\")"))
                || hasProceedCta();
    }

    public boolean hasProceedWithPayment() {
        return hasProceedCta();
    }

    /** "Proceed with payment" or "Proceed With Query" (installment / query path). */
    public boolean hasProceedCta() {
        return !DriverManager.getDriver().findElements(proceed).isEmpty()
                || !DriverManager.getDriver().findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Proceed with payment\")")).isEmpty()
                || !DriverManager.getDriver().findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Proceed With Query\")")).isEmpty()
                || !DriverManager.getDriver().findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"Proceed with Query\")")).isEmpty();
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
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Insurance\")"),
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Proceed\")")
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
        proceedPastSummary();
    }

    public void proceedPastSummary() {
        if (UiHelper.tapByDesc("Proceed with payment")
                || UiHelper.tapByDescContains("Proceed with payment")
                || UiHelper.tapByDesc("Proceed With Query")
                || UiHelper.tapByDescContains("Proceed With Query")
                || UiHelper.tapByDescContains("Proceed with Query")) {
            return;
        }
        throw new IllegalStateException("Proceed CTA not found (payment / query)");
    }

    public void acceptTermsAndConditions() {
        // Prefer empty clickable checkbox node (same pattern as installment sheet)
        try {
            org.openqa.selenium.Dimension size = DriverManager.getDriver().manage().window().getSize();
            int minY = (int) (size.height * 0.65);
            for (WebElement el : DriverManager.getDriver().findElements(
                    AppiumBy.androidUIAutomator("new UiSelector().clickable(true)"))) {
                String desc = "";
                try {
                    desc = el.getAttribute("contentDescription");
                } catch (Exception ignored) {
                    // skip
                }
                if (desc != null && !desc.isBlank()) {
                    continue;
                }
                org.openqa.selenium.Rectangle r = el.getRect();
                if (r.width > 0 && r.width <= 90 && r.height > 0 && r.height <= 90
                        && r.y >= minY && r.x < size.width * 0.25) {
                    el.click();
                    return;
                }
            }
        } catch (Exception ignored) {
            // fall through
        }
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
        UiHelper.tapByDescContains("I accept");
    }

    public void continueIfPresent() {
        if (hasProceedCta()) {
            proceedPastSummary();
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
