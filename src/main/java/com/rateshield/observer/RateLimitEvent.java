package com.rateshield.observer;

import org.springframework.context.ApplicationEvent;

public class RateLimitEvent extends ApplicationEvent {

    private final String clientIp;
    private final String requestPath;
    private final String algorithm;

    public RateLimitEvent(Object source, String clientIp, String requestPath, String algorithm) {
        super(source);
        this.clientIp = clientIp;
        this.requestPath = requestPath;
        this.algorithm = algorithm;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getRequestPath() {
        return requestPath;
    }

    public String getAlgorithm() {
        return algorithm;
    }
}
