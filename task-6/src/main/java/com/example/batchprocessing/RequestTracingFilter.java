package com.example.batchprocessing;

import static net.logstash.logback.argument.StructuredArguments.keyValue;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class RequestTracingFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(RequestTracingFilter.class);
	private static final String TRACE_ID_HEADER = "X-Trace-Id";
	private static final String TRACE_ID_KEY = "traceId";
	private static final String SPAN_ID_KEY = "spanId";
	private static final String URI_KEY = "uri";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
		throws ServletException, IOException {
		String traceId = extractTraceId(request.getHeader(TRACE_ID_HEADER));
		String spanId = generateSpanId();
		String uri = request.getRequestURI();
		long startedAt = System.currentTimeMillis();

		MDC.put(TRACE_ID_KEY, traceId);
		MDC.put(SPAN_ID_KEY, spanId);
		MDC.put(URI_KEY, uri);

		response.setHeader(TRACE_ID_HEADER, traceId);
		response.setHeader("X-Span-Id", spanId);

		log.info(
			"HTTP request started {} {}",
			keyValue("httpMethod", request.getMethod()),
			keyValue("uri", uri)
		);

		try {
			filterChain.doFilter(request, response);
		}
		finally {
			log.info(
				"HTTP request completed {} {} {}",
				keyValue("httpStatus", response.getStatus()),
				keyValue("durationMs", System.currentTimeMillis() - startedAt),
				keyValue("uri", uri)
			);
			MDC.remove(URI_KEY);
			MDC.remove(SPAN_ID_KEY);
			MDC.remove(TRACE_ID_KEY);
		}
	}

	private static String extractTraceId(String traceIdHeader) {
		if (traceIdHeader == null || traceIdHeader.isBlank()) {
			return generateTraceId();
		}

		String normalized = traceIdHeader.replace("-", "").trim().toLowerCase(Locale.ROOT);
		if (normalized.matches("[0-9a-f]{16,32}")) {
			return normalized;
		}

		return generateTraceId();
	}

	private static String generateTraceId() {
		return UUID.randomUUID().toString().replace("-", "");
	}

	private static String generateSpanId() {
		return UUID.randomUUID().toString().replace("-", "").substring(0, 16);
	}
}
