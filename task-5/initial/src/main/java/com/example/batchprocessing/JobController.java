package com.example.batchprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private static final Logger log = LoggerFactory.getLogger(JobController.class);

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job importProductJob;

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "application", "batch-processing"
        ));
    }

    @PostMapping("/process-file")
    public ResponseEntity<Map<String, Object>> startJob(
            @RequestParam(defaultValue = "product-data.csv") String fileName,
            HttpServletRequest request) {

        String traceId = request.getHeader("X-Trace-Id") != null ?
                request.getHeader("X-Trace-Id") :
                UUID.randomUUID().toString().substring(0, 16);
        String spanId = request.getHeader("X-Span-Id") != null ?
                request.getHeader("X-Span-Id") :
                UUID.randomUUID().toString().substring(0, 8);

        MDC.put("traceId", traceId);
        MDC.put("spanId", spanId);
        MDC.put("uri", request.getRequestURI());

        log.info("API called: POST /api/jobs/process-file - fileName={}", fileName);

        try {
            String requestId = UUID.randomUUID().toString().substring(0, 8);

            JobParameters jobParameters = new JobParametersBuilder()
                    .addString("fileName", fileName)
                    .addString("requestId", requestId)
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            log.info("Starting job: fileName={}, requestId={}", fileName, requestId);
            JobExecution jobExecution = jobLauncher.run(importProductJob, jobParameters);

            Map<String, Object> response = Map.of(
                    "jobId", jobExecution.getJobId(),
                    "jobName", jobExecution.getJobInstance().getJobName(),
                    "status", jobExecution.getStatus().toString(),
                    "traceId", traceId,
                    "spanId", spanId,
                    "requestId", requestId
            );

            log.info("Job completed: jobId={}, status={}",
                    jobExecution.getJobId(), jobExecution.getStatus());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Failed to start job: {}", e.getMessage(), e);
            return ResponseEntity.badRequest().body(Map.of(
                    "error", e.getMessage(),
                    "traceId", traceId,
                    "spanId", spanId
            ));
        } finally {
            MDC.remove("traceId");
            MDC.remove("spanId");
            MDC.remove("uri");
        }
    }
}