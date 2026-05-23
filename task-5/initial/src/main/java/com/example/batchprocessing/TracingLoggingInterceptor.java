package com.example.batchprocessing;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

// Note: If using Spring Boot 2.x, change 'jakarta.servlet' imports to 'javax.servlet'
@Component
public class TracingLoggingInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(TracingLoggingInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // The traceId and spanId are already automatically inside the MDC context here.
        // We just need to log the URI and Method.
        logger.info("Incoming request received | Method: {} | URI: {}", request.getMethod(), request.getRequestURI());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        logger.info("Request completed | URI: {} | Status: {}", request.getRequestURI(), response.getStatus());
    }
}
