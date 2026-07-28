package com.travelhouse.utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;
import com.aventstack.extentreports.reporter.configuration.Theme;
import com.travelhouse.config.ConfigReader;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ExtentReportManager {

    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> TEST = new ThreadLocal<>();

    private ExtentReportManager() {
    }

    public static synchronized ExtentReports getInstance() {
        if (extent == null) {
            ScreenshotUtil.ensureDirs();
            String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            Path reportPath = Path.of(ConfigReader.get("report.dir", "reports"),
                    "TravelHouse_Extent_" + stamp + ".html");

            ExtentSparkReporter spark = new ExtentSparkReporter(reportPath.toString());
            spark.config().setDocumentTitle("Travel House Appium Report");
            spark.config().setReportName("Travel House Android Automation");
            spark.config().setTheme(Theme.STANDARD);

            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("App Package", ConfigReader.get("app.package"));
            extent.setSystemInfo("Platform", ConfigReader.get("platform.name", "Android"));
            extent.setSystemInfo("Device", ConfigReader.get("device.name", "unknown"));
        }
        return extent;
    }

    public static void startTest(String name, String description) {
        ExtentTest test = getInstance().createTest(name, description == null ? "" : description);
        TEST.set(test);
    }

    public static ExtentTest getTest() {
        return TEST.get();
    }

    public static void logPass(String message) {
        if (getTest() != null) {
            getTest().pass(message);
        }
    }

    public static void logFail(String message) {
        if (getTest() != null) {
            getTest().fail(message);
        }
    }

    public static void logInfo(String message) {
        if (getTest() != null) {
            getTest().info(message);
        }
    }

    public static void attachScreenshot(String path) {
        if (getTest() != null && path != null && !path.isBlank()) {
            try {
                getTest().addScreenCaptureFromPath(path);
            } catch (Exception e) {
                getTest().warning("Could not attach screenshot: " + e.getMessage());
            }
        }
    }

    public static void flush() {
        if (extent != null) {
            extent.flush();
        }
        TEST.remove();
    }

    public static void clearTest() {
        TEST.remove();
    }
}
