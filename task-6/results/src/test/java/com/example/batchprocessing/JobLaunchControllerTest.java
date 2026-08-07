package com.example.batchprocessing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobInstance;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;

class JobLaunchControllerTest {

    @Test
    void launchesJobAndReturnsTraceId() throws Exception {
        JobLauncher launcher = mock(JobLauncher.class);
        Job job = mock(Job.class);
        Tracer tracer = mock(Tracer.class);
        Span span = mock(Span.class);
        TraceContext traceContext = mock(TraceContext.class);
        JobExecution execution = mock(JobExecution.class);
        JobInstance instance = mock(JobInstance.class);

        when(launcher.run(any(Job.class), any(JobParameters.class))).thenReturn(execution);
        when(execution.getId()).thenReturn(42L);
        when(execution.getJobInstance()).thenReturn(instance);
        when(execution.getStatus()).thenReturn(BatchStatus.COMPLETED);
        when(instance.getJobName()).thenReturn("importProductJob");
        when(tracer.currentSpan()).thenReturn(span);
        when(span.context()).thenReturn(traceContext);
        when(traceContext.traceId()).thenReturn("0123456789abcdef0123456789abcdef");

        ResponseEntity<JobLaunchResponse> response =
                new JobLaunchController(launcher, job, tracer).launch();

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).isEqualTo(new JobLaunchResponse(
                42L,
                "importProductJob",
                "COMPLETED",
                "0123456789abcdef0123456789abcdef"
        ));
    }
}
