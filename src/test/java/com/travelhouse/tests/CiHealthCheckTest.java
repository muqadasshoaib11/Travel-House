package com.travelhouse.tests;

import com.travelhouse.base.DriverManager;
import com.travelhouse.config.CapabilityFactory;
import io.appium.java_client.android.AndroidDriver;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.List;

/**
 * Lightweight CI gate: proves the self-hosted runner can see the phone and open an Appium session.
 * Full dual-route booking remains on workflow_dispatch / dual-route-booking.xml.
 */
public class CiHealthCheckTest {

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        // no-op — startDriver is part of the assertion path
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        DriverManager.quitDriver();
    }

    @Test(description = "CI health: adb device + Appium status + AndroidDriver session")
    public void ci_deviceAndAppiumSession() throws Exception {
        List<String> devices = CapabilityFactory.listConnectedDevices();
        Assert.assertFalse(devices.isEmpty(), "Expected at least one adb device in 'device' state");
        System.out.println("[CI-Health] adb devices: " + devices);

        HttpURLConnection conn = (HttpURLConnection) URI.create("http://127.0.0.1:4723/status").toURL().openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        int code = conn.getResponseCode();
        Assert.assertEquals(code, 200, "Appium /status should return HTTP 200");
        System.out.println("[CI-Health] Appium /status HTTP " + code);

        DriverManager.startDriver();
        AndroidDriver driver = DriverManager.getDriver();
        Assert.assertNotNull(driver.getSessionId(), "Appium session id should be present");
        String pkg = driver.getCurrentPackage();
        System.out.println("[CI-Health] Session ok, package=" + pkg + ", session=" + driver.getSessionId());
        Assert.assertNotNull(pkg, "Current package should not be null after session start");
    }
}
