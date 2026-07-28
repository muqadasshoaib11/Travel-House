package com.travelhouse.utils;

import com.travelhouse.base.DriverManager;
import com.travelhouse.config.ConfigReader;
import org.apache.commons.io.FileUtils;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public final class ScreenshotUtil {

    private ScreenshotUtil() {
    }

    public static void ensureDirs() {
        createDir(ConfigReader.get("screenshot.dir", "screenshots"));
        createDir(ConfigReader.get("report.dir", "reports"));
    }

    public static String capture(String testName) {
        if (!ConfigReader.getBoolean("screenshot.on.failure", true)) {
            return null;
        }
        ensureDirs();
        String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String safeName = testName.replaceAll("[^a-zA-Z0-9-_]", "_");
        Path dest = Path.of(ConfigReader.get("screenshot.dir", "screenshots"), safeName + "_" + stamp + ".png");
        try {
            File src = ((TakesScreenshot) DriverManager.getDriver()).getScreenshotAs(OutputType.FILE);
            FileUtils.copyFile(src, dest.toFile());
            return dest.toAbsolutePath().toString();
        } catch (Exception e) {
            System.err.println("Screenshot failed: " + e.getMessage());
            return null;
        }
    }

    private static void createDir(String dir) {
        try {
            Files.createDirectories(Path.of(dir));
        } catch (IOException e) {
            throw new IllegalStateException("Cannot create directory: " + dir, e);
        }
    }
}
