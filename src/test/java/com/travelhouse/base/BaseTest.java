package com.travelhouse.base;

import com.travelhouse.utils.ScreenshotUtil;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeSuite;

public abstract class BaseTest {

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() {
        ScreenshotUtil.ensureDirs();
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp() {
        DriverManager.startDriver();
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown(ITestResult result) {
        if (!result.isSuccess()) {
            ScreenshotUtil.capture(result.getMethod().getMethodName());
        }
        DriverManager.quitDriver();
    }

    @AfterSuite(alwaysRun = true)
    public void afterSuite() {
        DriverManager.quitDriver();
    }
}
