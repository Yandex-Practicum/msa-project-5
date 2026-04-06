package com.example.batchprocessing;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

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
	private final BatchJobMonitoring batchJobMonitoring;

	public JobCompletionNotificationListener(JdbcTemplate jdbcTemplate, BatchJobMonitoring batchJobMonitoring) {
		this.jdbcTemplate = jdbcTemplate;
		this.batchJobMonitoring = batchJobMonitoring;
	}

	@Override
	public void beforeJob(JobExecution jobExecution) {
		log.info(
			"Batch job started {} {}",
			keyValue("jobName", jobExecution.getJobInstance().getJobName()),
			keyValue("executionId", jobExecution.getId())
		);
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		batchJobMonitoring.recordJobExecution(jobExecution);

		log.info(
			"Batch job finished {} {} {} {} {}",
			keyValue("jobName", jobExecution.getJobInstance().getJobName()),
			keyValue("executionId", jobExecution.getId()),
			keyValue("status", jobExecution.getStatus().name()),
			keyValue("readCount", batchJobMonitoring.getLastJobReadCount()),
			keyValue("writeCount", batchJobMonitoring.getLastJobWriteCount())
		);

		if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
			log.info("Batch job completed successfully. Verifying products table contents.");

			jdbcTemplate
				.query(
					"SELECT productId, productSku, productName, productAmount, productData FROM products ORDER BY productId",
					new DataClassRowMapper<>(Product.class)
				)
				.forEach(product -> log.info("Loaded product into database {}", keyValue("product", product)));
			return;
		}

		log.error(
			"Batch job finished with unexpected status {} {}",
			keyValue("jobName", jobExecution.getJobInstance().getJobName()),
			keyValue("status", jobExecution.getStatus().name())
		);
	}
}
