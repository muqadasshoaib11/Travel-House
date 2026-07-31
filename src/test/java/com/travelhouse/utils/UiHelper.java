package com.travelhouse.utils;

import com.travelhouse.base.DriverManager;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public final class UiHelper {

    private static final int FIND_TIMEOUT_SECONDS = 12;

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
        return findElementsSafe(desc(accessibilityId));
    }

    public static boolean tapByDesc(String accessibilityId) {
        try {
            List<WebElement> elements = findByDesc(accessibilityId);
            if (!elements.isEmpty()) {
                elements.get(0).click();
                return true;
            }
        } catch (Exception e) {
            DevicePrep.restartUiAutomator2();
        }
        return false;
    }

    public static boolean tapByDescContains(String fragment) {
        try {
            List<WebElement> elements = findElementsSafe(descContains(fragment));
            if (!elements.isEmpty()) {
                elements.get(0).click();
                return true;
            }
        } catch (Exception e) {
            DevicePrep.restartUiAutomator2();
        }
        return false;
    }

    public static boolean tapByTextContains(String fragment) {
        try {
            List<WebElement> elements = findElementsSafe(textContains(fragment));
            if (!elements.isEmpty()) {
                elements.get(0).click();
                return true;
            }
        } catch (Exception e) {
            DevicePrep.restartUiAutomator2();
        }
        return false;
    }

    public static void typeInEditText(String text) {
        AndroidDriver driver = DriverManager.getDriver();
        List<WebElement> fields = findElementsSafe(AppiumBy.className("android.widget.EditText"));
        if (fields.isEmpty()) {
            throw new IllegalStateException("No EditText found to type into");
        }
        WebElement field = fields.get(0);
        field.click();
        field.clear();
        field.sendKeys(text);
    }

    public static boolean waitForDescContains(String fragment, int seconds) {
        long deadline = System.currentTimeMillis() + Math.max(1, seconds) * 1000L;
        while (System.currentTimeMillis() < deadline) {
            List<WebElement> found = findElementsSafe(descContains(fragment));
            try {
                if (!found.isEmpty() && found.get(0).isDisplayed()) {
                    return true;
                }
            } catch (Exception ignored) {
                // stale element
            }
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    public static boolean waitForDesc(String accessibilityId, int seconds) {
        long deadline = System.currentTimeMillis() + Math.max(1, seconds) * 1000L;
        while (System.currentTimeMillis() < deadline) {
            List<WebElement> found = findElementsSafe(desc(accessibilityId));
            try {
                if (!found.isEmpty() && found.get(0).isDisplayed()) {
                    return true;
                }
            } catch (Exception ignored) {
                // stale
            }
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            }
        }
        return false;
    }

    public static String getPageSourceSafe() {
        try {
            return callWithTimeout(15, () -> DriverManager.getDriver().getPageSource());
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isAnyDisplayed(By... locators) {
        for (By locator : locators) {
            List<WebElement> found = findElementsSafe(locator);
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

    public static List<WebElement> findElementsSafe(By locator) {
        try {
            return callWithTimeout(FIND_TIMEOUT_SECONDS,
                    () -> DriverManager.getDriver().findElements(locator));
        } catch (TimeoutException e) {
            System.out.println("[UiHelper] findElements timed out for " + locator + " — recycling UiAutomator2");
            DevicePrep.restartUiAutomator2();
            return Collections.emptyList();
        } catch (Exception e) {
            System.out.println("[UiHelper] findElements failed: " + e.getMessage());
            DevicePrep.restartUiAutomator2();
            return Collections.emptyList();
        }
    }

    private static <T> T callWithTimeout(int seconds, Callable<T> action) throws Exception {
        ExecutorService pool = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "uihelper-timeout");
            t.setDaemon(true);
            return t;
        });
        Future<T> future = pool.submit(action);
        try {
            return future.get(seconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw e;
        } finally {
            pool.shutdownNow();
        }
    }

    private static String escape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
