package com.example.batchprocessing;

import static net.logstash.logback.argument.StructuredArguments.kv;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class HttpTraceLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(HttpTraceLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        long startedAt = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            log.info(
                    "HTTP request completed",
                    kv("event", "HTTP_REQUEST"),
                    kv("httpMethod", request.getMethod()),
                    kv("uri", request.getRequestURI()),
                    kv("httpStatus", response.getStatus()),
                    kv("durationMs", TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt))
            );
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/api/");
    }
}
