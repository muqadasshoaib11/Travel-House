package com.travelhouse.pages;

import com.travelhouse.base.DriverManager;
import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.Keys;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.interactions.PointerInput;
import org.openqa.selenium.interactions.Sequence;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

public class LoginPage {

    private final AndroidDriver driver;

    private final By emailField = AppiumBy.accessibilityId("Email");
    private final By passwordField = AppiumBy.accessibilityId("Password");
    private final By loginButton = AppiumBy.accessibilityId("Log In");
    private final By forgetPassword = AppiumBy.accessibilityId("Forget Password?");

    public LoginPage() {
        this.driver = DriverManager.getDriver();
    }

    public boolean isLoginScreenVisible() {
        return !driver.findElements(loginButton).isEmpty()
                || !driver.findElements(emailField).isEmpty()
                || !driver.findElements(forgetPassword).isEmpty();
    }

    public void waitUntilVisible() {
        DriverManager.getWait().until(ExpectedConditions.presenceOfElementLocated(loginButton));
    }

    public void enterEmail(String email) {
        typeIntoFlutterField(emailField, email, 0);
    }

    public void enterPassword(String password) {
        typeIntoFlutterField(passwordField, password, 1);
    }

    public void tapLogin() {
        try {
            driver.hideKeyboard();
        } catch (Exception ignored) {
            // keyboard may already be hidden
        }
        WebElement button = DriverManager.getWait()
                .until(ExpectedConditions.presenceOfElementLocated(loginButton));
        tapCenter(button);
    }

    public void login(String email, String password) {
        enterEmail(email);
        enterPassword(password);
        tapLogin();
    }

    public boolean isStillOnLogin() {
        List<WebElement> buttons = driver.findElements(loginButton);
        return !buttons.isEmpty() && buttons.get(0).isDisplayed();
    }

    private void typeIntoFlutterField(By labelLocator, String value, int editTextIndex) {
        List<WebElement> labeled = driver.findElements(labelLocator);
        if (!labeled.isEmpty()) {
            tapCenter(labeled.get(0));
        }

        List<WebElement> fields = driver.findElements(AppiumBy.className("android.widget.EditText"));
        WebElement field;
        if (editTextIndex >= 0 && editTextIndex < fields.size()) {
            field = fields.get(editTextIndex);
        } else if (!fields.isEmpty()) {
            field = fields.get(0);
        } else if (!labeled.isEmpty()) {
            field = labeled.get(0);
        } else {
            throw new IllegalStateException("Login input field not found");
        }

        field.click();
        try {
            field.clear();
        } catch (Exception ignored) {
            // Flutter clear is unreliable — select-all then overwrite
            try {
                field.sendKeys(Keys.CONTROL + "a");
            } catch (Exception ignored2) {
                // ignore
            }
        }
        field.sendKeys(value);
    }

    private void tapCenter(WebElement element) {
        var rect = element.getRect();
        int x = rect.x + rect.width / 2;
        int y = rect.y + rect.height / 2;
        PointerInput finger = new PointerInput(PointerInput.Kind.TOUCH, "finger");
        Sequence tap = new Sequence(finger, 1);
        tap.addAction(finger.createPointerMove(Duration.ZERO, PointerInput.Origin.viewport(), x, y));
        tap.addAction(finger.createPointerDown(PointerInput.MouseButton.LEFT.asArg()));
        tap.addAction(finger.createPointerUp(PointerInput.MouseButton.LEFT.asArg()));
        driver.perform(Collections.singletonList(tap));
    }
}
