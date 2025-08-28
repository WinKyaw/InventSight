package com.pos.inventsight.exception;

public class DuplicateSkuException extends RuntimeException {
    public DuplicateSkuException(String message) {
        super(message);
    }
    
    public DuplicateSkuException(String message, Throwable cause) {
        super(message, cause);
    }
}