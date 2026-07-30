package com.travelhouse.utils;

import com.travelhouse.base.DriverManager;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;

/**
 * Dismiss Android runtime permission / post-login dialogs that block the app.
 */
public final class PermissionDialog {

    private static final String[] ALLOW_LABELS = {
            "Allow",
            "While using the app",
            "Allow only while using the app",
            "Only this time",
            "OK",
            "Got it"
    };

    private PermissionDialog() {
    }

    public static void dismissAll(int maxRounds) {
        for (int i = 0; i < maxRounds; i++) {
            if (!dismissOnce()) {
                break;
            }
            sleep(800);
        }
    }

    public static boolean dismissOnce() {
        Boolean handled = withZeroImplicitWait(() -> {
            String[] ids = {
                    "com.android.permissioncontroller:id/permission_allow_button",
                    "com.android.permissioncontroller:id/permission_allow_foreground_only_button",
                    "com.android.permissioncontroller:id/permission_allow_one_time_button",
                    "com.android.permissioncontroller:id/permission_allow_always_button",
                    "com.android.packageinstaller:id/permission_allow_button"
            };
            for (String id : ids) {
                try {
                    List<WebElement> buttons = DriverManager.getDriver().findElements(AppiumBy.id(id));
                    if (!buttons.isEmpty() && buttons.get(0).isDisplayed()) {
                        System.out.println("[Permission] Allow via id: " + id);
                        buttons.get(0).click();
                        return true;
                    }
                } catch (Exception e) {
                    System.out.println("[Permission] id lookup failed: " + e.getMessage());
                    DevicePrep.restartUiAutomator2();
                    return false;
                }
            }

            for (String label : ALLOW_LABELS) {
                if (tapExact(label)) {
                    System.out.println("[Permission] Allow via text: " + label);
                    return true;
                }
            }
            return false;
        });
        return Boolean.TRUE.equals(handled);
    }

    /**
     * After login: Allow notifications; Cancel biometric opt-in.
     * Also safe to call before login for notification popups.
     */
    public static void handlePostLoginDialogs() {
        for (int i = 0; i < 6; i++) {
            boolean handled = false;

            try {
                if (dismissOnce()) {
                    handled = true;
                }
            } catch (Exception e) {
                System.out.println("[Permission] dismissOnce failed: " + e.getMessage());
                DevicePrep.restartUiAutomator2();
                break;
            }

            try {
                if (isBiometricPrompt()) {
                    System.out.println("[Permission] Biometric prompt — Cancel");
                    if (tapExact("Cancel") || tapExact("No") || tapExact("Not now") || tapExact("Skip")) {
                        handled = true;
                    }
                }
            } catch (Exception e) {
                System.out.println("[Permission] biometric handling failed: " + e.getMessage());
                DevicePrep.restartUiAutomator2();
                break;
            }

            if (!handled) {
                break;
            }
            sleep(1000);
        }
    }

    private static boolean isBiometricPrompt() {
        return Boolean.TRUE.equals(withZeroImplicitWait(() ->
                !safeFind(AppiumBy.androidUIAutomator(
                        "new UiSelector().descriptionContains(\"biometric\")")).isEmpty()
                        || !safeFind(AppiumBy.androidUIAutomator(
                        "new UiSelector().textContains(\"biometric\")")).isEmpty()
                        || !safeFind(AppiumBy.androidUIAutomator(
                        "new UiSelector().textContains(\"Biometric\")")).isEmpty()
                        || !safeFind(AppiumBy.accessibilityId(
                        "Do you want to enable biometric authentication?")).isEmpty()));
    }

    private static boolean tapExact(String label) {
        return Boolean.TRUE.equals(withZeroImplicitWait(() -> {
            List<WebElement> byText = safeFind(
                    AppiumBy.androidUIAutomator("new UiSelector().text(\"" + label + "\")"));
            if (!byText.isEmpty() && byText.get(0).isDisplayed()) {
                byText.get(0).click();
                return true;
            }
            List<WebElement> byDesc = safeFind(AppiumBy.accessibilityId(label));
            if (!byDesc.isEmpty() && byDesc.get(0).isDisplayed()) {
                byDesc.get(0).click();
                return true;
            }
            return false;
        }));
    }

    private static List<WebElement> safeFind(org.openqa.selenium.By by) {
        try {
            return DriverManager.getDriver().findElements(by);
        } catch (Exception e) {
            System.out.println("[Permission] findElements failed: " + e.getMessage());
            DevicePrep.restartUiAutomator2();
            return List.of();
        }
    }

    private static <T> T withZeroImplicitWait(java.util.concurrent.Callable<T> action) {
        var driver = DriverManager.getDriver();
        Duration previous = Duration.ofSeconds(2);
        try {
            previous = driver.manage().timeouts().getImplicitWaitTimeout();
            driver.manage().timeouts().implicitlyWait(Duration.ZERO);
            return action.call();
        } catch (Exception e) {
            System.out.println("[Permission] action failed: " + e.getMessage());
            DevicePrep.restartUiAutomator2();
            return null;
        } finally {
            try {
                driver.manage().timeouts().implicitlyWait(previous);
            } catch (Exception ignored) {
                // session may have died
            }
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
