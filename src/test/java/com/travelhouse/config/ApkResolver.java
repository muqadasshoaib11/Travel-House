package com.travelhouse.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Resolves the Travel House APK from config or by auto-discovering *.apk under apps/.
 */
public final class ApkResolver {

    private ApkResolver() {
    }

    /**
     * @return absolute APK path, or empty if none configured/found
     */
    public static Optional<Path> resolve() {
        String explicit = ConfigReader.get("app.apk.path");
        if (!explicit.isBlank()) {
            Path apk = Path.of(explicit).toAbsolutePath().normalize();
            if (!Files.isRegularFile(apk)) {
                throw new IllegalStateException("Configured app.apk.path not found: " + apk);
            }
            return Optional.of(apk);
        }

        if (!ConfigReader.getBoolean("app.apk.auto", true)) {
            return Optional.empty();
        }

        Path dir = Path.of(ConfigReader.get("app.apk.dir", "apps")).toAbsolutePath().normalize();
        if (!Files.isDirectory(dir)) {
            return Optional.empty();
        }

        try (Stream<Path> stream = Files.list(dir)) {
            List<Path> apks = stream
                    .filter(p -> Files.isRegularFile(p))
                    .filter(p -> p.getFileName().toString().toLowerCase().endsWith(".apk"))
                    .sorted(Comparator.comparingLong(ApkResolver::lastModified).reversed())
                    .collect(Collectors.toList());

            if (apks.isEmpty()) {
                return Optional.empty();
            }
            if (apks.size() > 1) {
                System.out.println("[APK] Multiple APKs found in " + dir + " — using newest: "
                        + apks.get(0).getFileName());
            }
            return Optional.of(apks.get(0).toAbsolutePath().normalize());
        } catch (IOException e) {
            throw new IllegalStateException("Failed to scan APK directory: " + dir, e);
        }
    }

    public static Path requireApk() {
        return resolve().orElseThrow(() -> new IllegalStateException(
                "No APK found. Put your Travel House .apk file in the apps/ folder, then run tests again."));
    }

    private static long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException e) {
            return 0L;
        }
    }
}
