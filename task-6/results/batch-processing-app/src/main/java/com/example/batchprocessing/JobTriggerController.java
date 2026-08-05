package com.example.batchprocessing;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobTriggerController {

	private static final Logger log = LoggerFactory.getLogger(JobTriggerController.class);

	private final JobLauncher jobLauncher;
	private final Job importProductJob;

	public JobTriggerController(JobLauncher jobLauncher, Job importProductJob) {
		this.jobLauncher = jobLauncher;
		this.importProductJob = importProductJob;
	}

	@PostMapping("/jobs/import-product")
	public ResponseEntity<String> triggerImportProductJob(HttpServletRequest request) {
		String uri = request.getRequestURI();
		log.info("Received request to trigger Job, uri={}", uri);

		JobParameters jobParameters = new JobParametersBuilder()
			.addLong("run.id", System.currentTimeMillis())
			.toJobParameters();

		try {
			JobExecution execution = jobLauncher.run(importProductJob, jobParameters);
			log.info("Job triggered, uri={}, status={}", uri, execution.getStatus());
			return ResponseEntity.ok("Job status: " + execution.getStatus());
		} catch (JobExecutionAlreadyRunningException | JobRestartException
				| JobInstanceAlreadyCompleteException | JobParametersInvalidException e) {
			log.error("Failed to launch Job, uri={}", uri, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Job launch failed: " + e.getMessage());
		}
	}

}
