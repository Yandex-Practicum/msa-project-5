package com.example.batchprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class JobCompletionNotificationListener implements JobExecutionListener {
    private static final Logger log = LoggerFactory.getLogger(JobCompletionNotificationListener.class);
    private final JdbcTemplate jdbc;

    public JobCompletionNotificationListener(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void afterJob(JobExecution execution) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM products", Long.class);
        log.info("ETL status={} products={}", execution.getStatus(), count);
    }
}
