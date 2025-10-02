package com.example.csv.repository;

import com.example.csv.model.Shipment;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ShipmentRepository {
    private final String url;
    private final String username;
    private final String password;

    public ShipmentRepository(String url, String username, String password) {
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public List<Shipment> findAll() throws SQLException {
        List<Shipment> shipments = new ArrayList<>();
        String sql = "SELECT id, description, status, created_at FROM shipments ORDER BY id";

        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {

            while (resultSet.next()) {
                Shipment shipment = new Shipment();
                shipment.setId(resultSet.getLong("id"));
                shipment.setDescription(resultSet.getString("description"));
                shipment.setStatus(resultSet.getString("status"));
                
                Timestamp timestamp = resultSet.getTimestamp("created_at");
                if (timestamp != null) {
                    shipment.setCreatedAt(timestamp.toLocalDateTime());
                }
                
                shipments.add(shipment);
            }
        }

        return shipments;
    }

    public List<Shipment> findByStatus(String status) throws SQLException {
        List<Shipment> shipments = new ArrayList<>();
        String sql = "SELECT id, description, status, created_at FROM shipments WHERE status = ? ORDER BY id";

        try (Connection connection = DriverManager.getConnection(url, username, password);
             PreparedStatement statement = connection.prepareStatement(sql)) {

            statement.setString(1, status);
            
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Shipment shipment = new Shipment();
                    shipment.setId(resultSet.getLong("id"));
                    shipment.setDescription(resultSet.getString("description"));
                    shipment.setStatus(resultSet.getString("status"));
                    
                    Timestamp timestamp = resultSet.getTimestamp("created_at");
                    if (timestamp != null) {
                        shipment.setCreatedAt(timestamp.toLocalDateTime());
                    }
                    
                    shipments.add(shipment);
                }
            }
        }

        return shipments;
    }
}
