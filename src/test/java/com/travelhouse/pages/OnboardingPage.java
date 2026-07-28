package com.travelhouse.pages;

import com.travelhouse.base.DriverManager;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
import java.util.List;

public class OnboardingPage {

    private final AndroidDriver driver;
    private final By skip = AppiumBy.accessibilityId("Skip");
    private final By next = AppiumBy.accessibilityId("Next");
    private final By loginButton = AppiumBy.accessibilityId("Log In");
    private final By emailField = AppiumBy.accessibilityId("Email");

    public OnboardingPage() {
        this.driver = DriverManager.getDriver();
    }

    public boolean isVisible() {
        return !driver.findElements(skip).isEmpty() || !driver.findElements(next).isEmpty();
    }

    public void dismissToLogin() {
        WebDriverWait shortWait = new WebDriverWait(driver, Duration.ofSeconds(8));

        for (int attempt = 0; attempt < 6; attempt++) {
            if (isLoginVisible()) {
                return;
            }

            List<WebElement> skipButtons = driver.findElements(skip);
            if (!skipButtons.isEmpty() && skipButtons.get(0).isDisplayed()) {
                System.out.println("[Onboarding] Tap Skip (attempt " + (attempt + 1) + ")");
                skipButtons.get(0).click();
            } else {
                List<WebElement> nextButtons = driver.findElements(next);
                if (!nextButtons.isEmpty() && nextButtons.get(0).isDisplayed()) {
                    System.out.println("[Onboarding] Tap Next (attempt " + (attempt + 1) + ")");
                    nextButtons.get(0).click();
                }
            }

            try {
                shortWait.until(d -> isLoginVisible() || stillOnOnboarding());
            } catch (Exception ignored) {
                // continue retrying
            }

            if (isLoginVisible()) {
                return;
            }
        }

        if (!isLoginVisible()) {
            throw new IllegalStateException(
                    "Could not reach login screen from onboarding. Page source snippet may help debug.");
        }
    }

    private boolean isLoginVisible() {
        return !driver.findElements(loginButton).isEmpty() || !driver.findElements(emailField).isEmpty();
    }

    private boolean stillOnOnboarding() {
        return isVisible();
    }
}
