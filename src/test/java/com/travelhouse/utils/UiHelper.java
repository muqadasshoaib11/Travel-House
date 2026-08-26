package com.travelhouse.utils;

import com.travelhouse.base.DriverManager;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.Collections;
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
        return findElementsNow(desc(accessibilityId));
    }

    /** Instant presence check — no timeout burn when the node is missing. */
    public static boolean isDescPresent(String fragment) {
        return !findElementsNow(descContains(fragment)).isEmpty();
    }

    public static boolean isDescExactPresent(String accessibilityId) {
        return !findElementsNow(desc(accessibilityId)).isEmpty();
    }

    public static boolean tapByDesc(String accessibilityId) {
        try {
            List<WebElement> elements = findByDesc(accessibilityId);
            if (!elements.isEmpty()) {
                elements.get(0).click();
                return true;
            }
        } catch (Exception ignored) {
            // Do not force-stop UiAutomator2 mid-session
        }
        return false;
    }

    public static boolean tapByDescContains(String fragment) {
        try {
            List<WebElement> elements = findElementsNow(descContains(fragment));
            if (!elements.isEmpty()) {
                elements.get(0).click();
                return true;
            }
        } catch (Exception ignored) {
            // Do not force-stop UiAutomator2 mid-session
        }
        return false;
    }

    public static boolean tapByTextContains(String fragment) {
        try {
            List<WebElement> elements = findElementsNow(textContains(fragment));
            if (!elements.isEmpty()) {
                elements.get(0).click();
                return true;
            }
        } catch (Exception ignored) {
            // Do not force-stop UiAutomator2 mid-session
        }
        return false;
    }

    public static void typeInEditText(String text) {
        AndroidDriver driver = DriverManager.getDriver();
        List<WebElement> fields = findElementsNow(AppiumBy.className("android.widget.EditText"));
        if (fields.isEmpty()) {
            throw new IllegalStateException("No EditText found to type into");
        }
        WebElement field = fields.get(0);
        field.click();
        field.clear();
        field.sendKeys(text);
    }

    /**
     * Wait until a description appears. Uses zero implicit wait so missing nodes
     * fail fast each poll instead of burning the driver implicit timeout.
     */
    public static boolean waitForDescContains(String fragment, int seconds) {
        long deadline = System.currentTimeMillis() + Math.max(0, seconds) * 1000L;
        do {
            if (isDescPresent(fragment)) {
                return true;
            }
            if (seconds <= 0) {
                return false;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        } while (System.currentTimeMillis() < deadline);
        return false;
    }

    public static boolean waitForDesc(String accessibilityId, int seconds) {
        long deadline = System.currentTimeMillis() + Math.max(0, seconds) * 1000L;
        do {
            if (isDescExactPresent(accessibilityId)) {
                return true;
            }
            if (seconds <= 0) {
                return false;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        } while (System.currentTimeMillis() < deadline);
        return false;
    }

    public static String getPageSourceSafe() {
        try {
            return DriverManager.getDriver().getPageSource();
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isAnyDisplayed(By... locators) {
        for (By locator : locators) {
            List<WebElement> found = findElementsNow(locator);
            try {
                if (!found.isEmpty() && found.get(0).isDisplayed()) {
                    return true;
                }
            } catch (Exception ignored) {
                // continue
            }
        }
        return false;
    }

    /** findElements with implicit wait forced to 0 — missing UI returns immediately. */
    public static List<WebElement> findElementsSafe(By locator) {
        return findElementsNow(locator);
    }

    public static List<WebElement> findElementsNow(By locator) {
        AndroidDriver driver = DriverManager.getDriver();
        Duration previous = driver.manage().timeouts().getImplicitWaitTimeout();
        try {
            driver.manage().timeouts().implicitlyWait(Duration.ZERO);
            return driver.findElements(locator);
        } catch (Exception e) {
            System.out.println("[UiHelper] findElements failed: " + e.getMessage());
            return Collections.emptyList();
        } finally {
            try {
                driver.manage().timeouts().implicitlyWait(previous);
            } catch (Exception ignored) {
                // ignore
            }
        }
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
