package com.example.batchprocessing;

import org.slf4j.MDC;

import org.springframework.batch.core.JobParametersInvalidException;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/batch-jobs")
public class BatchJobController {

	private final BatchJobLauncherService batchJobLauncherService;

	public BatchJobController(BatchJobLauncherService batchJobLauncherService) {
		this.batchJobLauncherService = batchJobLauncherService;
	}

	@PostMapping("/import-products")
	public ResponseEntity<BatchLaunchResponse> launchImportProductsJob() {
		try {
			BatchLaunchResult result = batchJobLauncherService.launchImportProductsJob();
			BatchLaunchResponse response = new BatchLaunchResponse(
				result.jobName(),
				result.executionId(),
				result.status(),
				MDC.get("traceId"),
				MDC.get("spanId"),
				MDC.get("uri"),
				result.startedAt(),
				result.endedAt()
			);

			HttpStatus status = "COMPLETED".equals(result.status()) ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
			return ResponseEntity.status(status).body(response);
		}
		catch (JobExecutionAlreadyRunningException | JobInstanceAlreadyCompleteException ex) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, ex.getMessage(), ex);
		}
		catch (JobRestartException | JobParametersInvalidException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
		}
		catch (Exception ex) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage(), ex);
		}
	}
}
