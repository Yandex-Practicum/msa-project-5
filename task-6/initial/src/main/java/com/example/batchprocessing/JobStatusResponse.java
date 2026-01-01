package com.example.batchprocessing;

import org.springframework.batch.core.BatchStatus;

public record JobStatusResponse(String jobName, BatchStatus status, String message) { }
