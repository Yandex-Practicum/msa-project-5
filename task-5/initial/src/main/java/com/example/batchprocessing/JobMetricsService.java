package com.example.batchprocessing;

import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

@Component
public class JobMetricsService {
    private final MeterRegistry meterRegistry;
    private final AtomicInteger activeJobsCount = new AtomicInteger(0);

    public JobMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        initMetrics();
    }

    private void initMetrics() {
        Counter.builder("job.success")
                .description("Successful executions")
                .register(meterRegistry);

        Counter.builder("job.failure")
                .description("Failed executions")
                .register(meterRegistry);

        Gauge.builder("job.count", activeJobsCount, AtomicInteger::get)
                .description("Jobs count")
                .register(meterRegistry);
    }

    public void increaseCounter() {
        activeJobsCount.incrementAndGet();
    }

    public void decreaseCounter() {
        activeJobsCount.decrementAndGet();
    }

    public void success(String job) {
        meterRegistry.counter("job.success").increment();
    }

    public void failure(String job) {
        meterRegistry.counter("job.failure").increment();
    }
}