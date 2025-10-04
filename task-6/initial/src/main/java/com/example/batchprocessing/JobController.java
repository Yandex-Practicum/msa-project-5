package com.example.batchprocessing;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private Job importProductJob;

    @PostMapping("/run")
    public String runJob() throws Exception {
        JobParameters params = new JobParametersBuilder()
                .addLong("startTime", System.currentTimeMillis())
                .addString("uniqueId", String.valueOf(System.nanoTime()))
                .toJobParameters();
        jobLauncher.run(importProductJob, params);
        return "Job importProductJob started";
    }
}