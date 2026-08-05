package com.example.shipmentexporter;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

final class CsvWriter {
    private CsvWriter() {
    }

    static void writeRecord(Writer writer, List<String> values) throws IOException {
        for (int index = 0; index < values.size(); index++) {
            if (index > 0) {
                writer.write(',');
            }
            writer.write(escape(values.get(index)));
        }
        writer.write('\n');
    }

    private static String escape(String value) {
        if (value == null) {
            return "";
        }

        boolean requiresQuotes = value.indexOf(',') >= 0
                || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0
                || value.indexOf('\r') >= 0;

        if (!requiresQuotes) {
            return value;
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
