package com.example.batchprocessing;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import java.util.concurrent.TimeUnit;

public class StepMetricsListener implements StepExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(StepMetricsListener.class);
    private final MeterRegistry meterRegistry;
    private Timer.Sample sample;

    public StepMetricsListener(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        sample = Timer.start(meterRegistry);
        meterRegistry.counter("batch.step.started",
                "step", stepExecution.getStepName(),
                "job", stepExecution.getJobExecution().getJobInstance().getJobName()).increment();

        log.info("Step {} started", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        sample.stop(Timer.builder("batch.step.duration")
                .description("Step execution duration")
                .tag("step", stepExecution.getStepName())
                .tag("status", stepExecution.getStatus().toString())
                .register(meterRegistry));

        meterRegistry.counter("batch.step.completed",
                "step", stepExecution.getStepName(),
                "status", stepExecution.getStatus().toString()).increment();

        meterRegistry.gauge("batch.step.read.count",
                stepExecution.getReadCount());
        meterRegistry.gauge("batch.step.write.count",
                stepExecution.getWriteCount());
        meterRegistry.gauge("batch.step.skip.count",
                stepExecution.getSkipCount());

        log.info("Step {} completed with status: {}. Read: {}, Written: {}, Skipped: {}",
                stepExecution.getStepName(),
                stepExecution.getStatus(),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getSkipCount());

        return stepExecution.getExitStatus();
    }
}