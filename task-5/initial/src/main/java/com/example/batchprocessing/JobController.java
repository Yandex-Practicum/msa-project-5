package com.example.batchprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobController {
    
    private static final Logger log = LoggerFactory.getLogger(JobController.class);
    
    @Autowired
    private JobLauncher jobLauncher;
    
    @Autowired
    private Job importProductJob;
    
    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> runJob(@RequestHeader(value = "X-Trace-Id", required = false) String traceId) {
        
        // Генерируем traceId если не передан
        String requestTraceId = traceId != null ? traceId : UUID.randomUUID().toString().replace("-", "");
        String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        
        // Добавляем в MDC для логирования внутри метода
        MDC.put("traceId", requestTraceId);
        MDC.put("spanId", spanId);
        MDC.put("uri", "/api/v1/jobs/run");
        MDC.put("method", "POST");
        
        log.info("=== JOB EXECUTION STARTED ===");
        
        Map<String, Object> response = new HashMap<>();
        response.put("traceId", requestTraceId);
        response.put("spanId", spanId);
        response.put("timestamp", System.currentTimeMillis());
        
        try {
            log.info("Creating job parameters");
            JobParameters params = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .addString("traceId", requestTraceId)
                    .addString("spanId", spanId)
                    .toJobParameters();
            
            log.info("Launching job: importProductJob");
            long startTime = System.currentTimeMillis();
            
            var execution = jobLauncher.run(importProductJob, params);
            
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("Job completed with status: {}", execution.getStatus());
            log.info("Job duration: {} ms", duration);
            
            response.put("status", execution.getStatus().toString());
            response.put("jobId", execution.getJobId());
            response.put("duration", duration);
            response.put("message", "Job executed successfully");
            
            log.info("=== JOB EXECUTION COMPLETED ===");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error executing job", e);
            
            response.put("status", "FAILED");
            response.put("message", e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            
            log.error("=== JOB EXECUTION FAILED ===");
            
            return ResponseEntity.internalServerError().body(response);
            
        } finally {
            MDC.clear();
        }
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        response.put("app", "batch-processing");
        return ResponseEntity.ok(response);
    }
}