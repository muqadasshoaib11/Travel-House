package com.travelhouse.config;

import io.appium.java_client.android.options.UiAutomator2Options;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public final class CapabilityFactory {

    private CapabilityFactory() {
    }

    public static UiAutomator2Options createAndroidOptions() {
        UiAutomator2Options options = new UiAutomator2Options();
        options.setPlatformName(ConfigReader.get("platform.name", "Android"));
        options.setAutomationName(ConfigReader.get("automation.name", "UiAutomator2"));
        options.setDeviceName(ConfigReader.get("device.name", "Android Device"));
        options.setNewCommandTimeout(Duration.ofSeconds(ConfigReader.getInt("new.command.timeout", 300)));
        options.setNoReset(ConfigReader.getBoolean("no.reset", true));
        options.setFullReset(ConfigReader.getBoolean("full.reset", false));
        options.setAutoGrantPermissions(ConfigReader.getBoolean("auto.grant.permissions", true));

        String platformVersion = ConfigReader.get("platform.version");
        if (!platformVersion.isBlank()) {
            options.setPlatformVersion(platformVersion);
        }

        String udid = resolveUdid();
        if (!udid.isBlank()) {
            options.setUdid(udid);
        }

        Optional<Path> apk = ApkResolver.resolve();
        String appPackage = ConfigReader.get("app.package");
        String activity = ConfigReader.get("app.activity");

        if (apk.isPresent()) {
            // APK is installed earlier by DevicePrep via adb; session just launches the app.
            System.out.println("[APK] Using apps/ build: " + apk.get().getFileName()
                    + " (installed in DevicePrep, launching " + appPackage + ")");
            if (appPackage.isBlank()) {
                throw new IllegalStateException(
                        "app.package is required when installing from apps/. Keep com.travelhouse.uk.app in config.properties.");
            }
            options.setAppPackage(appPackage);
            if (!activity.isBlank()) {
                options.setAppActivity(activity);
            }
            options.setCapability("appium:enforceAppInstall", false);
        } else if (!appPackage.isBlank()) {
            System.out.println("[APK] No APK in apps/ — launching already-installed package: " + appPackage);
            options.setAppPackage(appPackage);
            if (!activity.isBlank()) {
                options.setAppActivity(activity);
            }
        } else {
            throw new IllegalStateException(
                    "Put a .apk in the apps/ folder (recommended), or set app.package in config.properties.");
        }

        // Keep session stable on physical devices (esp. Xiaomi/MIUI)
        options.setCapability("appium:ignoreHiddenApiPolicyError", true);
        options.setCapability("appium:disableWindowAnimation", true);
        options.setCapability("appium:settingsAppStartupTimeout", 60000);
        options.setCapability("appium:uiautomator2ServerLaunchTimeout", 60000);
        options.setCapability("appium:adbExecTimeout", 120000);
        options.setCapability("appium:skipDeviceInitialization", false);

        return options;
    }

    private static String resolveUdid() {
        String configured = ConfigReader.get("device.udid");
        if (!configured.isBlank()) {
            return configured.trim();
        }
        List<String> devices = listConnectedDevices();
        if (devices.isEmpty()) {
            throw new IllegalStateException(
                    "No Android device detected via adb. Enable USB debugging, authorize this PC, then run: adb devices");
        }
        if (devices.size() > 1) {
            throw new IllegalStateException(
                    "Multiple devices connected (" + String.join(", ", devices)
                            + "). Set device.udid in config.properties to the target device.");
        }
        return devices.get(0);
    }

    public static List<String> listConnectedDevices() {
        List<String> devices = new ArrayList<>();
        try {
            Process process = new ProcessBuilder("adb", "devices")
                    .redirectErrorStream(true)
                    .start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                List<String> lines = reader.lines().collect(Collectors.toList());
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty() || line.startsWith("List of devices")) {
                        continue;
                    }
                    String[] parts = line.split("\\s+");
                    if (parts.length >= 2 && "device".equalsIgnoreCase(parts[1])) {
                        devices.add(parts[0]);
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to run 'adb devices'. Is Android platform-tools on PATH?", e);
        }
        return devices;
    }
}
