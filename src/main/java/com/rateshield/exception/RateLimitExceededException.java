package com.rateshield.exception;

public class RateLimitExceededException extends RuntimeException {

    private final String clientId;
    private final String path;

    public RateLimitExceededException(String clientId, String path) {
        super("Rate limit exceeded for client " + clientId + " on path " + path);
        this.clientId = clientId;
        this.path = path;
    }

    public String getClientId() {
        return clientId;
    }

    public String getPath() {
        return path;
    }
}
