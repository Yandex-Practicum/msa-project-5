package com.example.batchprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

@Component
public class JobCompletionNotificationListener implements JobExecutionListener {

	private static final Logger log = LoggerFactory.getLogger(JobCompletionNotificationListener.class);

	private final JdbcTemplate jdbcTemplate;

	public JobCompletionNotificationListener(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
	}

    @Override
    public void afterJob(JobExecution jobExecution) {
        Duration duration = Duration.between(Objects.requireNonNull(jobExecution.getStartTime()), jobExecution.getEndTime());
        Long jobId = jobExecution.getJobId();
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("✅ Job COMPLETED successfully. ExecutionId: {}, Duration: {}ms, " +
                            "ReadCount: {}, WriteCount: {}, SkipCount: {}",
                    jobId,
                    duration,
                    jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getReadCount).sum(),
                    jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getWriteCount).sum(),
                    jobExecution.getStepExecutions().stream().mapToLong(StepExecution::getSkipCount).sum());

            jdbcTemplate
                    .query("SELECT productId, productSku, productName, productAmount, productData FROM products", new DataClassRowMapper<>(Product.class))
                    .forEach(product -> log.debug("Processed product: {}", product));
        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
            log.error("💥 Job FAILED. ExecutionId: {}, Duration: {}ms, ExitCode: {}, ExitDescription: {}",
                    jobId,
                    duration,
                    jobExecution.getExitStatus().getExitCode(),
                    jobExecution.getExitStatus().getExitDescription());

            jobExecution.getStepExecutions().forEach(step -> {
                if (step.getStatus() == BatchStatus.FAILED) {
                    log.error("Failed step: {}, ReadCount: {}, WriteCount: {}, SkipCount: {}, " +
                                    "Rollbacks: {}, Failures: {}",
                            step.getStepName(),
                            step.getReadCount(),
                            step.getWriteCount(),
                            step.getSkipCount(),
                            step.getRollbackCount(),
                            step.getFailureExceptions());
                }
            });
        }
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("⚙️ Job STARTING. ExecutionId: {}, JobName: {}, Parameters: {}",
                jobExecution.getJobId(),
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getJobParameters());
    }
}
