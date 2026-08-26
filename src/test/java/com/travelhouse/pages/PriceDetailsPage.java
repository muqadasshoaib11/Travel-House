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
 * Final price details before payment.
 */
public class PriceDetailsPage {

    private final By detailsHint = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Price Detail\")");
    private final By detailsText = AppiumBy.androidUIAutomator(
            "new UiSelector().textContains(\"Price Detail\")");
    private final By paymentHint = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Payment\")");
    private final By totalHint = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Total\")");
    private final By breakdown = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Pay\")");
    private final By netPrice = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Net Price\")");
    private final By totalPrice = AppiumBy.androidUIAutomator(
            "new UiSelector().descriptionContains(\"Total Price\")");

    /** True only on the dedicated Price Details screen (not Price Summary / Pay CTAs). */
    public boolean isDisplayed() {
        return UiHelper.isAnyDisplayed(detailsHint, detailsText);
    }

    public List<String> getTotalOrBreakdownSnapshot() {
        List<String> snapshot = new ArrayList<>();
        List<By> locators = List.of(totalHint, breakdown, detailsHint, netPrice, totalPrice,
                AppiumBy.androidUIAutomator("new UiSelector().descriptionContains(\"Adult\")"),
                AppiumBy.androidUIAutomator("new UiSelector().textContains(\"Total\")"));

        for (By locator : locators) {
            List<WebElement> elements = DriverManager.getDriver().findElements(locator);
            for (WebElement el : elements) {
                try {
                    String desc = el.getAttribute("contentDescription");
                    if (desc != null && !desc.isBlank()) {
                        snapshot.add(desc.trim());
                    }
                    String text = el.getText();
                    if (text != null && !text.isBlank()) {
                        snapshot.add(text.trim());
                    }
                } catch (Exception ignored) {
                    // skip
                }
            }
        }
        return snapshot.stream().distinct().collect(Collectors.toList());
    }
}
