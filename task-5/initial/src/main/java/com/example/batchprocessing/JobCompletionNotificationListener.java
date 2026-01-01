package com.example.batchprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
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
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("!!! JOB FINISHED! Waiting 30 seconds before verifying results...");

            try {
                Thread.sleep(60_000); // 60 секунд;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Sleep was interrupted", e);
                return;
            }

            log.info("30 seconds passed. Verifying the results in the database");

            jdbcTemplate
                    .query(
                            "SELECT productId, productSku, productName, productAmount, productData FROM products",
                            new DataClassRowMapper<>(Product.class)
                    )
                    .forEach(product ->
                            log.info("Transformed <{}> in the database.", product)
                    );
        }
    }
}
