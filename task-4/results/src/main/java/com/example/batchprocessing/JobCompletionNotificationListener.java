package com.example.batchprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobCompletionNotificationListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(JobCompletionNotificationListener.class);

    private final JdbcTemplate jdbcTemplate;

    public JobCompletionNotificationListener(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        long readCount = jobExecution.getStepExecutions().stream()
                .mapToLong(step -> step.getReadCount())
                .sum();
        long writeCount = jobExecution.getStepExecutions().stream()
                .mapToLong(step -> step.getWriteCount())
                .sum();
        long skipCount = jobExecution.getStepExecutions().stream()
                .mapToLong(step -> step.getSkipCount())
                .sum();

        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            Long productCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM products", Long.class);
            log.info(
                    "JOB_COMPLETED job={} read={} written={} skipped={} products={}",
                    jobExecution.getJobInstance().getJobName(),
                    readCount,
                    writeCount,
                    skipCount,
                    productCount
            );
        } else {
            log.error(
                    "JOB_FAILED job={} status={} read={} written={} skipped={} exitDescription={}",
                    jobExecution.getJobInstance().getJobName(),
                    jobExecution.getStatus(),
                    readCount,
                    writeCount,
                    skipCount,
                    jobExecution.getExitStatus().getExitDescription()
            );
        }
    }
}
