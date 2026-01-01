package com.example.batchprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;

import org.springframework.jdbc.core.DataClassRowMapper;
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
    public void beforeJob(JobExecution jobExecution) {
        log.info("Job {} is starting...", jobExecution.getJobInstance().getJobName());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("!!! JOB FINISHED! Verifying results in the database...");

            jdbcTemplate.query(
                    "SELECT productId, productSku, productName, productAmount, productData FROM products",
                    new DataClassRowMapper<>(Product.class)
            ).forEach(product -> log.info("Saved to DB: {}", product));
        } else if (jobExecution.getStatus() == BatchStatus.FAILED) {
            log.warn("Job {} failed with {} failure(s).", jobExecution.getJobInstance().getJobName(),
                    jobExecution.getAllFailureExceptions().size());
        }
    }
}
