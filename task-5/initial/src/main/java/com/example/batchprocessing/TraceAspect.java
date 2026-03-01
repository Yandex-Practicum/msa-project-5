package com.example.batchprocessing;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;

@Aspect
@Component
public class TraceAspect {
    
    private static final Logger log = LoggerFactory.getLogger(TraceAspect.class);
    
    @Around("@annotation(org.springframework.web.bind.annotation.PostMapping) || " +
            "@annotation(org.springframework.web.bind.annotation.GetMapping)")
    public Object traceRequest(ProceedingJoinPoint joinPoint) throws Throwable {
        
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;
        
        // Генерируем или получаем traceId
        String traceId = request != null ? request.getHeader("X-Trace-Id") : null;
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        
        // Генерируем spanId
        String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        
        try {
            // Устанавливаем MDC контекст для логирования
            MDC.put("traceId", traceId);
            MDC.put("spanId", spanId);
            if (request != null) {
                MDC.put("uri", request.getRequestURI());
                MDC.put("method", request.getMethod());
            }
            
            log.info("=== INCOMING REQUEST ===");
            log.info("Method: {}, URI: {}", 
                request != null ? request.getMethod() : "UNKNOWN",
                request != null ? request.getRequestURI() : "UNKNOWN");
            log.info("TraceId: {}, SpanId: {}", traceId, spanId);
            log.info("Target: {}.{}", 
                joinPoint.getSignature().getDeclaringTypeName(),
                joinPoint.getSignature().getName());
            
            long startTime = System.currentTimeMillis();
            Object result = joinPoint.proceed();
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("=== REQUEST COMPLETED ===");
            log.info("Duration: {} ms", duration);
            log.info("TraceId: {}, SpanId: {}", traceId, spanId);
            
            return result;
            
        } finally {
            // Очищаем MDC после завершения
            MDC.clear();
        }
    }
}