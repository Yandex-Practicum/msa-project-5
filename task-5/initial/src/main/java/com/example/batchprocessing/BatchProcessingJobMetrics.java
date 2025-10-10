package com.example.batchprocessing;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;

@Component
public class BatchProcessingJobMetrics {
    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeJobsCount = new AtomicInteger(0);

    public BatchProcessingJobMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @PostConstruct
    public void initMetrics() {
        Counter.builder("batch.job.executions.success")
            .description("Number of successful job executions")
            .register(meterRegistry);

        Counter.builder("batch.job.executions.failure")
            .description("Number of failed job executions")
            .register(meterRegistry);

        Gauge.builder("batch.job.active.current", activeJobsCount, AtomicInteger::get)
            .description("Current number of active jobs")
            .register(meterRegistry);

        Counter.builder("batch.csv.rows.processed")
            .description("Number of CSV rows processed")
            .register(meterRegistry);
    }

    public void incrementActiveJobs() {
        activeJobsCount.incrementAndGet();
    }

    public void decrementActiveJobs() {
        activeJobsCount.decrementAndGet();
    }

    public void successJobCounterInc(String jobName) {
        meterRegistry.counter("batch.job.executions.success").increment();
    }

    public void failureJobCounterInc(String jobName) {
        meterRegistry.counter("batch.job.executions.failure").increment();
    }

    public void incBatchCsvRowProcessed(String jobName, long amount) {
        meterRegistry.counter("batch.csv.rows.processed").increment(amount);
    }
}