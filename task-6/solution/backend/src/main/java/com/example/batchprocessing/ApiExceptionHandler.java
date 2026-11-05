package com.example.batchprocessing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);
    private final Tracer tracer;

    public ApiExceptionHandler(Tracer tracer) { this.tracer = tracer; }

    @ExceptionHandler(Exception.class)
    ResponseEntity<JobController.JobResponse> handle(Exception exception) {
        log.error("Не удалось запустить задание", exception);
        Span current = tracer.currentSpan();
        String traceId = current == null ? null : current.context().traceId();
        return ResponseEntity.internalServerError().body(
                new JobController.JobResponse(null, "ERROR", traceId, "Не удалось запустить обработку."));
    }
}
