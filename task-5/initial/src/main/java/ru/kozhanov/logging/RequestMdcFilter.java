package ru.kozhanov.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class RequestMdcFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestMdcFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws java.io.IOException, javax.servlet.ServletException {

        long start = System.currentTimeMillis();
        MDC.put("uri", request.getRequestURI());
        MDC.put("method", request.getMethod());

        try {
            filterChain.doFilter(request, response);
        } finally {
            MDC.put("status", String.valueOf(response.getStatus()));
            MDC.put("durationMs", String.valueOf(System.currentTimeMillis() - start));

            log.info("http_request_completed");

            MDC.remove("uri");
            MDC.remove("method");
            MDC.remove("status");
            MDC.remove("durationMs");
        }
    }
}
