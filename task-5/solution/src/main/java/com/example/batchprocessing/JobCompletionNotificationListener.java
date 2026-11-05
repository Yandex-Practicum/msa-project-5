package com.example.batchprocessing;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class JobCompletionNotificationListener implements JobExecutionListener {
    private static final Logger log = LoggerFactory.getLogger(JobCompletionNotificationListener.class);
    private final Counter completed;
    private final Counter failed;
    private final Counter read;
    private final Counter written;
    private final Timer duration;

    public JobCompletionNotificationListener(MeterRegistry registry) {
        completed = registry.counter("tradeware.batch.jobs", "status", "completed");
        failed = registry.counter("tradeware.batch.jobs", "status", "failed");
        read = registry.counter("tradeware.batch.items", "operation", "read");
        written = registry.counter("tradeware.batch.items", "operation", "written");
        duration = registry.timer("tradeware.batch.duration");
    }

    @Override
    public void beforeJob(JobExecution execution) {
        log.info("Задание {} запущено, executionId={}", execution.getJobInstance().getJobName(), execution.getId());
    }

    @Override
    public void afterJob(JobExecution execution) {
        if (execution.getStatus() == BatchStatus.COMPLETED) completed.increment();
        else if (execution.getStatus() == BatchStatus.FAILED) failed.increment();
        execution.getStepExecutions().forEach(step -> {
            read.increment(step.getReadCount());
            written.increment(step.getWriteCount());
        });
        if (execution.getStartTime() != null && execution.getEndTime() != null) {
            duration.record(java.time.Duration.between(execution.getStartTime(), execution.getEndTime()));
        }
        log.info("Задание {} завершено: {}, executionId={}, exitDescription={}", execution.getJobInstance().getJobName(), execution.getStatus(), execution.getId(), execution.getExitStatus().getExitDescription());
    }
}
