package com.example.shipmentexporter;

import java.sql.Connection;
import java.sql.DriverManager;
import java.time.LocalDate;

public final class ShipmentExportApplication {
    private ShipmentExportApplication() {
    }

    public static void main(String[] args) {
        int exitCode = run();
        if (exitCode != 0) {
            System.exit(exitCode);
        }
    }

    static int run() {
        try {
            ExportSettings settings = ExportSettings.fromEnvironment();
            LocalDate exportDate = LocalDate.now(settings.timeZone());

            System.out.printf("Starting shipments export for %s%n", exportDate);

            try (Connection connection = DriverManager.getConnection(
                    settings.dbUrl(),
                    settings.dbUser(),
                    settings.dbPassword()
            )) {
                connection.setReadOnly(true);
                connection.setAutoCommit(false);

                ShipmentCsvExporter.ExportResult result = new ShipmentCsvExporter().export(
                        connection,
                        settings.outputDirectory(),
                        exportDate,
                        settings.fetchSize()
                );
                connection.commit();

                System.out.printf(
                        "Export completed: rows=%d, file=%s%n",
                        result.rowCount(),
                        result.outputFile()
                );
            }
            return 0;
        } catch (Exception exception) {
            System.err.println("Export failed: " + exception.getMessage());
            exception.printStackTrace(System.err);
            return 1;
        }
    }
}
