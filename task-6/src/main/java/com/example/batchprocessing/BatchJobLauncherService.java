package com.example.batchprocessing;

import java.time.Instant;

import org.slf4j.MDC;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Service;

@Service
public class BatchJobLauncherService {

	private final JobLauncher jobLauncher;
	private final Job importProductJob;

	public BatchJobLauncherService(JobLauncher jobLauncher, Job importProductJob) {
		this.jobLauncher = jobLauncher;
		this.importProductJob = importProductJob;
	}

	public BatchLaunchResult launchImportProductsJob() throws Exception {
		JobParameters parameters = new JobParametersBuilder()
			.addLong("triggeredAt", System.currentTimeMillis())
			.addString("requestTraceId", defaultValue(MDC.get("traceId")))
			.addString("requestUri", defaultValue(MDC.get("uri")))
			.toJobParameters();

		JobExecution jobExecution = jobLauncher.run(importProductJob, parameters);

		return new BatchLaunchResult(
			jobExecution.getId(),
			jobExecution.getJobInstance().getJobName(),
			jobExecution.getStatus().name(),
			asInstant(jobExecution.getStartTime()),
			asInstant(jobExecution.getEndTime())
		);
	}

	private static String defaultValue(String value) {
		return value == null || value.isBlank() ? "n/a" : value;
	}

	private static Instant asInstant(java.time.LocalDateTime dateTime) {
		return dateTime == null ? null : dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant();
	}
}
