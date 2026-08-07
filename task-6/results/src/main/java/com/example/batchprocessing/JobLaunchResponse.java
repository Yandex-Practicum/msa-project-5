package com.example.batchprocessing;

public record JobLaunchResponse(
        Long jobExecutionId,
        String jobName,
        String status,
        String traceId
) {
}
