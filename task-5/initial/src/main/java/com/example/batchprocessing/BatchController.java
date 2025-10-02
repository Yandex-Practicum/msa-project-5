package com.example.batchprocessing;

import java.util.HashMap;
import java.util.Map;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api")
public class BatchController {

    private static final Logger log = LoggerFactory.getLogger(BatchController.class);

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job importProductJob;

    @Autowired
    private MetricsService metricsService;

    @PostMapping("/batch/start")
    public Map<String, Object> startBatchJob() {
        Map<String, Object> response = new HashMap<>();

        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(importProductJob, jobParameters);

            response.put("status", "success");
            response.put("message", "Batch job started successfully");
            response.put("timestamp", System.currentTimeMillis());

        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Failed to start batch job: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
        }

        return response;
    }

    @PostMapping("/alerts/webhook")
    public Map<String, String> handleAlertWebhook(@RequestBody Map<String, Object> alert) {
        Map<String, String> response = new HashMap<>();
        try {
            log.warn("Received alert: {}", alert);
            
            // Here you can add custom alert handling logic
            // For example, send to external systems, update databases, etc.
            
            response.put("message", "Alert received and processed");
            response.put("status", "success");
            response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        } catch (Exception e) {
            log.error("Error processing alert: {}", e.getMessage());
            response.put("message", "Error processing alert: " + e.getMessage());
            response.put("status", "error");
            response.put("timestamp", String.valueOf(System.currentTimeMillis()));
        }
        return response;
    }

}
