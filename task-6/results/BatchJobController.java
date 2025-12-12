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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/batch")
public class BatchJobController {

    private static final Logger log = LoggerFactory.getLogger(BatchJobController.class);

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job importProductJob;

    @PostMapping("/trigger")
    public ResponseEntity<Map<String, Object>> triggerBatchJob(HttpServletRequest request) {
        MDC.put("uri", request.getRequestURI());
        
        try {
            log.info("Batch job trigger endpoint called. URI: {}", request.getRequestURI());
            
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("timestamp", System.currentTimeMillis())
                    .toJobParameters();
            
            log.info("Starting batch job with parameters: {}", jobParameters);
            
            var jobExecution = jobLauncher.run(importProductJob, jobParameters);
            
            log.info("Batch job completed. Status: {}, Exit Status: {}",
                    jobExecution.getStatus(), 
                    jobExecution.getExitStatus());
            
            Map<String, Object> response = new HashMap<>();
            response.put("jobId", jobExecution.getJobId());
            response.put("status", jobExecution.getStatus().toString());
            response.put("exitStatus", jobExecution.getExitStatus().getExitCode());
            response.put("startTime", jobExecution.getStartTime());
            response.put("endTime", jobExecution.getEndTime());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error executing batch job", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("error", "Failed to execute batch job");
            errorResponse.put("message", e.getMessage());
            
            return ResponseEntity.internalServerError().body(errorResponse);
        } finally {
            MDC.remove("uri");
        }
    }
}
