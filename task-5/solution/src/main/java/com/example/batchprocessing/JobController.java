package com.example.batchprocessing;

import java.util.UUID;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class JobController {
    private final JobLauncher launcher;
    private final Job job;

    public JobController(JobLauncher launcher, Job importProductJob) {
        this.launcher = launcher;
        this.job = importProductJob;
    }

    @PostMapping("/jobs/import")
    public String run() throws Exception {
        JobExecution execution = launcher.run(job, new JobParametersBuilder()
                .addString("run.id", UUID.randomUUID().toString()).toJobParameters());
        return "executionId=" + execution.getId() + ", status=" + execution.getStatus();
    }
}
