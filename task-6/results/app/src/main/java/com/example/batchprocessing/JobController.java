package com.example.batchprocessing;

import java.util.Map;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobController {
    private static final Logger log = LoggerFactory.getLogger(JobController.class);
    private final JobLauncher launcher;
    private final Job job;

    public JobController(JobLauncher launcher, Job importProductJob) {
        this.launcher = launcher;
        this.job = importProductJob;
    }

    @PostMapping("/import-products")
    public Map<String, Object> importProducts(HttpServletRequest request) throws Exception {
        try (MDC.MDCCloseable uri = MDC.putCloseable("uri", request.getRequestURI())) {
            log.info("ETL request received");
            JobExecution execution = launcher.run(job, new JobParametersBuilder()
                .addString("requestId", UUID.randomUUID().toString())
                .toJobParameters());
            log.info("ETL request completed status={} jobExecutionId={}", execution.getStatus(), execution.getId());
            return Map.of("jobExecutionId", execution.getId(), "status", execution.getStatus().name());
        }
    }
}
