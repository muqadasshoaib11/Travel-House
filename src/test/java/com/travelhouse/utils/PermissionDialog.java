package com.travelhouse.utils;

import com.travelhouse.base.DriverManager;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * Dismiss Android runtime permission / post-login dialogs that block the app.
 * Keep Appium lookups minimal — UiAutomator findElements can hang when the server freezes.
 */
public final class PermissionDialog {

    private PermissionDialog() {
    }

    public static void dismissAll(int maxRounds) {
        for (int i = 0; i < maxRounds; i++) {
            if (!dismissOnce()) {
                break;
            }
            sleep(600);
        }
    }

    public static boolean dismissOnce() {
        var driver = DriverManager.getDriver();
        Duration previous = Duration.ofSeconds(2);
        try {
            previous = driver.manage().timeouts().getImplicitWaitTimeout();
            driver.manage().timeouts().implicitlyWait(Duration.ZERO);
            String[] ids = {
                    "com.android.permissioncontroller:id/permission_allow_button",
                    "com.android.permissioncontroller:id/permission_allow_foreground_only_button",
                    "com.android.permissioncontroller:id/permission_allow_one_time_button",
                    "com.android.permissioncontroller:id/permission_allow_always_button",
                    "com.android.packageinstaller:id/permission_allow_button"
            };
            for (String id : ids) {
                List<WebElement> buttons = driver.findElements(AppiumBy.id(id));
                if (!buttons.isEmpty() && buttons.get(0).isDisplayed()) {
                    System.out.println("[Permission] Allow via id: " + id);
                    buttons.get(0).click();
                    return true;
                }
            }
            // Prefer text Allow once — avoid scanning many labels (each can hang if UiAutomator freezes)
            List<WebElement> allow = driver.findElements(AppiumBy.androidUIAutomator(
                    "new UiSelector().text(\"Allow\")"));
            if (!allow.isEmpty() && allow.get(0).isDisplayed()) {
                allow.get(0).click();
                return true;
            }
            return false;
        } catch (Exception e) {
            System.out.println("[Permission] dismissOnce failed: " + e.getMessage());
            DevicePrep.restartUiAutomator2();
            return false;
        } finally {
            try {
                driver.manage().timeouts().implicitlyWait(previous);
            } catch (Exception ignored) {
                // session may have died
            }
        }
    }

    /**
     * After login: Allow notifications; Cancel biometric opt-in.
     */
    public static void handlePostLoginDialogs() {
        for (int i = 0; i < 4; i++) {
            boolean handled = false;
            try {
                if (dismissOnce()) {
                    handled = true;
                }
            } catch (Exception e) {
                DevicePrep.restartUiAutomator2();
                break;
            }

            try {
                var driver = DriverManager.getDriver();
                Duration previous = driver.manage().timeouts().getImplicitWaitTimeout();
                driver.manage().timeouts().implicitlyWait(Duration.ZERO);
                try {
                    List<WebElement> cancel = driver.findElements(AppiumBy.androidUIAutomator(
                            "new UiSelector().text(\"Cancel\")"));
                    if (cancel.isEmpty()) {
                        cancel = driver.findElements(AppiumBy.accessibilityId("Cancel"));
                    }
                    if (!cancel.isEmpty() && cancel.get(0).isDisplayed()) {
                        System.out.println("[Permission] Tapping Cancel");
                        cancel.get(0).click();
                        handled = true;
                    }
                } finally {
                    driver.manage().timeouts().implicitlyWait(previous);
                }
            } catch (Exception e) {
                System.out.println("[Permission] Cancel handling failed: " + e.getMessage());
                DevicePrep.restartUiAutomator2();
                // Fall back to system Back (often dismisses biometric sheet)
                adbBack();
                break;
            }

            if (!handled) {
                break;
            }
            sleep(800);
        }
    }

    private static void adbBack() {
        try {
            String udid = com.travelhouse.config.ConfigReader.get("device.udid");
            ProcessBuilder pb = (udid == null || udid.isBlank())
                    ? new ProcessBuilder("adb", "shell", "input", "keyevent", "4")
                    : new ProcessBuilder("adb", "-s", udid, "shell", "input", "keyevent", "4");
            pb.redirectErrorStream(true).start().waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (Exception ignored) {
            // ignore
        }
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
