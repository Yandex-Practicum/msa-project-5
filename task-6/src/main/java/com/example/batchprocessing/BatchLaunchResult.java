package com.example.batchprocessing;

import java.time.Instant;

public record BatchLaunchResult(Long executionId, String jobName, String status, Instant startedAt, Instant endedAt) {

}
