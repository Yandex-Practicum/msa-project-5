package com.example.batchprocessing;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Component
public class StepExecutionLoggingListener implements StepExecutionListener {

	private static final Logger log = LoggerFactory.getLogger(StepExecutionLoggingListener.class);

	@Override
	public void beforeStep(StepExecution stepExecution) {
		log.info(
			"Batch step started {} {} {}",
			keyValue("stepName", stepExecution.getStepName()),
			keyValue("jobExecutionId", stepExecution.getJobExecutionId()),
			keyValue("stepExecutionId", stepExecution.getId())
		);
	}

	@Override
	public ExitStatus afterStep(StepExecution stepExecution) {
		log.info(
			"Batch step finished {} {} {} {} {} {} {}",
			keyValue("stepName", stepExecution.getStepName()),
			keyValue("status", stepExecution.getStatus().name()),
			keyValue("readCount", stepExecution.getReadCount()),
			keyValue("writeCount", stepExecution.getWriteCount()),
			keyValue("filterCount", stepExecution.getFilterCount()),
			keyValue("skipCount", stepExecution.getReadSkipCount() + stepExecution.getProcessSkipCount() + stepExecution.getWriteSkipCount()),
			keyValue("commitCount", stepExecution.getCommitCount())
		);
		return stepExecution.getExitStatus();
	}
}
