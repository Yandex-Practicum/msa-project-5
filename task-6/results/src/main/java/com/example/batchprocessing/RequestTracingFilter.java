package com.example.batchprocessing;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Кладёт URI и HTTP-метод запроса в MDC, чтобы каждая строка лога
 * содержала контекст запроса: uri + traceId/spanId (последние добавляет
 * Micrometer Tracing). JSON-encoder выгружает весь MDC в поля события.
 */
@Component
public class RequestTracingFilter extends OncePerRequestFilter {

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
									FilterChain filterChain) throws ServletException, java.io.IOException {
		MDC.put("uri", request.getRequestURI());
		MDC.put("httpMethod", request.getMethod());
		try {
			filterChain.doFilter(request, response);
		} finally {
			MDC.remove("uri");
			MDC.remove("httpMethod");
		}
	}
}
