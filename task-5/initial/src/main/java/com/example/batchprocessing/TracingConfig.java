package com.example.batchprocessing;

import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.Span;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Configuration
public class TracingConfig {

    private static final Logger log = LoggerFactory.getLogger(TracingConfig.class);

    @Bean
    public OncePerRequestFilter traceContextFilter(Tracer tracer) {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                         FilterChain filterChain) throws ServletException, IOException {
                
                // Получаем trace headers из запроса
                String traceParent = request.getHeader("traceparent");
                String traceId = request.getHeader("X-Trace-Id");
                String spanId = request.getHeader("X-Span-Id");
                
                if (traceId != null && spanId != null) {
                    log.info("Received trace context - traceId: {}, spanId: {}", traceId, spanId);
                    
                    // Создаем новый span как дочерний от переданного
                    Span parentSpan = tracer.nextSpan()
                            .name("http-server-request")
                            .tag("http.method", request.getMethod())
                            .tag("http.url", request.getRequestURL().toString())
                            .tag("parent.traceId", traceId)
                            .tag("parent.spanId", spanId)
                            .start();
                    
                    try {
                        log.info("Processing request with traceId: {}, spanId: {}", 
                                parentSpan.context().traceId(), 
                                parentSpan.context().spanId());
                        filterChain.doFilter(request, response);
                    } finally {
                        parentSpan.end();
                    }
                } else {
                    // Если нет trace headers, создаем новый trace
                    Span newSpan = tracer.nextSpan()
                            .name("http-server-request")
                            .tag("http.method", request.getMethod())
                            .tag("http.url", request.getRequestURL().toString())
                            .start();
                    
                    try {
                        log.info("Processing request with new traceId: {}, spanId: {}", 
                                newSpan.context().traceId(), 
                                newSpan.context().spanId());
                        filterChain.doFilter(request, response);
                    } finally {
                        newSpan.end();
                    }
                }
            }
        };
    }
}
