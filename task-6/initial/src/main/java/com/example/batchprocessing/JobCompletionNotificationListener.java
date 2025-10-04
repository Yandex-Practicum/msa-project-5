package com.example.batchprocessing;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class JobCompletionNotificationListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(JobCompletionNotificationListener.class);

    private final JdbcTemplate jdbcTemplate;
    private final MeterRegistry meterRegistry;
    private volatile long startTime;

    public JobCompletionNotificationListener(JdbcTemplate jdbcTemplate, MeterRegistry meterRegistry) {
        this.jdbcTemplate = jdbcTemplate;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        startTime = System.currentTimeMillis();
        log.info("Job {} started at {}", jobExecution.getJobInstance().getJobName(), startTime);
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        long duration = System.currentTimeMillis() - startTime;
        String jobName = jobExecution.getJobInstance().getJobName();
        BatchStatus status = jobExecution.getStatus();

        Timer timer = Timer.builder("batch.job.duration")
                .description("Duration of Spring Batch job execution")
                .tags("job_name", jobName, "status", status.toString())
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.95)
                .register(meterRegistry);
        timer.record(duration, TimeUnit.MILLISECONDS);

        log.info("Job {} finished with status {} in {} ms", jobName, status, duration);

        if (status == BatchStatus.COMPLETED) {
            log.info("!!! JOB FINISHED! Verify the results");
            jdbcTemplate.query("SELECT productId, productSku, productName, productAmount, productData FROM products",
                new DataClassRowMapper<>(Product.class)
            ).forEach(product -> log.info("Product in db: {}", product));
        } else {
            log.error("!!! JOB FAILED!!!: {}", status);
        }
    }
	
}