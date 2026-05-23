package com.example.batchprocessing;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class BatchMetricsListener implements StepExecutionListener {

    private final MeterRegistry meterRegistry;
    private long startTime;

    public BatchMetricsListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        startTime = System.currentTimeMillis();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        String stepName = stepExecution.getStepName();
        String jobName = stepExecution.getJobExecution().getJobInstance().getJobName();
        String exitCode = stepExecution.getExitStatus().getExitCode();

        // 1. Timer Metric: Tracks execution duration, total runs, and max time
        Timer.builder("batch.step.duration")
                .description("Time taken to execute the step")
                .tags("job.name", jobName, "step.name", stepName, "status", exitCode)
                .register(meterRegistry)
                .record(duration, TimeUnit.MILLISECONDS);

        // 2. Counter Metrics: Increments processed, filtered, and skipped item counts
        Counter.builder("batch.step.items.read")
                .description("Total number of items read successfully")
                .tags("job.name", jobName, "step.name", stepName)
                .register(meterRegistry)
                .increment(stepExecution.getReadCount());

        Counter.builder("batch.step.items.written")
                .description("Total number of items written successfully")
                .tags("job.name", jobName, "step.name", stepName)
                .register(meterRegistry)
                .increment(stepExecution.getWriteCount());

        // 3. Distribution Summary Metric: Tracks chunk sizes and skip distributions
        if (stepExecution.getSkipCount() > 0) {
            DistributionSummary.builder("batch.step.items.skipped")
                    .description("Distribution of skipped items per step execution")
                    .tags("job.name", jobName, "step.name", stepName)
                    .register(meterRegistry)
                    .record(stepExecution.getSkipCount());
        }

        return stepExecution.getExitStatus();
    }
}