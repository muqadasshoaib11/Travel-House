package com.travelhouse.base;

import com.travelhouse.config.CapabilityFactory;
import com.travelhouse.config.ConfigReader;
import com.travelhouse.utils.DevicePrep;
import io.appium.java_client.AppiumClientConfig;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.net.URI;
import java.time.Duration;

/**
 * Single shared Android driver for this suite.
 * Not ThreadLocal — TestNG {@code timeOut} runs the test body on a worker thread.
 */
public final class DriverManager {

    private static volatile AndroidDriver driver;
    private static volatile WebDriverWait wait;

    private DriverManager() {
    }

    public static synchronized void startDriver() {
        if (driver != null) {
            return;
        }
        DevicePrep.prepareForSession();
        UiAutomator2Options options = CapabilityFactory.createAndroidOptions();
        String serverUrl = ConfigReader.get("appium.server.url", "http://127.0.0.1:4723");
        int readTimeoutSec = ConfigReader.getInt("appium.read.timeout.seconds", 45);
        AppiumClientConfig clientConfig = AppiumClientConfig.defaultConfig()
                .baseUri(URI.create(serverUrl))
                .readTimeout(Duration.ofSeconds(readTimeoutSec))
                .connectionTimeout(Duration.ofSeconds(30));
        AndroidDriver created = new AndroidDriver(clientConfig, options);
        created.manage().timeouts().implicitlyWait(Duration.ZERO);
        driver = created;
        wait = new WebDriverWait(created,
                Duration.ofSeconds(ConfigReader.getInt("explicit.wait.seconds", 20)));
    }

    public static AndroidDriver getDriver() {
        AndroidDriver current = driver;
        if (current == null) {
            throw new IllegalStateException("Driver not started. Call DriverManager.startDriver() first.");
        }
        return current;
    }

    public static WebDriverWait getWait() {
        WebDriverWait current = wait;
        if (current == null) {
            throw new IllegalStateException("Wait not initialized. Call DriverManager.startDriver() first.");
        }
        return current;
    }

    public static synchronized void quitDriver() {
        AndroidDriver current = driver;
        if (current != null) {
            try {
                current.quit();
            } finally {
                driver = null;
                wait = null;
            }
        }
    }
}
