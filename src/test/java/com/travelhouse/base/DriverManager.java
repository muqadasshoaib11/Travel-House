package com.travelhouse.base;

import com.travelhouse.config.CapabilityFactory;
import com.travelhouse.config.ConfigReader;
import com.travelhouse.utils.DevicePrep;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.MalformedURLException;
import java.net.URI;
import java.time.Duration;

public final class DriverManager {

    private static final ThreadLocal<AndroidDriver> DRIVER = new ThreadLocal<>();
    private static final ThreadLocal<WebDriverWait> WAIT = new ThreadLocal<>();

    private DriverManager() {
    }

    public static void startDriver() {
        if (DRIVER.get() != null) {
            return;
        }
        DevicePrep.prepareForSession();
        UiAutomator2Options options = CapabilityFactory.createAndroidOptions();
        String serverUrl = ConfigReader.get("appium.server.url", "http://127.0.0.1:4723");
        try {
            AndroidDriver driver = new AndroidDriver(URI.create(serverUrl).toURL(), options);
            // Keep implicit wait low — Flutter trees + many findElements otherwise stall for minutes
            driver.manage().timeouts().implicitlyWait(
                    Duration.ofSeconds(ConfigReader.getInt("implicit.wait.seconds", 2)));
            DRIVER.set(driver);
            WAIT.set(new WebDriverWait(driver,
                    Duration.ofSeconds(ConfigReader.getInt("explicit.wait.seconds", 20))));
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid Appium server URL: " + serverUrl, e);
        }
    }

    public static AndroidDriver getDriver() {
        AndroidDriver driver = DRIVER.get();
        if (driver == null) {
            throw new IllegalStateException("Driver not started. Call DriverManager.startDriver() first.");
        }
        return driver;
    }

    public static WebDriverWait getWait() {
        WebDriverWait wait = WAIT.get();
        if (wait == null) {
            throw new IllegalStateException("Wait not initialized. Call DriverManager.startDriver() first.");
        }
        return wait;
    }

    public static void quitDriver() {
        AndroidDriver driver = DRIVER.get();
        if (driver != null) {
            try {
                driver.quit();
            } finally {
                DRIVER.remove();
                WAIT.remove();
            }
        }
    }
}
