package com.example.batchprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.JobParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;

@Component
public class TracingJobExecutionListener implements JobExecutionListener {
    
    private static final Logger log = LoggerFactory.getLogger(TracingJobExecutionListener.class);
    
    @Override
    public void beforeJob(JobExecution jobExecution) {
        JobParameters params = jobExecution.getJobParameters();
        
        String traceId = params != null ? params.getString("traceId") : null;
        String spanId = params != null ? params.getString("spanId") : null;
        
        if (traceId != null) {
            MDC.put("traceId", traceId);
        }
        if (spanId != null) {
            MDC.put("spanId", spanId + "-job");
        }
        
        log.info("=== JOB STARTED ===");
        log.info("Job name: {}", jobExecution.getJobInstance().getJobName());
        log.info("Job parameters: {}", params);
    }
    
    @Override
    public void afterJob(JobExecution jobExecution) {
        log.info("=== JOB FINISHED ===");
        log.info("Job status: {}", jobExecution.getStatus());
        
        LocalDateTime startTime = jobExecution.getStartTime();
        LocalDateTime endTime = jobExecution.getEndTime();
        
        if (startTime != null && endTime != null) {
            Duration duration = Duration.between(startTime, endTime);
            log.info("Job duration: {} ms", duration.toMillis());
        } else {
            log.info("Job duration: unknown");
        }
        
        if (jobExecution.getStatus() == BatchStatus.FAILED) {
            log.error("Job failed with exceptions: {}", jobExecution.getAllFailureExceptions());
        }
        
        MDC.clear();
    }
}