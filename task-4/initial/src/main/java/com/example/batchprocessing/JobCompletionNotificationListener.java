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
			log.info("!!! JOB FINISHED! Verify the results");
			jdbcTemplate.query("SELECT productId, productSku, productName, productAmount, productData FROM products",
				(rs, row) -> new Product(
						rs.getLong(1),
						rs.getLong(2),
						rs.getString(3),
						rs.getLong(4),
						rs.getString(5)
				)
			).forEach(product -> log.info("Product in db: {}", product));
		} else {
			log.error("!!! JOB FAILED!!!: {}", jobExecution.getStatus());
		}
	}
}
