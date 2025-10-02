package com.example.batchclient;

import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/client")
public class BatchClientController {

    private static final Logger log = LoggerFactory.getLogger(BatchClientController.class);

    @Autowired
    private BatchApiService batchApiService;

    @Autowired
    private Tracer tracer;

    @PostMapping("/trigger-batch")
    public ResponseEntity<Map> triggerBatchJob() {
        String traceId = tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : "none";
        String spanId = tracer.currentSpan() != null ? tracer.currentSpan().context().spanId() : "none";
        String uri = "/client/trigger-batch";
        
        log.info("Received request to trigger batch job with traceId: {}, spanId: {}, URI: {}", 
                traceId, spanId, uri);
        
        try {
            Map<String, Object> response = batchApiService.startBatchJob();
            String status = (String) response.get("status");
            
            if ("success".equals(status)) {
                log.info("Batch job triggered successfully with traceId: {}, spanId: {}", traceId, spanId);
                return ResponseEntity.ok(response);
            } else {
                log.error("Failed to trigger batch job: {} with traceId: {}, spanId: {}", 
                        response.get("message"), traceId, spanId);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception error) {
            log.error("Error triggering batch job: {} with traceId: {}, spanId: {}", 
                    error.getMessage(), traceId, spanId);
            return ResponseEntity.internalServerError().body(Map.of(
                "status", "error",
                "message", "Internal server error: " + error.getMessage(),
                "timestamp", System.currentTimeMillis()
            ));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        String traceId = tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : "none";
        String spanId = tracer.currentSpan() != null ? tracer.currentSpan().context().spanId() : "none";
        
        log.info("Health check requested with traceId: {}, spanId: {}", traceId, spanId);
        
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "batch-client",
            "timestamp", String.valueOf(System.currentTimeMillis()),
            "traceId", traceId,
            "spanId", spanId
        ));
    }
}
