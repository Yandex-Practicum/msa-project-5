package com.example.batchprocessing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobController {
    private static final Logger log = LoggerFactory.getLogger(JobController.class);
    private final JobLauncher launcher;
    private final Job job;
    private final Tracer tracer;

    public JobController(@Qualifier("synchronousJobLauncher") JobLauncher launcher, Job importProductJob, Tracer tracer) {
        this.launcher = launcher;
        this.job = importProductJob;
        this.tracer = tracer;
    }

    @PostMapping("/jobs/import")
    public ResponseEntity<JobResponse> run() throws Exception {
        Span current = tracer.currentSpan();
        String traceId = current == null ? null : current.context().traceId();
        log.info("Получен запрос на обработку товаров");
        JobExecution execution = launcher.run(job, new JobParametersBuilder()
                .addString("run.id", UUID.randomUUID().toString()).toJobParameters());
        boolean success = execution.getStatus() == BatchStatus.COMPLETED;
        log.info("Запрос обработан: executionId={}, status={}", execution.getId(), execution.getStatus());
        JobResponse response = new JobResponse(execution.getId(), execution.getStatus().name(), traceId,
                success ? null : "Задание завершилось с ошибкой. Подробности доступны в логах.");
        return ResponseEntity.status(success ? 200 : 500).body(response);
    }

    record JobResponse(Long executionId, String status, String traceId, String message) {}
}
