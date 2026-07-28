package com.travelhouse.config;

public final class Credentials {

    private Credentials() {
    }

    public static String email() {
        return ConfigReader.get("login.email").trim();
    }

    public static String password() {
        return ConfigReader.get("login.password").trim();
    }

    public static boolean isConfigured() {
        return !email().isBlank() && !password().isBlank();
    }

    public static void requireConfigured() {
        if (!isConfigured()) {
            throw new IllegalStateException(
                    "Login credentials missing. Set login.email and login.password in src/test/resources/config.properties");
        }
    }
}
