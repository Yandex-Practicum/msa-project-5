package com.example.batchprocessing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestLogFilterTest {
    @Test
    void includesPathButNotQueryAndClearsContext() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/jobs/import");
        request.setQueryString("token=secret");
        MockFilterChain chain = new MockFilterChain() {
            @Override
            public void doFilter(jakarta.servlet.ServletRequest req, jakarta.servlet.ServletResponse res) {
                assertEquals("/jobs/import", MDC.get("uri"));
            }
        };
        new RequestLogFilter().doFilter(request, new MockHttpServletResponse(), chain);
        assertNull(MDC.get("uri"));
    }
}
