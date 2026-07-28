package com.travelhouse.base;

import com.travelhouse.utils.PermissionDialog;
import com.travelhouse.utils.ScreenshotUtil;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;

/**
 * Single Appium session for multi-step journey tests (dependsOnMethods chain).
 * Smoke tests should continue using {@link BaseTest}.
 */
public abstract class JourneyBaseTest {

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        ScreenshotUtil.ensureDirs();
    }

    @BeforeClass(alwaysRun = true)
    public void startJourneySession() {
        DriverManager.startDriver();
        // Notification permission may appear before login
        PermissionDialog.dismissAll(8);
    }

    @AfterMethod(alwaysRun = true)
    public void afterJourneyMethod(ITestResult result) {
        if (!result.isSuccess()) {
            ScreenshotUtil.capture(result.getMethod().getMethodName());
        }
        // Keep driver alive for dependsOnMethods / priority chain
    }

    @AfterClass(alwaysRun = true)
    public void endJourneySession() {
        DriverManager.quitDriver();
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        DriverManager.quitDriver();
    }
}
