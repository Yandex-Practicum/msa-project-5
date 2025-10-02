package com.example.csv.service;

import com.example.csv.model.Shipment;
import com.example.csv.repository.ShipmentRepository;
import com.opencsv.CSVWriter;

import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CsvExportService {
    private final ShipmentRepository shipmentRepository;
    private final String outputDirectory;

    public CsvExportService(ShipmentRepository shipmentRepository, String outputDirectory) {
        this.shipmentRepository = shipmentRepository;
        this.outputDirectory = outputDirectory;
    }

    public String exportAllShipments() throws SQLException, IOException {
        List<Shipment> shipments = shipmentRepository.findAll();
        return exportShipmentsToCsv(shipments, "all_shipments");
    }

    public String exportShipmentsByStatus(String status) throws SQLException, IOException {
        List<Shipment> shipments = shipmentRepository.findByStatus(status);
        return exportShipmentsToCsv(shipments, "shipments_" + status.toLowerCase().replace(" ", "_"));
    }

    private String exportShipmentsToCsv(List<Shipment> shipments, String filenamePrefix) throws IOException {
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String filename = String.format("%s_%s.csv", filenamePrefix, timestamp);
        String filepath = outputDirectory + "/" + filename;

        try (FileWriter fileWriter = new FileWriter(filepath);
             CSVWriter csvWriter = new CSVWriter(fileWriter)) {

            // Write header
            String[] header = {"ID", "Description", "Status", "Created At"};
            csvWriter.writeNext(header);

            // Write data
            for (Shipment shipment : shipments) {
                String[] data = {
                    String.valueOf(shipment.getId()),
                    shipment.getDescription(),
                    shipment.getStatus(),
                    shipment.getCreatedAt() != null ? 
                        shipment.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : ""
                };
                csvWriter.writeNext(data);
            }
        }

        return filepath;
    }

    public void createOutputDirectory() {
        java.io.File directory = new java.io.File(outputDirectory);
        if (!directory.exists()) {
            directory.mkdirs();
        }
    }
}
