package com.example.batchprocessing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * REST API для запуска ETL-джобы. Каждый вызов логируется с traceId/spanId (Micrometer Tracing
 * кладёт их в MDC) и uri запроса (см. RequestTracingFilter).
 */
@RestController
@RequestMapping("/api/etl")
public class JobLaunchController {

	private static final Logger log = LoggerFactory.getLogger(JobLaunchController.class);

	private final JobLauncher jobLauncher;
	private final Job importProductJob;
	private final JdbcTemplate jdbcTemplate;

	public JobLaunchController(JobLauncher jobLauncher, Job importProductJob, JdbcTemplate jdbcTemplate) {
		this.jobLauncher = jobLauncher;
		this.importProductJob = importProductJob;
		this.jdbcTemplate = jdbcTemplate;
	}

	@GetMapping("/status")
	public Map<String, Object> status() {
		log.info("Status check requested");
		return Map.of("service", "batch-processing", "state", "UP");
	}

	@PostMapping("/run")
	public ResponseEntity<Map<String, Object>> runImportJob() {
		log.info("Запрошен запуск ETL-джобы {} через API", importProductJob.getName());

		try {
			jdbcTemplate.execute("TRUNCATE TABLE products");
			log.info("Таблица products очищена перед загрузкой нового отчёта");

			// Уникальный параметр — иначе Spring Batch откажется перезапускать успешно завершённую джобу
			JobParameters params = new JobParametersBuilder()
					.addLong("launchedAt", System.currentTimeMillis())
					.toJobParameters();

			JobExecution execution = jobLauncher.run(importProductJob, params);

			log.info("Джоба завершилась: executionId={}, status={}", execution.getId(), execution.getStatus());

			Map<String, Object> body = new LinkedHashMap<>();
			body.put("jobName", importProductJob.getName());
			body.put("executionId", execution.getId());
			body.put("status", execution.getStatus().toString());
			body.put("traceId", MDC.get("traceId"));
			return ResponseEntity.ok(body);
		} catch (Exception e) {
			log.error("Ошибка при запуске ETL-джобы", e);
			return ResponseEntity.internalServerError().body(Map.of("error", String.valueOf(e.getMessage())));
		}
	}
}
