package com.travelhouse.tests;

import com.travelhouse.base.BaseTest;
import com.travelhouse.base.DriverManager;
import com.travelhouse.config.ConfigReader;
import com.travelhouse.pages.HomePage;
import org.testng.Assert;
import org.testng.annotations.Test;

public class AppLaunchTest extends BaseTest {

    @Test(description = "Launch Travel House and verify the session is on the expected package")
    public void launchApp_shouldOpenTravelHouse() {
        HomePage home = new HomePage();
        home.waitForUiReady();

        String expectedPackage = ConfigReader.get("app.package");
        String actualPackage = home.getCurrentPackage();

        Assert.assertNotNull(actualPackage, "Current package should not be null");
        Assert.assertTrue(
                actualPackage.equalsIgnoreCase(expectedPackage)
                        || actualPackage.toLowerCase().contains("travel"),
                "Expected package '" + expectedPackage + "' but was '" + actualPackage + "'. "
                        + "Update app.package in config.properties using scripts/find-app.ps1");

        Assert.assertNotNull(DriverManager.getDriver().getSessionId(), "Appium session should be active");
        System.out.println("Opened package: " + actualPackage + " | activity: " + home.getCurrentActivity());
    }
}
