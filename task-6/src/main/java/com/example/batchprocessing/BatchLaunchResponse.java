package com.example.batchprocessing;

import java.time.Instant;

public record BatchLaunchResponse(
	String jobName,
	Long executionId,
	String status,
	String traceId,
	String spanId,
	String uri,
	Instant startedAt,
	Instant endedAt
) {

}
