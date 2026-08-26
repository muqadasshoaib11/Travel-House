package com.travelhouse.pages;

import com.travelhouse.base.DriverManager;
import com.travelhouse.utils.ExtentReportManager;
import com.travelhouse.utils.GestureUtil;
import com.travelhouse.utils.UiHelper;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.Rectangle;
import org.openqa.selenium.WebElement;
import org.testng.Assert;

import java.util.List;

/**
 * Full Payment path from search results: Cheapest/Fastest → red Pay £ →
 * Proceed with payment → Terms → My Travellers
 * (matches Playwright full-payment.page.js from Travel-House-Complete.zip).
 */
public class FullPaymentPage {

    private final AndroidDriver driver;

    public FullPaymentPage() {
        this.driver = DriverManager.getDriver();
    }

    public void selectFareAndPay(String fareTab) {
        Assert.assertTrue(UiHelper.tapByDesc(fareTab) || UiHelper.tapByDescContains(fareTab),
                "Fare tab not found: " + fareTab);

        for (int attempt = 1; attempt <= 2; attempt++) {
            List<WebElement> pays = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().descriptionContains(\"Pay £\")"));
            Assert.assertFalse(pays.isEmpty(), "Pay £ CTA not found (attempt " + attempt + ")");
            WebElement pay = pays.get(0);
            Rectangle r = pay.getRect();
            if (r.height < 250) {
                // One Way: Pay is its own semantics node
                tapCenter(pay);
            } else {
                // Return: whole card; Pay is lower-right
                GestureUtil.tapAt((int) Math.round(r.x + r.width * 0.82),
                        (int) Math.round(r.y + r.height * 0.9));
            }
            if (UiHelper.waitForDesc("Proceed with payment", 10)
                    || UiHelper.waitForDescContains("Proceed with payment", 5)) {
                UiHelper.tapByDesc("Proceed with payment");
                UiHelper.tapByDescContains("Proceed with payment");
                ExtentReportManager.logInfo("Selected " + fareTab + " + Pay £ → Proceed with payment");
                return;
            }
        }
        throw new IllegalStateException("The red full-payment Pay button did not open fare details");
    }

    public void acceptTermsAndContinue() {
        List<WebElement> labels = driver.findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"I accept\")"));
        Assert.assertFalse(labels.isEmpty(), "Full-payment Terms label not found");
        Rectangle r = labels.get(0).getRect();
        GestureUtil.tapAt(Math.max(20, r.x - 45), r.y + r.height / 2);

        long deadline = System.currentTimeMillis() + 15_000L;
        boolean tapped = false;
        while (System.currentTimeMillis() < deadline && !tapped) {
            List<WebElement> continues = driver.findElements(AppiumBy.accessibilityId("Continue"));
            if (!continues.isEmpty()) {
                WebElement btn = continues.get(0);
                try {
                    if ("true".equalsIgnoreCase(String.valueOf(btn.getAttribute("clickable")))) {
                        tapCenter(btn);
                        tapped = true;
                        break;
                    }
                } catch (Exception ignored) {
                    // retry
                }
            }
        }
        if (!tapped) {
            UiHelper.tapByDesc("Continue");
            UiHelper.tapByDescContains("Continue");
        }
        Assert.assertTrue(
                UiHelper.waitForDescContains("Please Select a Saved Traveller", 60)
                        || UiHelper.waitForDescContains("Who's Going", 10)
                        || UiHelper.waitForDescContains("My Travellers", 10),
                "My Travellers should appear after Full Payment Terms Continue");
        ExtentReportManager.logInfo("Accepted Full Payment terms → My Travellers");
    }

    private void tapCenter(WebElement el) {
        Rectangle r = el.getRect();
        GestureUtil.tapAt(r.x + Math.max(1, r.width / 2), r.y + Math.max(1, r.height / 2));
    }
}
