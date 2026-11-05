package com.example.batchprocessing;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepExecution;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JobCompletionNotificationListenerTest {
    @Test
    void recordsCompletedJobAndItems() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        JobCompletionNotificationListener listener = new JobCompletionNotificationListener(registry);
        JobExecution execution = new JobExecution(new JobInstance(1L, "importProductJob"), 1L, new JobParameters());
        execution.setStatus(BatchStatus.COMPLETED);
        StepExecution step = new StepExecution("importStep", execution);
        step.setReadCount(5);
        step.setWriteCount(5);
        execution.addStepExecutions(java.util.List.of(step));

        listener.afterJob(execution);

        assertEquals(1, registry.get("tradeware.batch.jobs").tag("status", "completed").counter().count());
        assertEquals(5, registry.get("tradeware.batch.items").tag("operation", "written").counter().count());
    }
}
