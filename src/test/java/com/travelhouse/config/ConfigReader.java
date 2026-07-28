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

        // Allow -D overrides: -Dapp.package=..., -Ddevice.udid=...
        for (String key : PROPS.stringPropertyNames()) {
            String sys = System.getProperty(key);
            if (sys != null && !sys.isBlank()) {
                PROPS.setProperty(key, sys);
            }
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
