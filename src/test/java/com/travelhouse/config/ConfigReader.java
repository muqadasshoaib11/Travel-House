package com.travelhouse.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

public final class ConfigReader {

    private static final Properties PROPS = new Properties();

    static {
        load();
    }

    private ConfigReader() {
    }

    private static void load() {
        String override = System.getProperty("config.file");
        Path path = Path.of(Objects.requireNonNullElse(override, "src/test/resources/config.properties"));
        try (InputStream in = Files.exists(path)
                ? Files.newInputStream(path)
                : ConfigReader.class.getClassLoader().getResourceAsStream("config.properties")) {
            if (in == null) {
                throw new IllegalStateException("config.properties not found");
            }
            PROPS.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load config.properties", e);
        }

        // -Dkey=value overrides for any key already in the file
        for (String key : PROPS.stringPropertyNames()) {
            String sys = System.getProperty(key);
            if (sys != null && !sys.isBlank()) {
                PROPS.setProperty(key, sys);
            }
        }

        // CI/CD: always allow these System properties / env vars (even if blank in file)
        applyOverride("login.email", "LOGIN_EMAIL");
        applyOverride("login.password", "LOGIN_PASSWORD");
        applyOverride("device.udid", "DEVICE_UDID");
        applyOverride("device.name", "DEVICE_NAME");
        applyOverride("appium.server.url", "APPIUM_SERVER_URL");
        applyOverride("app.package", "APP_PACKAGE");
        applyOverride("app.activity", "APP_ACTIVITY");
    }

    private static void applyOverride(String propertyKey, String envKey) {
        String sys = System.getProperty(propertyKey);
        if (sys != null && !sys.isBlank()) {
            PROPS.setProperty(propertyKey, sys.trim());
            return;
        }
        String env = System.getenv(envKey);
        if (env != null && !env.isBlank()) {
            PROPS.setProperty(propertyKey, env.trim());
        }
    }

    public static String get(String key) {
        return PROPS.getProperty(key, "");
    }

    public static String get(String key, String defaultValue) {
        String value = PROPS.getProperty(key);
        return (value == null || value.isBlank()) ? defaultValue : value;
    }

    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value.isBlank()) {
            return defaultValue;
        }
        return Integer.parseInt(value.trim());
    }

    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(value.trim());
    }
}
