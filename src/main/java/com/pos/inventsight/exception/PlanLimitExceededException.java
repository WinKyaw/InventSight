package com.pos.inventsight.exception;

public class PlanLimitExceededException extends RuntimeException {
    public PlanLimitExceededException(String message) {
        super(message);
    }
    
    public PlanLimitExceededException(String message, Throwable cause) {
        super(message, cause);
    }
}
