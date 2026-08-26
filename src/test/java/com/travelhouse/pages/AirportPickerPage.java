package com.travelhouse.pages;

import com.travelhouse.base.DriverManager;
import com.travelhouse.utils.UiHelper;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.testng.Assert;

import java.util.List;

/**
 * Airport / city picker — EditText search + result rows via content-desc.
 */
public class AirportPickerPage {

    private final AndroidDriver driver;

    public AirportPickerPage() {
        this.driver = DriverManager.getDriver();
    }

    public void searchAndSelect(String query) {
        DriverManager.getWait().until(ExpectedConditions.presenceOfElementLocated(
                AppiumBy.className("android.widget.EditText")));
        UiHelper.typeInEditText(query);

        // Wait for results that mention the query (e.g. Islamabad / ISB)
        boolean found = UiHelper.waitForDescContains(query, 15);
        if (!found) {
            // Fallback: any clickable result under the list
            found = UiHelper.waitForDescContains("Airport", 5)
                    || UiHelper.waitForDescContains("International", 3);
        }

        List<WebElement> matches = driver.findElements(UiHelper.descContains(query));
        if (matches.isEmpty()) {
            matches = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().clickable(true).descriptionContains(\""
                            + query.replace("\"", "") + "\")"));
        }
        if (matches.isEmpty()) {
            throw new IllegalStateException("No airport result matching: " + query);
        }
        matches.get(0).click();
    }

    /**
     * Playwright full-payment flow: type city, tap result {@code IATA – City},
     * then confirm Home shows {@code City – IATA}.
     */
    public void searchAndSelectByIata(String city, String iata) {
        DriverManager.getWait().until(ExpectedConditions.presenceOfElementLocated(
                AppiumBy.className("android.widget.EditText")));
        List<WebElement> fields = driver.findElements(AppiumBy.className("android.widget.EditText"));
        Assert.assertFalse(fields.isEmpty(), "Airport search field missing");
        WebElement search = fields.get(0);
        search.click();
        try {
            search.clear();
        } catch (Exception ignored) {
            // Flutter
        }
        search.sendKeys(city);

        String resultHint = iata + " – " + city;
        Assert.assertTrue(UiHelper.waitForDescContains(resultHint, 30)
                        || UiHelper.waitForDescContains(iata, 10),
                "Airport result not found for " + resultHint);
        if (!UiHelper.tapByDescContains(resultHint)) {
            UiHelper.tapByDescContains(iata);
        }
        try {
            driver.hideKeyboard();
        } catch (Exception ignored) {
            // ignore
        }
        String selectedHint = city + " – " + iata;
        Assert.assertTrue(UiHelper.waitForDescContains(selectedHint, 15)
                        || UiHelper.waitForDescContains(city, 8),
                "Home should show selected destination " + selectedHint);
    }
}
