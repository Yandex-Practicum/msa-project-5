package ru.kozhanov.api;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.sleuth.Tracer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/etl")
public class EtlRunController {

    private static final Logger log = LoggerFactory.getLogger(EtlRunController.class);

    private final JobLauncher jobLauncher;
    private final Job etlJob;
    private final Tracer tracer;

    public EtlRunController(JobLauncher jobLauncher,
                            @Qualifier("productEtlJob") Job etlJob,
                            Tracer tracer) {
        this.jobLauncher = jobLauncher;
        this.etlJob = etlJob;
        this.tracer = tracer;
    }

    @PostMapping("/run")
    public ResponseEntity<Map<String, Object>> run() throws Exception {
        String traceId = tracer.currentSpan() != null ? tracer.currentSpan().context().traceId() : null;
        String spanId  = tracer.currentSpan() != null ? tracer.currentSpan().context().spanId() : null;

        JobParameters params = new JobParametersBuilder()
                .addLong("run.id", Instant.now().toEpochMilli())
                .toJobParameters();

        log.info("ETL trigger received. Starting job...");

        JobExecution exec = jobLauncher.run(etlJob, params);

        log.info("ETL job started. jobExecutionId={} status={}", exec.getId(), exec.getStatus());

        Map<String, Object> resp = new HashMap<>();
        resp.put("jobExecutionId", exec.getId());
        resp.put("status", exec.getStatus().toString());
        resp.put("traceId", traceId);
        resp.put("spanId", spanId);

        return ResponseEntity.ok(resp);
    }
}
