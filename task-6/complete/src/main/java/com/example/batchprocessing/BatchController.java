package com.example.batchprocessing;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/batch")
public class BatchController {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job importProductJob;

    @Autowired
    private Tracer tracer;

    private static final Logger log = LoggerFactory.getLogger(BatchController.class);

    @PostMapping("/run")
    public ResponseEntity<Map<String, String>> runJob(HttpServletRequest request) {
        Map<String, String> response = new HashMap<>();

        // Получаем traceId и spanId
        String traceId = tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : "unknown";
        String spanId = tracer.currentSpan() != null ? tracer.currentSpan().context().spanId() : "unknown";
        String uri = request.getRequestURI();

        log.info("Batch job start. URI: {}, traceId: {}, spanId: {}", uri, traceId, spanId);

        try {
            // Создаем уникальные параметры для каждого запуска
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("startTime", System.currentTimeMillis())
                    .toJobParameters();

            // Запускаем job
            jobLauncher.run(importProductJob, jobParameters);

            response.put("status", "success");
            response.put("message", "Batch job started successfully");
            response.put("timestamp", String.valueOf(System.currentTimeMillis()));
            response.put("traceId", traceId);
            response.put("spanId", spanId);

            log.info("Batch job started successfully. URI: {}, traceId: {}, spanId: {}", uri, traceId, spanId);

            return ResponseEntity.ok(response);

        } catch (JobExecutionAlreadyRunningException e) {
            log.error("Job is already running. URI: {}, traceId: {}, spanId: {}", uri, traceId, spanId, e);
            response.put("status", "error");
            response.put("message", "Job is already running");
            response.put("traceId", traceId);
            response.put("spanId", spanId);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (JobRestartException e) {
            log.error("Job restart error. URI: {}, traceId: {}, spanId: {}", uri, traceId, spanId, e);
            response.put("status", "error");
            response.put("message", "Job restart error: " + e.getMessage());
            response.put("traceId", traceId);
            response.put("spanId", spanId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (JobInstanceAlreadyCompleteException e) {
            log.error("Job instance already complete. URI: {}, traceId: {}, spanId: {}", uri, traceId, spanId, e);
            response.put("status", "error");
            response.put("message", "Job instance already complete");
            response.put("traceId", traceId);
            response.put("spanId", spanId);
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (JobParametersInvalidException e) {
            log.error("Invalid job parameters. URI: {}, traceId: {}, spanId: {}", uri, traceId, spanId, e);
            response.put("status", "error");
            response.put("message", "Invalid job parameters: " + e.getMessage());
            response.put("traceId", traceId);
            response.put("spanId", spanId);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception e) {
            log.error("Unexpected error during batch job execution. URI: {}, traceId: {}, spanId: {}", uri, traceId, spanId, e);
            response.put("status", "error");
            response.put("message", "Unexpected error: " + e.getMessage());
            response.put("traceId", traceId);
            response.put("spanId", spanId);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}
