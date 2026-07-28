package com.travelhouse.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Properties;

public final class TestDataReader {

    private static final Properties PROPS = new Properties();

    static {
        load();
    }

    private TestDataReader() {
    }

    private static void load() {
        String override = System.getProperty("testdata.file");
        Path path = Path.of(Objects.requireNonNullElse(override, "src/test/resources/testdata.properties"));
        try (InputStream in = Files.exists(path)
                ? Files.newInputStream(path)
                : TestDataReader.class.getClassLoader().getResourceAsStream("testdata.properties")) {
            if (in == null) {
                throw new IllegalStateException("testdata.properties not found");
            }
            PROPS.load(in);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load testdata.properties", e);
        }

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
}
