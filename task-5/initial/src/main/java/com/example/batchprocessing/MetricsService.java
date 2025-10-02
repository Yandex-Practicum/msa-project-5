package com.example.batchprocessing;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicLong;

@Service
public class MetricsService {

    private final MeterRegistry meterRegistry;
    private final Counter productsProcessedCounter;
    private final Counter productsWithLoyaltyUpdatedCounter;
    private final Counter productsWithLoyaltyKeptCounter;
    private final Counter batchJobCompletedCounter;
    private final Counter batchJobFailedCounter;
    private final Timer productProcessingTimer;
    private final AtomicLong totalProductsProcessed = new AtomicLong(0);
    private final AtomicLong totalProductsWithLoyaltyUpdated = new AtomicLong(0);
    private final AtomicLong totalProductsWithLoyaltyKept = new AtomicLong(0);

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Counters for different events
        this.productsProcessedCounter = Counter.builder("batch_products_processed_total")
                .description("Total number of products processed")
                .tag("application", "batch-processing")
                .register(meterRegistry);
                
        this.productsWithLoyaltyUpdatedCounter = Counter.builder("batch_products_loyalty_updated_total")
                .description("Total number of products with loyalty data updated")
                .tag("application", "batch-processing")
                .register(meterRegistry);
                
        this.productsWithLoyaltyKeptCounter = Counter.builder("batch_products_loyalty_kept_total")
                .description("Total number of products with loyalty data kept unchanged")
                .tag("application", "batch-processing")
                .register(meterRegistry);
                
        this.batchJobCompletedCounter = Counter.builder("batch_job_completed_total")
                .description("Total number of batch jobs completed successfully")
                .tag("application", "batch-processing")
                .register(meterRegistry);
                
        this.batchJobFailedCounter = Counter.builder("batch_job_failed_total")
                .description("Total number of batch jobs failed")
                .tag("application", "batch-processing")
                .register(meterRegistry);
        
        // Timer for measuring processing time
        this.productProcessingTimer = Timer.builder("batch_product_processing_duration_seconds")
                .description("Time taken to process a single product")
                .tag("application", "batch-processing")
                .register(meterRegistry);
        
        // Note: Gauges are registered automatically when accessed
    }

    public void incrementProductsProcessed() {
        productsProcessedCounter.increment();
        totalProductsProcessed.incrementAndGet();
    }

    public void incrementProductsWithLoyaltyUpdated() {
        productsWithLoyaltyUpdatedCounter.increment();
        totalProductsWithLoyaltyUpdated.incrementAndGet();
    }

    public void incrementProductsWithLoyaltyKept() {
        productsWithLoyaltyKeptCounter.increment();
        totalProductsWithLoyaltyKept.incrementAndGet();
    }

    public void incrementBatchJobCompleted() {
        batchJobCompletedCounter.increment();
    }

    public void incrementBatchJobFailed() {
        batchJobFailedCounter.increment();
    }

    public Timer.Sample startProductProcessingTimer() {
        return Timer.start(meterRegistry);
    }

    public void recordProductProcessingTime(Timer.Sample sample) {
        sample.stop(productProcessingTimer);
    }

    public long getTotalProductsProcessed() {
        return totalProductsProcessed.get();
    }

    public long getTotalProductsWithLoyaltyUpdated() {
        return totalProductsWithLoyaltyUpdated.get();
    }

    public long getTotalProductsWithLoyaltyKept() {
        return totalProductsWithLoyaltyKept.get();
    }

}
