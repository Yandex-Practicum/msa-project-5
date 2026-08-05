package com.example.shipmentexporter;

import org.junit.jupiter.api.Test;

import java.io.StringWriter;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CsvWriterTest {
    @Test
    void writesRegularValuesAndNull() throws Exception {
        StringWriter output = new StringWriter();

        CsvWriter.writeRecord(output, List.of("id", "status", "comment"));
        CsvWriter.writeRecord(output, java.util.Arrays.asList("1", "DELIVERED", null));

        assertEquals("id,status,comment\n1,DELIVERED,\n", output.toString());
    }

    @Test
    void escapesCommaAndQuotes() throws Exception {
        StringWriter output = new StringWriter();

        CsvWriter.writeRecord(output, List.of("ACME, Inc.", "driver \"Alex\""));

        assertEquals("\"ACME, Inc.\",\"driver \"\"Alex\"\"\"\n", output.toString());
    }

    @Test
    void escapesLineBreak() throws Exception {
        StringWriter output = new StringWriter();

        CsvWriter.writeRecord(output, List.of("first line\nsecond line"));

        assertEquals("\"first line\nsecond line\"\n", output.toString());
    }
}
