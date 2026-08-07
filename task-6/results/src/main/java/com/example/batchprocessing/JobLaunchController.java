package com.example.batchprocessing;

import java.util.UUID;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/jobs")
public class JobLaunchController {

    private final JobLauncher jobLauncher;
    private final Job importProductJob;
    private final Tracer tracer;

    public JobLaunchController(JobLauncher jobLauncher, Job importProductJob, Tracer tracer) {
        this.jobLauncher = jobLauncher;
        this.importProductJob = importProductJob;
        this.tracer = tracer;
    }

    @PostMapping("/import-products")
    public ResponseEntity<JobLaunchResponse> launch() throws Exception {
        JobParameters parameters = new JobParametersBuilder()
                .addString("requestId", UUID.randomUUID().toString())
                .toJobParameters();
        JobExecution execution = jobLauncher.run(importProductJob, parameters);

        JobLaunchResponse response = new JobLaunchResponse(
                execution.getId(),
                execution.getJobInstance().getJobName(),
                execution.getStatus().name(),
                currentTraceId()
        );
        HttpStatus status = execution.getStatus() == BatchStatus.COMPLETED
                ? HttpStatus.OK
                : HttpStatus.INTERNAL_SERVER_ERROR;
        return ResponseEntity.status(status).body(response);
    }

    private String currentTraceId() {
        Span span = tracer.currentSpan();
        return span == null ? "n/a" : span.context().traceId();
    }
}
