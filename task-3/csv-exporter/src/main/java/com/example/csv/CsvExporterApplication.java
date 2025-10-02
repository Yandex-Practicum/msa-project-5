package com.example.csv;

import com.example.csv.repository.ShipmentRepository;
import com.example.csv.service.CsvExportService;

import java.sql.SQLException;
import java.util.Arrays;

public class CsvExporterApplication {
    
    public static void main(String[] args) {
        // Configuration from environment variables
        String dbHost = getEnvOrDefault("DB_HOST", "postgres-service");
        String dbPort = getEnvOrDefault("DB_PORT", "5432");
        String dbName = getEnvOrDefault("DB_NAME", "shipment");
        String dbUser = getEnvOrDefault("DB_USER", "shipment");
        String dbPassword = getEnvOrDefault("DB_PASSWORD", "shipment");
        String outputDir = getEnvOrDefault("OUTPUT_DIR", "/app/exports");
        String exportType = getEnvOrDefault("EXPORT_TYPE", "all"); // all, status
        String statusFilter = getEnvOrDefault("STATUS_FILTER", "");

        String jdbcUrl = String.format("jdbc:postgresql://%s:%s/%s", dbHost, dbPort, dbName);
        
        System.out.println("Starting CSV Export Application");
        System.out.println("Database URL: " + jdbcUrl);
        System.out.println("Output Directory: " + outputDir);
        System.out.println("Export Type: " + exportType);
        
        try {
            // Initialize repository and service
            ShipmentRepository repository = new ShipmentRepository(jdbcUrl, dbUser, dbPassword);
            CsvExportService exportService = new CsvExportService(repository, outputDir);
            
            // Create output directory
            exportService.createOutputDirectory();
            
            String exportedFile;
            
            if ("status".equals(exportType) && !statusFilter.isEmpty()) {
                System.out.println("Exporting shipments with status: " + statusFilter);
                exportedFile = exportService.exportShipmentsByStatus(statusFilter);
            } else {
                System.out.println("Exporting all shipments");
                exportedFile = exportService.exportAllShipments();
            }
            
            System.out.println("Export completed successfully!");
            System.out.println("Exported file: " + exportedFile);
            
        } catch (SQLException e) {
            System.err.println("Database error: " + e.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("Error during export: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static String getEnvOrDefault(String key, String defaultValue) {
        String value = System.getenv(key);
        return value != null ? value : defaultValue;
    }
}
