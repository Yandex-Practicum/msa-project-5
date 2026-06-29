package com.example.batchprocessing.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public class BatchJobClient {

    private static final Logger log = LoggerFactory.getLogger(BatchJobClient.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final String BASE_URL = "http://localhost:8080/api/jobs";

    private final HttpClient httpClient;

    public BatchJobClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public static void main(String[] args) {
        String fileName = args.length > 0 ? args[0] : "product-data.csv";

        String traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
        String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);

        MDC.put("traceId", traceId);
        MDC.put("spanId", spanId);

        log.info("╔════════════════════════════════════════╗");
        log.info("║   Batch Job Client                     ║");
        log.info("╠════════════════════════════════════════╣");
        log.info("║ TraceId: {}  ║", traceId);
        log.info("║ SpanId:  {}          ║", spanId);
        log.info("║ File:    {}                 ║", fileName);
        log.info("╚════════════════════════════════════════╝");

        BatchJobClient client = new BatchJobClient();

        try {
            // Health check
            String healthUri = BASE_URL + "/health";
            MDC.put("uri", healthUri);
            log.info("Health check...");
            HttpResponse<String> healthResp = client.get(healthUri, traceId, spanId);
            log.info("Health: {}", healthResp.body());
            MDC.remove("uri");

            if (healthResp.statusCode() != 200) {
                log.error("App is down!");
                System.exit(1);
            }

            // Start job
            String jobUri = BASE_URL + "/process-file?fileName=" + fileName;
            MDC.put("uri", jobUri);
            log.info("Starting ETL job...");
            HttpResponse<String> jobResp = client.post(jobUri, traceId, spanId);
            log.info("Response: {}", jobResp.body());
            MDC.remove("uri");

            Map<String, Object> result = objectMapper.readValue(jobResp.body(), Map.class);
            log.info("Status: {} | JobId: {}", result.get("status"), result.get("jobId"));

        } catch (Exception e) {
            log.error("Error: {}", e.getMessage());
        } finally {
            MDC.remove("traceId");
            MDC.remove("spanId");
            log.info("Done. Kibana: http://localhost:5601");
        }
    }

    private HttpResponse<String> get(String url, String traceId, String spanId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(10))
                .header("X-Trace-Id", traceId)
                .header("X-Span-Id", spanId)
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(String url, String traceId, String spanId) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("X-Trace-Id", traceId)
                .header("X-Span-Id", spanId)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}