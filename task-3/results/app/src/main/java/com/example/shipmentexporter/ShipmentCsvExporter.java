package com.example.shipmentexporter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

final class ShipmentCsvExporter {
    private static final String EXPORT_QUERY = """
            SELECT *
            FROM shipments
            WHERE shipment_date = ?
            ORDER BY id
            """;

    ExportResult export(Connection connection, Path outputDirectory, LocalDate exportDate, int fetchSize)
            throws SQLException, IOException {
        Files.createDirectories(outputDirectory);

        Path outputFile = outputDirectory.resolve("shipments-" + exportDate + ".csv");
        Path temporaryFile = outputDirectory.resolve(outputFile.getFileName() + ".part");
        Files.deleteIfExists(temporaryFile);

        long rowCount;
        try {
            rowCount = writeCsv(connection, temporaryFile, exportDate, fetchSize);
            moveCompletedFile(temporaryFile, outputFile);
        } finally {
            Files.deleteIfExists(temporaryFile);
        }

        return new ExportResult(outputFile, rowCount);
    }

    private long writeCsv(Connection connection, Path temporaryFile, LocalDate exportDate, int fetchSize)
            throws SQLException, IOException {
        try (PreparedStatement statement = connection.prepareStatement(EXPORT_QUERY)) {
            statement.setObject(1, exportDate);
            statement.setFetchSize(fetchSize);

            try (ResultSet resultSet = statement.executeQuery();
                 BufferedWriter writer = Files.newBufferedWriter(
                         temporaryFile,
                         StandardCharsets.UTF_8,
                         StandardOpenOption.CREATE_NEW,
                         StandardOpenOption.WRITE
                 )) {
                ResultSetMetaData metadata = resultSet.getMetaData();
                int columnCount = metadata.getColumnCount();
                CsvWriter.writeRecord(writer, columnNames(metadata, columnCount));

                long rowCount = 0;
                while (resultSet.next()) {
                    CsvWriter.writeRecord(writer, rowValues(resultSet, columnCount));
                    rowCount++;
                }
                return rowCount;
            }
        }
    }

    private List<String> columnNames(ResultSetMetaData metadata, int columnCount) throws SQLException {
        List<String> names = new ArrayList<>(columnCount);
        for (int column = 1; column <= columnCount; column++) {
            names.add(metadata.getColumnLabel(column));
        }
        return names;
    }

    private List<String> rowValues(ResultSet resultSet, int columnCount) throws SQLException {
        List<String> values = new ArrayList<>(columnCount);
        for (int column = 1; column <= columnCount; column++) {
            values.add(resultSet.getString(column));
        }
        return values;
    }

    private void moveCompletedFile(Path temporaryFile, Path outputFile) throws IOException {
        try {
            Files.move(
                    temporaryFile,
                    outputFile,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryFile, outputFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    record ExportResult(Path outputFile, long rowCount) {
    }
}
