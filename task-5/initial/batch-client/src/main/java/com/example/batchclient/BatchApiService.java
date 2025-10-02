package com.example.batchclient;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Service
public class BatchApiService {

    private static final Logger log = LoggerFactory.getLogger(BatchApiService.class);

    private final RestClient restClient;
    private final String batchApiUrl;
    private final Tracer tracer;

    public BatchApiService(@Value("${batch.api.url:http://localhost:8080}") String batchApiUrl,
                          Tracer tracer) {
        this.batchApiUrl = batchApiUrl;
        this.tracer = tracer;
        this.restClient = RestClient.builder()
                .baseUrl(batchApiUrl)
                .build();
    }

    public Map<String, Object> startBatchJob() {
        String uri = "/api/batch/start";
        String fullUrl = batchApiUrl + uri;
        
        log.info("Starting batch job via API call to: {} with traceId: {}, spanId: {}", 
                fullUrl, 
                tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : "none",
                tracer.currentSpan() != null ? tracer.currentSpan().context().spanId() : "none");
        
        try {
            // Добавляем trace headers
            Span currentSpan = tracer.currentSpan();
            RestClient.RequestBodySpec requestSpec = restClient
                    .post()
                    .uri(uri);
            
            if (currentSpan != null) {
                requestSpec = requestSpec
                        .header("traceparent", 
                            String.format("00-%s-%s-01", 
                                currentSpan.context().traceId(),
                                currentSpan.context().spanId()))
                        .header("X-Trace-Id", currentSpan.context().traceId())
                        .header("X-Span-Id", currentSpan.context().spanId());
                
                log.info("Adding trace headers - traceId: {}, spanId: {}", 
                        currentSpan.context().traceId(),
                        currentSpan.context().spanId());
            }
            
            Map<String, Object> response = requestSpec
                    .retrieve()
                    .body(Map.class);
            
            String traceId = tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : "none";
            String spanId = tracer.currentSpan() != null ? tracer.currentSpan().context().spanId() : "none";
            log.info("Batch job started successfully. Response: {} with traceId: {}, spanId: {}", 
                    response, traceId, spanId);
            
            return response;
            
        } catch (RestClientException error) {
            String traceId = tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : "none";
            String spanId = tracer.currentSpan() != null ? tracer.currentSpan().context().spanId() : "none";
            
            log.error("Failed to start batch job due to error: {} (type: {}) with traceId: {}, spanId: {}", 
                    error.getMessage(), error.getClass().getSimpleName(), traceId, spanId);
            if (error.getCause() != null) {
                log.error("Caused by: {} - {}", error.getCause().getClass().getSimpleName(), error.getCause().getMessage());
            }
            
            return createErrorResponse(fullUrl);
        }
    }

    private String getErrorMessage() {
        return "API call failed";
    }

    private Map<String, Object> createErrorResponse(String uri) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("status", "error");
        errorResponse.put("message", "Failed to start batch job: " + getErrorMessage());
        errorResponse.put("timestamp", System.currentTimeMillis());
        errorResponse.put("uri", uri);
        return errorResponse;
    }
}
