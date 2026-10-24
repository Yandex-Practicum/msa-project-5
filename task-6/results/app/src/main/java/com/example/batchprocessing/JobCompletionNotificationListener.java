package com.example.batchprocessing;

import java.time.Duration;
import io.micrometer.core.instrument.MeterRegistry;
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
    private final MeterRegistry metrics;

    public JobCompletionNotificationListener(JdbcTemplate jdbc, MeterRegistry metrics) {
        this.jdbc = jdbc;
        this.metrics = metrics;
    }

    @Override
    public void afterJob(JobExecution execution) {
        Long count = jdbc.queryForObject("SELECT COUNT(*) FROM products", Long.class);
        log.info("ETL status={} products={}", execution.getStatus(), count);
        metrics.counter("batch_job_runs_total", "status", execution.getStatus().name()).increment();
        if (execution.getStartTime() != null && execution.getEndTime() != null) {
            metrics.timer("batch_job_duration").record(Duration.between(execution.getStartTime(), execution.getEndTime()));
        }
    }
}
