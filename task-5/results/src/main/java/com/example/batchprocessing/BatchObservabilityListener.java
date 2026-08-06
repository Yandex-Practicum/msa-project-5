package com.example.batchprocessing;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.time.Duration;
import java.time.LocalDateTime;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class BatchObservabilityListener implements JobExecutionListener, StepExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(BatchObservabilityListener.class);

    private final MeterRegistry meterRegistry;
    private final JdbcTemplate jdbcTemplate;

    public BatchObservabilityListener(MeterRegistry meterRegistry, JdbcTemplate jdbcTemplate) {
        this.meterRegistry = meterRegistry;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info(
                "Batch job started",
                kv("event", "JOB_STARTED"),
                kv("jobName", jobName(jobExecution)),
                kv("jobExecutionId", jobExecution.getId()),
                kv("runId", runId(jobExecution)),
                kv("status", jobExecution.getStatus().name())
        );
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobName(jobExecution);
        String status = jobExecution.getStatus().name();
        long readCount = jobExecution.getStepExecutions().stream()
                .mapToLong(StepExecution::getReadCount)
                .sum();
        long writeCount = jobExecution.getStepExecutions().stream()
                .mapToLong(StepExecution::getWriteCount)
                .sum();
        long skipCount = jobExecution.getStepExecutions().stream()
                .mapToLong(StepExecution::getSkipCount)
                .sum();
        Duration duration = duration(jobExecution.getStartTime(), jobExecution.getEndTime());

        Counter.builder("batch.job.executions")
                .description("Number of completed Spring Batch job executions")
                .tag("job_name", jobName)
                .tag("status", status)
                .register(meterRegistry)
                .increment();

        Timer.builder("batch.job.duration")
                .description("Spring Batch job duration")
                .tag("job_name", jobName)
                .tag("status", status)
                .register(meterRegistry)
                .record(duration);

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            Long productCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Long.class);
            log.info(
                    "Batch job completed",
                    kv("event", "JOB_COMPLETED"),
                    kv("jobName", jobName),
                    kv("jobExecutionId", jobExecution.getId()),
                    kv("runId", runId(jobExecution)),
                    kv("status", status),
                    kv("readCount", readCount),
                    kv("writeCount", writeCount),
                    kv("skipCount", skipCount),
                    kv("durationMs", duration.toMillis()),
                    kv("productCount", productCount)
            );
            return;
        }

        Throwable failure = jobExecution.getAllFailureExceptions().stream().findFirst().orElse(null);
        if (failure == null) {
            log.error(
                    "Batch job failed",
                    kv("event", "JOB_FAILED"),
                    kv("jobName", jobName),
                    kv("jobExecutionId", jobExecution.getId()),
                    kv("runId", runId(jobExecution)),
                    kv("status", status),
                    kv("readCount", readCount),
                    kv("writeCount", writeCount),
                    kv("skipCount", skipCount),
                    kv("durationMs", duration.toMillis())
            );
            return;
        }

        log.error(
                "Batch job failed",
                kv("event", "JOB_FAILED"),
                kv("jobName", jobName),
                kv("jobExecutionId", jobExecution.getId()),
                kv("runId", runId(jobExecution)),
                kv("status", status),
                kv("readCount", readCount),
                kv("writeCount", writeCount),
                kv("skipCount", skipCount),
                kv("durationMs", duration.toMillis()),
                kv("errorType", failure.getClass().getSimpleName()),
                kv("errorMessage", failure.getMessage()),
                failure
        );
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
        String stepName = stepExecution.getStepName();

        increment("batch.step.items.read", "Number of items read by a Spring Batch step",
                jobName, stepName, stepExecution.getReadCount());
        increment("batch.step.items.written", "Number of items written by a Spring Batch step",
                jobName, stepName, stepExecution.getWriteCount());
        increment("batch.step.items.skipped", "Number of items skipped by a Spring Batch step",
                jobName, stepName, stepExecution.getSkipCount());

        log.info(
                "Batch step completed",
                kv("event", "STEP_COMPLETED"),
                kv("jobName", jobName),
                kv("jobExecutionId", stepExecution.getJobExecutionId()),
                kv("runId", runId(stepExecution.getJobExecution())),
                kv("stepName", stepName),
                kv("status", stepExecution.getStatus().name()),
                kv("readCount", stepExecution.getReadCount()),
                kv("writeCount", stepExecution.getWriteCount()),
                kv("skipCount", stepExecution.getSkipCount()),
                kv("durationMs", duration(stepExecution.getStartTime(), stepExecution.getEndTime()).toMillis())
        );
        return null;
    }

    private void increment(String name, String description, String jobName, String stepName, long value) {
        Counter.builder(name)
                .description(description)
                .tag("job_name", jobName)
                .tag("step_name", stepName)
                .register(meterRegistry)
                .increment(value);
    }

    private static Duration duration(LocalDateTime start, LocalDateTime end) {
        if (start == null || end == null || end.isBefore(start)) {
            return Duration.ZERO;
        }
        return Duration.between(start, end);
    }

    private static String jobName(JobExecution jobExecution) {
        return jobExecution.getJobInstance().getJobName();
    }

    private static String runId(JobExecution jobExecution) {
        Long runId = jobExecution.getJobParameters().getLong("run.id");
        return runId == null ? "n/a" : runId.toString();
    }
}
