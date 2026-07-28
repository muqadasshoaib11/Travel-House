package com.travelhouse.utils;

import com.travelhouse.base.DriverManager;
import io.appium.java_client.AppiumBy;
import org.openqa.selenium.WebElement;

import java.util.List;

/**
 * Dismiss Android runtime permission / post-login dialogs that block the app.
 */
public final class PermissionDialog {

    private static final String[] ALLOW_LABELS = {
            "Allow",
            "ALLOW",
            "While using the app",
            "Allow only while using the app",
            "Allow all the time",
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
        String[] ids = {
                "com.android.permissioncontroller:id/permission_allow_button",
                "com.android.permissioncontroller:id/permission_allow_foreground_only_button",
                "com.android.permissioncontroller:id/permission_allow_one_time_button",
                "com.android.permissioncontroller:id/permission_allow_always_button",
                "com.android.packageinstaller:id/permission_allow_button"
        };
        for (String id : ids) {
            List<WebElement> buttons = DriverManager.getDriver().findElements(AppiumBy.id(id));
            if (!buttons.isEmpty() && buttons.get(0).isDisplayed()) {
                System.out.println("[Permission] Allow via id: " + id);
                buttons.get(0).click();
                return true;
            }
        }

        for (String label : ALLOW_LABELS) {
            if (tapExact(label)) {
                System.out.println("[Permission] Allow via text: " + label);
                return true;
            }
        }
        return false;
    }

    /**
     * After login: Allow notifications; Cancel biometric opt-in.
     * Also safe to call before login for notification popups.
     */
    public static void handlePostLoginDialogs() {
        for (int i = 0; i < 8; i++) {
            boolean handled = false;

            if (dismissOnce()) {
                handled = true;
            }

            if (isBiometricPrompt()) {
                System.out.println("[Permission] Biometric prompt — Cancel");
                if (tapExact("Cancel") || tapExact("No") || tapExact("Not now") || tapExact("Skip")) {
                    handled = true;
                }
            }

            if (!handled) {
                break;
            }
            sleep(1000);
        }
    }

    private static boolean isBiometricPrompt() {
        return !DriverManager.getDriver().findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().descriptionContains(\"biometric\")")).isEmpty()
                || !DriverManager.getDriver().findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().textContains(\"biometric\")")).isEmpty()
                || !DriverManager.getDriver().findElements(AppiumBy.androidUIAutomator(
                "new UiSelector().textContains(\"Biometric\")")).isEmpty()
                || !DriverManager.getDriver().findElements(AppiumBy.accessibilityId(
                "Do you want to enable biometric authentication?")).isEmpty();
    }

    private static boolean tapExact(String label) {
        List<WebElement> byText = DriverManager.getDriver().findElements(
                AppiumBy.androidUIAutomator("new UiSelector().text(\"" + label + "\")"));
        if (!byText.isEmpty() && byText.get(0).isDisplayed()) {
            byText.get(0).click();
            return true;
        }
        List<WebElement> byDesc = DriverManager.getDriver().findElements(AppiumBy.accessibilityId(label));
        if (!byDesc.isEmpty() && byDesc.get(0).isDisplayed()) {
            byDesc.get(0).click();
            return true;
        }
        return false;
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
