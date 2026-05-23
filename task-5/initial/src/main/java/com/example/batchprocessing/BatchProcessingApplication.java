package com.example.batchprocessing;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController // Adds the web controller directly here
public class BatchProcessingApplication {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job importProductJob; // Make sure this matches your defined batch job bean name

    public static void main(String[] args) {
        SpringApplication.run(BatchProcessingApplication.class, args);
    }

    // 1. SIMPLE GET TEST (To verify web hosting works)
    @GetMapping("/test")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Web server is running and reachable!");
    }

    // 2. THE ACTUAL JOB TRIGGER
    @PostMapping("/api/batch/start")
    public ResponseEntity<String> startJob() {
        try {
            JobParameters params = new JobParametersBuilder()
                    .addLong("time", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(importProductJob, params);
            return ResponseEntity.ok("Batch job started successfully.");
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Job failed: " + e.getMessage());
        }
    }
}
