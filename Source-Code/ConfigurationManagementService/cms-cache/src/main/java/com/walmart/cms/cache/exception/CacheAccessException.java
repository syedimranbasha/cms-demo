package com.walmart.cms.cache.exception;

public class CacheAccessException extends RuntimeException {
    public CacheAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
