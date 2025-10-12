package com.pos.inventsight.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.inventsight.model.sql.CompanyRole;
import com.pos.inventsight.model.sql.CompanyStoreUser;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.CompanyStoreUserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.util.List;
import java.util.Map;

/**
 * Response body advice that redacts price/cost fields for EMPLOYEE role.
 * Applies to warehouse inventory responses where employees should not see cost information.
 */
@ControllerAdvice
public class EmployeePriceRedactionAdvice implements ResponseBodyAdvice<Object> {
    
    private static final Logger logger = LoggerFactory.getLogger(EmployeePriceRedactionAdvice.class);
    
    @Autowired
    private CompanyStoreUserRepository companyStoreUserRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        // Only apply to warehouse inventory controller responses
        String controllerName = returnType.getContainingClass().getSimpleName();
        return controllerName.contains("WarehouseInventory");
    }
    
    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        
        // Check if user is an EMPLOYEE
        if (!isEmployee()) {
            return body; // No redaction needed for managers
        }
        
        try {
            // Redact cost fields from response
            return redactCostFields(body);
        } catch (Exception e) {
            logger.error("Error redacting cost fields: {}", e.getMessage(), e);
            return body; // Return original on error
        }
    }
    
    /**
     * Check if current user is an EMPLOYEE
     */
    private boolean isEmployee() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || !authentication.isAuthenticated()) {
                return false;
            }
            
            Object principal = authentication.getPrincipal();
            if (!(principal instanceof User)) {
                return false;
            }
            
            User user = (User) principal;
            List<CompanyStoreUser> memberships = companyStoreUserRepository.findByUserAndIsActiveTrue(user);
            
            // Check if user has only EMPLOYEE role (no manager or founder roles)
            for (CompanyStoreUser membership : memberships) {
                CompanyRole role = membership.getRole();
                if (role.isManagerLevel()) {
                    return false; // User has manager privileges
                }
            }
            
            return true; // User is employee only
            
        } catch (Exception e) {
            logger.debug("Error checking employee status: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Redact cost/price fields from response body
     */
    @SuppressWarnings("unchecked")
    private Object redactCostFields(Object body) {
        if (body == null) {
            return null;
        }
        
        // Handle Map responses (common for REST APIs)
        if (body instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) body;
            redactMapFields(map);
            return map;
        }
        
        // Handle List responses
        if (body instanceof List) {
            List<?> list = (List<?>) body;
            for (Object item : list) {
                if (item instanceof Map) {
                    redactMapFields((Map<String, Object>) item);
                }
            }
            return list;
        }
        
        // For other types, try to convert to map, redact, and convert back
        try {
            Map<String, Object> map = objectMapper.convertValue(body, Map.class);
            redactMapFields(map);
            return objectMapper.convertValue(map, body.getClass());
        } catch (Exception e) {
            logger.debug("Could not convert body to map for redaction: {}", e.getMessage());
            return body;
        }
    }
    
    /**
     * Redact cost/price fields from a map
     */
    @SuppressWarnings("unchecked")
    private void redactMapFields(Map<String, Object> map) {
        if (map == null) {
            return;
        }
        
        // Fields to redact
        String[] costFields = {
            "unitCost", "unit_cost",
            "totalCost", "total_cost",
            "cost", "price",
            "unitPrice", "unit_price",
            "totalPrice", "total_price"
        };
        
        // Redact direct fields
        for (String field : costFields) {
            if (map.containsKey(field)) {
                map.put(field, null);
                logger.debug("Redacted cost field: {}", field);
            }
        }
        
        // Recursively handle nested objects
        for (Object value : map.values()) {
            if (value instanceof Map) {
                redactMapFields((Map<String, Object>) value);
            } else if (value instanceof List) {
                List<?> list = (List<?>) value;
                for (Object item : list) {
                    if (item instanceof Map) {
                        redactMapFields((Map<String, Object>) item);
                    }
                }
            }
        }
    }
}
