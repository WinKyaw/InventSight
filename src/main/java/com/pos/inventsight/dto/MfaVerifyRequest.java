package com.pos.inventsight.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for MFA verification
 */
public class MfaVerifyRequest {
    
    @NotNull(message = "Verification code is required")
    private Integer code;
    
    public MfaVerifyRequest() {}
    
    public MfaVerifyRequest(Integer code) {
        this.code = code;
    }
    
    public Integer getCode() {
        return code;
    }
    
    public void setCode(Integer code) {
        this.code = code;
    }
}
