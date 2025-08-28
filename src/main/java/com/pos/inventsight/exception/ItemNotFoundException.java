package com.pos.inventsight.exception;

public class ItemNotFoundException extends ResourceNotFoundException {
    public ItemNotFoundException(String message) {
        super(message);
    }
    
    public ItemNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}