package com.example.batchprocessing;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobController {

	private final JobLauncher jobLauncher;
	private final Job importProductJob;

	public JobController(JobLauncher jobLauncher, @Qualifier("importProductJob") Job importProductJob) {
		this.jobLauncher = jobLauncher;
		this.importProductJob = importProductJob;
	}

	@PostMapping("/api/jobs/run")
	public ResponseEntity<String> runJob() throws Exception {
		JobParameters params = new JobParametersBuilder()
				.addLong("runAt", System.currentTimeMillis())
				.toJobParameters();
		JobExecution execution = jobLauncher.run(importProductJob, params);
		return ResponseEntity.ok(execution.getStatus().toString());
	}

}
