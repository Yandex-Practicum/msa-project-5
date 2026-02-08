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

	private final JobMetricsService jobMetricsService;

	public JobCompletionNotificationListener(JobMetricsService jobMetricsService) {
		this.jobMetricsService = jobMetricsService;
	}

	@Override
	public void beforeJob(JobExecution jobExecution) {
		String jobName = jobExecution.getJobInstance().getJobName();
		log.info("Starting job: {}", jobName);
		jobMetricsService.increaseCounter();
	}

	@Override
	public void afterJob(JobExecution jobExecution) {
		jobMetricsService.decreaseCounter();
		String job = jobExecution.getJobInstance().getJobName();

		if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
			jobMetricsService.success(job);
		} else {
			jobMetricsService.failure(job);
		}
		log.info("Job {} completed", job);
	}
}
