package com.example.batchprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private static final Logger log = LoggerFactory.getLogger(JobController.class);

    private final JobLauncher jobLauncher;
    private final Job importProductJob;
    private final JdbcTemplate jdbcTemplate;

    public JobController(JobLauncher jobLauncher, Job importProductJob, JdbcTemplate jdbcTemplate) {
        this.jobLauncher = jobLauncher;
        this.importProductJob = importProductJob;
        this.jdbcTemplate = jdbcTemplate;
    }

    @PostMapping("/import-products")
    public ResponseEntity<Map<String, Object>> launchImportJob() {
        log.info("Received request to launch importProductJob");

        try {
            jdbcTemplate.execute("TRUNCATE TABLE products");
            log.info("Cleared products table before job run");
            JobParameters params = new JobParametersBuilder()
                    .addLong("startedAt", System.currentTimeMillis())
                    .toJobParameters();

            var execution = jobLauncher.run(importProductJob, params);

            log.info("Job launched successfully, executionId={}, status={}",
                    execution.getId(), execution.getStatus());

            return ResponseEntity.ok(Map.of(
                    "jobId", execution.getId(),
                    "status", execution.getStatus().toString()
            ));
        } catch (Exception e) {
            log.error("Failed to launch job", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", e.getMessage()
            ));
        }
    }
}
