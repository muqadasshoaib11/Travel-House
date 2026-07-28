package com.travelhouse.utils;

import com.travelhouse.base.DriverManager;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public final class UiHelper {

    private UiHelper() {
    }

    public static By desc(String accessibilityId) {
        return AppiumBy.accessibilityId(accessibilityId);
    }

    public static By descContains(String fragment) {
        return AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"" + escape(fragment) + "\")");
    }

    public static By textContains(String fragment) {
        return AppiumBy.androidUIAutomator(
                "new UiSelector().textContains(\"" + escape(fragment) + "\")");
    }

    public static List<WebElement> findByDesc(String accessibilityId) {
        return DriverManager.getDriver().findElements(desc(accessibilityId));
    }

    public static boolean tapByDesc(String accessibilityId) {
        try {
            List<WebElement> elements = findByDesc(accessibilityId);
            if (!elements.isEmpty()) {
                elements.get(0).click();
                return true;
            }
        } catch (Exception e) {
            try {
                List<WebElement> elements = findByDesc(accessibilityId);
                if (!elements.isEmpty()) {
                    elements.get(0).click();
                    return true;
                }
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }

    public static boolean tapByDescContains(String fragment) {
        try {
            List<WebElement> elements = DriverManager.getDriver().findElements(descContains(fragment));
            if (!elements.isEmpty()) {
                elements.get(0).click();
                return true;
            }
        } catch (Exception e) {
            try {
                List<WebElement> elements = DriverManager.getDriver().findElements(descContains(fragment));
                if (!elements.isEmpty()) {
                    elements.get(0).click();
                    return true;
                }
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }

    public static boolean tapByTextContains(String fragment) {
        try {
            List<WebElement> elements = DriverManager.getDriver().findElements(textContains(fragment));
            if (!elements.isEmpty()) {
                elements.get(0).click();
                return true;
            }
        } catch (Exception e) {
            try {
                List<WebElement> elements = DriverManager.getDriver().findElements(textContains(fragment));
                if (!elements.isEmpty()) {
                    elements.get(0).click();
                    return true;
                }
            } catch (Exception ignored) {
                return false;
            }
        }
        return false;
    }

    public static void typeInEditText(String text) {
        AndroidDriver driver = DriverManager.getDriver();
        List<WebElement> fields = driver.findElements(AppiumBy.className("android.widget.EditText"));
        if (fields.isEmpty()) {
            throw new IllegalStateException("No EditText found to type into");
        }
        WebElement field = fields.get(0);
        field.click();
        field.clear();
        field.sendKeys(text);
    }

    public static boolean waitForDescContains(String fragment, int seconds) {
        try {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(seconds));
            wait.until(d -> {
                List<WebElement> found = d.findElements(descContains(fragment));
                return !found.isEmpty() && found.get(0).isDisplayed();
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static boolean waitForDesc(String accessibilityId, int seconds) {
        try {
            WebDriverWait wait = new WebDriverWait(DriverManager.getDriver(), Duration.ofSeconds(seconds));
            wait.until(d -> {
                List<WebElement> found = d.findElements(desc(accessibilityId));
                return !found.isEmpty() && found.get(0).isDisplayed();
            });
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public static String getPageSourceSafe() {
        try {
            return DriverManager.getDriver().getPageSource();
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isAnyDisplayed(By... locators) {
        AndroidDriver driver = DriverManager.getDriver();
        for (By locator : locators) {
            List<WebElement> found = driver.findElements(locator);
            if (!found.isEmpty() && found.get(0).isDisplayed()) {
                return true;
            }
        }
        return false;
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
