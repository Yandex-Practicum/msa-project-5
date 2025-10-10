package com.example.batchprocessing;

import static org.springframework.batch.core.BatchStatus.COMPLETED;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.stereotype.Component;

@Component
public class JobCompletionNotificationListener implements JobExecutionListener {
    private static final Logger log = LoggerFactory.getLogger(JobCompletionNotificationListener.class);
    private final BatchProcessingJobMetrics batchProcessingJobMetrics;

    public JobCompletionNotificationListener(BatchProcessingJobMetrics batchProcessingJobMetrics) {
        this.batchProcessingJobMetrics = batchProcessingJobMetrics;
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        batchProcessingJobMetrics.incrementActiveJobs();
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        batchProcessingJobMetrics.decrementActiveJobs();

        String jobName = jobExecution.getJobInstance().getJobName();

        if (jobExecution.getStatus() == COMPLETED) {
            batchProcessingJobMetrics.successJobCounterInc(jobName);
        } else {
            batchProcessingJobMetrics.failureJobCounterInc(jobName);
        }

        long totalWriteRecords = jobExecution.getStepExecutions().stream()
            .mapToLong(StepExecution::getReadCount)
            .sum();

        batchProcessingJobMetrics.incBatchCsvRowProcessed(jobName, totalWriteRecords);

        log.info("Job {} finished", jobName);
    }
}
