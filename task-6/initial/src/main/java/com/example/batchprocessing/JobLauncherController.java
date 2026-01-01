package com.example.batchprocessing;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/api/batch")
public class JobLauncherController {

    private final JobLauncher jobLauncher;
    private final Job importProductJob;
    private final JobExplorer jobExplorer;

    @Autowired
    public JobLauncherController(JobLauncher jobLauncher, Job importProductJob, JobExplorer jobExplorer) {
        this.jobLauncher = jobLauncher;
        this.importProductJob = importProductJob;
        this.jobExplorer = jobExplorer;
    }

    // Запуск Job через GET
    @GetMapping("/run")
    public ResponseEntity<JobStatusResponse> runJob() {
        try {
            JobParameters jobParameters = new JobParametersBuilder()
                    .addDate("startAt", new Date())
                    .toJobParameters();

            JobExecution execution = jobLauncher.run(importProductJob, jobParameters);

            return ResponseEntity.ok(new JobStatusResponse(
                    execution.getJobInstance().getJobName(),
                    execution.getStatus(),
                    "Job started successfully. ExecutionId: " + execution.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new JobStatusResponse(
                    importProductJob.getName(),
                    null,
                    "Job failed to start: " + e.getMessage()
            ));
        }
    }

    // Получение статуса Job по executionId
    @GetMapping("/status/{executionId}")
    public ResponseEntity<JobStatusResponse> getJobStatus(@PathVariable Long executionId) {
        try {
            JobExecution execution = jobExplorer.getJobExecution(executionId);
            if (execution == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(new JobStatusResponse(
                    execution.getJobInstance().getJobName(),
                    execution.getStatus(),
                    "Job executionId: " + execution.getId()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(new JobStatusResponse(
                    importProductJob.getName(),
                    null,
                    "Failed to retrieve job status: " + e.getMessage()
            ));
        }
    }
}
