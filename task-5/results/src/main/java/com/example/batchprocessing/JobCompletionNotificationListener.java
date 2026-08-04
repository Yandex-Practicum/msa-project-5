package com.example.batchprocessing;

import java.time.Duration;

import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.jdbc.core.DataClassRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Логирует итог выполнения джобы и публикует бизнес-метрики ETL в Micrometer:
 * количество прочитанных/записанных/пропущенных записей, статус и длительность запуска.
 * Метрики попадают в Prometheus через /actuator/prometheus.
 */
@Component
public class JobCompletionNotificationListener implements JobExecutionListener {

	private static final Logger log = LoggerFactory.getLogger(JobCompletionNotificationListener.class);

	private final JdbcTemplate jdbcTemplate;
	private final MeterRegistry meterRegistry;

	public JobCompletionNotificationListener(JdbcTemplate jdbcTemplate, MeterRegistry meterRegistry) {
		this.jdbcTemplate = jdbcTemplate;
		this.meterRegistry = meterRegistry;
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		jobExecution.getStepExecutions().forEach(step -> {
			meterRegistry.counter("etl.records.read", "step", step.getStepName())
					.increment(step.getReadCount());
			meterRegistry.counter("etl.records.written", "step", step.getStepName())
					.increment(step.getWriteCount());
			meterRegistry.counter("etl.records.skipped", "step", step.getStepName())
					.increment(step.getSkipCount());
		});
		meterRegistry.counter("etl.job.runs", "job", jobExecution.getJobInstance().getJobName(),
				"status", jobExecution.getStatus().toString()).increment();
		meterRegistry.timer("etl.job.duration", "job", jobExecution.getJobInstance().getJobName())
				.record(Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime()));

		if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
			long elapsedMs = Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime()).toMillis();
			log.info("!!! JOB FINISHED in {} ms! Time to verify the results", elapsedMs);

			jdbcTemplate
					.query("SELECT productId, productSku, productName, productAmount, productData FROM products",
							new DataClassRowMapper<>(Product.class))
					.forEach(product -> log.info("Transformed <{}> in the database.", product));
		} else {
			log.error("!!! JOB FINISHED WITH STATUS {} — check the logs above", jobExecution.getStatus());
		}
	}
}
