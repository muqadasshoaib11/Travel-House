package com.travelhouse.utils;

import org.testng.ITestContext;
import org.testng.ITestListener;
import org.testng.ITestResult;

public class TestListener implements ITestListener {

    @Override
    public void onStart(ITestContext context) {
        ExtentReportManager.getInstance();
        HtmlSummaryReport.setSuiteName(context.getSuite().getName() + " / " + context.getName());
        System.out.println("[SUITE] Starting: " + context.getName());
    }

    @Override
    public void onTestStart(ITestResult result) {
        String name = result.getMethod().getMethodName();
        String description = result.getMethod().getDescription();
        ExtentReportManager.startTest(name, description);
        ExtentReportManager.logInfo("Started: " + name);
        System.out.println("[START] " + name);
    }

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentReportManager.logPass("Passed: " + result.getMethod().getMethodName());
        HtmlSummaryReport.record(result);
        ExtentReportManager.clearTest();
        System.out.println("[PASS ] " + result.getMethod().getMethodName());
    }

    @Override
    public void onTestFailure(ITestResult result) {
        String name = result.getMethod().getMethodName();
        String message = result.getThrowable() != null ? result.getThrowable().getMessage() : "Unknown failure";
        System.out.println("[FAIL ] " + name + " -> " + message);

        String screenshotPath = ScreenshotUtil.capture(name);
        ExtentReportManager.logFail(message);
        if (screenshotPath != null) {
            ExtentReportManager.attachScreenshot(screenshotPath);
            ExtentReportManager.logInfo("Screenshot: " + screenshotPath);
        }
        HtmlSummaryReport.record(result);
        ExtentReportManager.clearTest();
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        if (ExtentReportManager.getTest() != null) {
            ExtentReportManager.getTest().skip(
                    result.getThrowable() != null ? result.getThrowable().getMessage() : "Skipped");
        }
        HtmlSummaryReport.record(result);
        ExtentReportManager.clearTest();
        System.out.println("[SKIP ] " + result.getMethod().getMethodName());
    }

    @Override
    public void onFinish(ITestContext context) {
        ExtentReportManager.flush();
        HtmlSummaryReport.write(java.nio.file.Path.of(
                com.travelhouse.config.ConfigReader.get("report.dir", "reports")));
        System.out.println("[DONE ] Passed=" + context.getPassedTests().size()
                + " Failed=" + context.getFailedTests().size()
                + " Skipped=" + context.getSkippedTests().size());
    }
}
