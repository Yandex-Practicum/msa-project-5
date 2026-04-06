package com.example.batchprocessing;

import java.time.ZoneOffset;

import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.stereotype.Component;

@Component
@ManagedResource(objectName = "com.example.batchprocessing:type=BatchJobMetrics")
public class BatchJobMonitoring {

	private volatile long lastJobDurationMillis;
	private volatile long lastJobReadCount;
	private volatile long lastJobWriteCount;
	private volatile long lastJobSkipCount;
	private volatile int lastJobStatusCode = -1;
	private volatile long lastJobExecutionId = -1;
	private volatile long lastJobStartEpochMillis;
	private volatile long lastJobEndEpochMillis;
	private volatile long lastSuccessfulJobEpochMillis;
	private volatile long lastFailedJobEpochMillis;

	public void recordJobExecution(JobExecution jobExecution) {
		lastJobExecutionId = jobExecution.getId() == null ? -1 : jobExecution.getId();
		lastJobStartEpochMillis = jobExecution.getStartTime() == null
			? 0
			: jobExecution.getStartTime().toInstant(ZoneOffset.UTC).toEpochMilli();
		lastJobEndEpochMillis = jobExecution.getEndTime() == null
			? 0
			: jobExecution.getEndTime().toInstant(ZoneOffset.UTC).toEpochMilli();
		lastJobDurationMillis = Math.max(0, lastJobEndEpochMillis - lastJobStartEpochMillis);
		lastJobReadCount = jobExecution.getStepExecutions().stream().mapToLong(step -> step.getReadCount()).sum();
		lastJobWriteCount = jobExecution.getStepExecutions().stream().mapToLong(step -> step.getWriteCount()).sum();
		lastJobSkipCount = jobExecution.getStepExecutions().stream()
			.mapToLong(step -> step.getReadSkipCount() + step.getProcessSkipCount() + step.getWriteSkipCount())
			.sum();
		lastJobStatusCode = jobExecution.getStatus() == BatchStatus.COMPLETED ? 1 : 0;

		if (lastJobStatusCode == 1) {
			lastSuccessfulJobEpochMillis = lastJobEndEpochMillis;
		} else {
			lastFailedJobEpochMillis = lastJobEndEpochMillis;
		}
	}

	@ManagedAttribute
	public long getLastJobDurationMillis() {
		return lastJobDurationMillis;
	}

	@ManagedAttribute
	public long getLastJobReadCount() {
		return lastJobReadCount;
	}

	@ManagedAttribute
	public long getLastJobWriteCount() {
		return lastJobWriteCount;
	}

	@ManagedAttribute
	public long getLastJobSkipCount() {
		return lastJobSkipCount;
	}

	@ManagedAttribute
	public int getLastJobStatusCode() {
		return lastJobStatusCode;
	}

	@ManagedAttribute
	public long getLastJobExecutionId() {
		return lastJobExecutionId;
	}

	@ManagedAttribute
	public long getLastJobStartEpochMillis() {
		return lastJobStartEpochMillis;
	}

	@ManagedAttribute
	public long getLastJobEndEpochMillis() {
		return lastJobEndEpochMillis;
	}

	@ManagedAttribute
	public long getLastSuccessfulJobEpochMillis() {
		return lastSuccessfulJobEpochMillis;
	}

	@ManagedAttribute
	public long getLastFailedJobEpochMillis() {
		return lastFailedJobEpochMillis;
	}
}
