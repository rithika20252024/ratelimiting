package com.rateshield.filter;

import com.rateshield.model.RateLimitResponse;
import com.rateshield.service.RateLimitService;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class RateLimitFilter implements Filter {

    private final RateLimitService rateLimitService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = httpRequest.getRemoteAddr();
        String requestPath = httpRequest.getRequestURI();

        if (requestPath.equals("/") || 
            requestPath.endsWith(".html") || requestPath.endsWith(".css") || requestPath.endsWith(".js") || requestPath.endsWith(".ico") ||
            requestPath.startsWith("/h2-console") || 
            requestPath.startsWith("/management/config") || 
            requestPath.startsWith("/management/dashboard")) {
            chain.doFilter(request, response);
            return;
        }

        log.debug("Checking rate limit for IP: {} on path: {}", clientIp, requestPath);
        RateLimitResponse rateLimitResponse = rateLimitService.checkRateLimit(clientIp, requestPath);

        if (rateLimitResponse.isAllowed()) {
            httpResponse.setHeader("X-RateLimit-Remaining", String.valueOf(rateLimitResponse.getRemainingRequests()));
            httpResponse.setHeader("X-RateLimit-Algorithm", rateLimitResponse.getAlgorithm());
            chain.doFilter(request, response);
        } else {
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            String errorJson = String.format("{\"error\": \"%s\", \"algorithm\": \"%s\"}", 
                    rateLimitResponse.getMessage(), rateLimitResponse.getAlgorithm());
            httpResponse.getWriter().write(errorJson);
        }
    }
}
