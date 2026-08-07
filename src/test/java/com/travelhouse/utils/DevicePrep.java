package com.travelhouse.utils;

import com.travelhouse.config.ApkResolver;
import com.travelhouse.config.ConfigReader;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Prepares the physical device before an Appium session:
 * - Warms up Appium Settings (Xiaomi/MIUI)
 * - Installs the APK from apps/ via adb when present
 */
public final class DevicePrep {

    private static volatile boolean apkInstalledThisRun = false;

    private DevicePrep() {
    }

    public static void prepareForSession() {
        String udid = ConfigReader.get("device.udid");
        String[] adbPrefix = udid.isBlank()
                ? new String[]{"adb"}
                : new String[]{"adb", "-s", udid};

        runQuiet(adbPrefix, "shell", "dumpsys", "deviceidle", "whitelist", "+io.appium.settings");
        runQuiet(adbPrefix, "shell", "cmd", "appops", "set", "io.appium.settings", "RUN_IN_BACKGROUND", "allow");
        runQuiet(adbPrefix, "shell", "cmd", "appops", "set", "io.appium.settings", "RUN_ANY_IN_BACKGROUND", "allow");
        runQuiet(adbPrefix, "shell", "am", "start", "-n", "io.appium.settings/.Settings");

        // Keep display on for USB-connected CI (Xiaomi otherwise blacks out mid-run)
        runQuiet(adbPrefix, "shell", "svc", "power", "stayon", "usb");
        runQuiet(adbPrefix, "shell", "settings", "put", "system", "screen_off_timeout", "1800000");
        runQuiet(adbPrefix, "shell", "input", "keyevent", "KEYCODE_WAKEUP");
        runQuiet(adbPrefix, "shell", "input", "keyevent", "KEYCODE_MENU");
        // Unlock swipe (no-op if already unlocked)
        runQuiet(adbPrefix, "shell", "input", "swipe", "540", "1800", "540", "600");
        // Do not force-stop the app here: cold start + login often exceeds CI step timeouts.
        // Only recycle UiAutomator2 so findElements cannot hang from a stale server.
        restartUiAutomator2();

        if (!apkInstalledThisRun) {
            installApkIfPresent(adbPrefix);
            apkInstalledThisRun = true;
        }

        logInstalledAppVersion(adbPrefix);

        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Prints the on-device app version so CI/local logs show which build under test. */
    private static void logInstalledAppVersion(String[] adbPrefix) {
        String pkg = ConfigReader.get("app.package", "com.travelhouse.uk.app");
        List<String> command = new ArrayList<>();
        for (String part : adbPrefix) {
            command.add(part);
        }
        command.add("shell");
        command.add("dumpsys");
        command.add("package");
        command.add(pkg);
        CommandResult result = run(command.toArray(new String[0]), 20);
        String versionName = firstMatch(result.output, "versionName=");
        String versionCode = firstMatch(result.output, "versionCode=");
        String expectedName = ConfigReader.get("app.version.name");
        String expectedCode = ConfigReader.get("app.version.code");
        System.out.println("[APP] Installed " + pkg
                + " versionName=" + (versionName.isBlank() ? "?" : versionName)
                + " versionCode=" + (versionCode.isBlank() ? "?" : versionCode.split("\\s+")[0]));
        if (!expectedName.isBlank() && !versionName.isBlank() && !expectedName.equals(versionName)) {
            System.out.println("[APP] WARNING: expected versionName=" + expectedName
                    + " but device has " + versionName);
        }
        if (!expectedCode.isBlank() && !versionCode.isBlank()) {
            String codeOnly = versionCode.split("\\s+")[0];
            if (!expectedCode.equals(codeOnly)) {
                System.out.println("[APP] WARNING: expected versionCode=" + expectedCode
                        + " but device has " + codeOnly);
            }
        }
    }

    private static String firstMatch(String text, String prefix) {
        if (text == null || text.isBlank()) {
            return "";
        }
        for (String line : text.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith(prefix)) {
                return trimmed.substring(prefix.length()).trim();
            }
        }
        return "";
    }

    /**
     * Force-restart UiAutomator2 on-device servers when findElements hangs or the session freezes.
     */
    public static void restartUiAutomator2() {
        String udid = ConfigReader.get("device.udid");
        String[] adbPrefix = udid.isBlank()
                ? new String[]{"adb"}
                : new String[]{"adb", "-s", udid};
        System.out.println("[DevicePrep] Restarting UiAutomator2 server processes");
        runQuiet(adbPrefix, "shell", "am", "force-stop", "io.appium.uiautomator2.server");
        runQuiet(adbPrefix, "shell", "am", "force-stop", "io.appium.uiautomator2.server.test");
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void installApkIfPresent(String[] adbPrefix) {
        Optional<Path> apk = ApkResolver.resolve();
        if (apk.isEmpty()) {
            return;
        }

        boolean enforce = ConfigReader.getBoolean("enforce.app.install", true);
        if (!enforce) {
            System.out.println("[APK] Skipping install (enforce.app.install=false) — using installed app if present");
            return;
        }

        Path apkPath = apk.get();
        System.out.println("[APK] Installing via adb: " + apkPath);
        System.out.println("[APK] If Xiaomi shows a popup, tap Allow / Install on the phone.");

        List<String> command = new ArrayList<>();
        for (String part : adbPrefix) {
            command.add(part);
        }
        command.add("install");
        command.add("-r");
        command.add("-g");
        command.add(apkPath.toString());

        CommandResult result = run(command.toArray(new String[0]), 180);
        if (result.exitCode != 0 || !result.output.toLowerCase().contains("success")) {
            List<String> retry = new ArrayList<>();
            for (String part : adbPrefix) {
                retry.add(part);
            }
            retry.add("install");
            retry.add("-r");
            retry.add(apkPath.toString());
            result = run(retry.toArray(new String[0]), 180);
        }

        if (result.exitCode != 0 || !result.output.toLowerCase().contains("success")) {
            throw new IllegalStateException(
                    "Failed to install APK from apps/.\n"
                            + "Output: " + result.output + "\n"
                            + "On Xiaomi: enable Install via USB + USB debugging (Security settings), "
                            + "unlock the phone, and tap Allow when the install popup appears.");
        }
        System.out.println("[APK] Install succeeded: " + apkPath.getFileName());
    }

    private static boolean isPackageInstalled(String[] adbPrefix, String appPackage) {
        List<String> command = new ArrayList<>();
        for (String part : adbPrefix) {
            command.add(part);
        }
        command.add("shell");
        command.add("pm");
        command.add("path");
        command.add(appPackage);
        CommandResult result = run(command.toArray(new String[0]), 15);
        return result.exitCode == 0 && result.output.contains("package:");
    }

    private static void runQuiet(String[] prefix, String... args) {
        String[] command = new String[prefix.length + args.length];
        System.arraycopy(prefix, 0, command, 0, prefix.length);
        System.arraycopy(args, 0, command, prefix.length, args.length);
        run(command, 10);
    }

    private static CommandResult run(String[] command, long timeoutSeconds) {
        try {
            Process process = new ProcessBuilder(command)
                    .redirectErrorStream(true)
                    .start();
            String output;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = reader.lines().collect(Collectors.joining("\n"));
            }
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new CommandResult(1, "Timed out after " + timeoutSeconds + "s");
            }
            return new CommandResult(process.exitValue(), output);
        } catch (Exception e) {
            return new CommandResult(1, e.getMessage());
        }
    }

    private static final class CommandResult {
        private final int exitCode;
        private final String output;

        private CommandResult(int exitCode, String output) {
            this.exitCode = exitCode;
            this.output = output == null ? "" : output;
        }
    }
}
