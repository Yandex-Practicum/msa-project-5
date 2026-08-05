package com.example.shipmentexporter;

import java.nio.file.Path;
import java.time.ZoneId;
import java.util.Map;

record ExportSettings(
        String dbUrl,
        String dbUser,
        String dbPassword,
        Path outputDirectory,
        ZoneId timeZone,
        int fetchSize
) {
    private static final int DEFAULT_FETCH_SIZE = 1_000;

    static ExportSettings fromEnvironment() {
        return from(System.getenv());
    }

    static ExportSettings from(Map<String, String> environment) {
        return new ExportSettings(
                required(environment, "DB_URL"),
                required(environment, "DB_USER"),
                required(environment, "DB_PASSWORD"),
                Path.of(environment.getOrDefault("OUTPUT_DIR", "/data/exports")),
                ZoneId.of(environment.getOrDefault("EXPORT_TIME_ZONE", "UTC")),
                positiveInteger(environment.get("DB_FETCH_SIZE"), DEFAULT_FETCH_SIZE)
        );
    }

    private static String required(Map<String, String> environment, String name) {
        String value = environment.get(name);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Required environment variable is missing: " + name);
        }
        return value;
    }

    private static int positiveInteger(String value, int defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }

        int parsed = Integer.parseInt(value);
        if (parsed <= 0) {
            throw new IllegalArgumentException("DB_FETCH_SIZE must be greater than zero");
        }
        return parsed;
    }
}
