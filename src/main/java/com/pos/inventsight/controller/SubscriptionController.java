package com.pos.inventsight.controller;

import com.pos.inventsight.dto.GenericApiResponse;
import com.pos.inventsight.dto.SubscriptionInfoResponse;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.service.SubscriptionService;
import com.pos.inventsight.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/subscription")
@Tag(name = "Subscription Management", description = "User subscription and quota management")
public class SubscriptionController {
    
    @Autowired
    private SubscriptionService subscriptionService;
    
    @Autowired
    private UserService userService;
    
    /**
     * Get current user's subscription information
     */
    @GetMapping
    @Operation(summary = "Get subscription info", description = "Get current user's subscription plan and usage information")
    public ResponseEntity<GenericApiResponse<SubscriptionInfoResponse>> getSubscriptionInfo(Authentication authentication) {
        try {
            String username = authentication.getName();
            User user = userService.getUserByUsername(username);
            
            SubscriptionInfoResponse response = subscriptionService.getSubscriptionInfo(user);
            
            return ResponseEntity.ok(new GenericApiResponse<>(true, "Subscription info retrieved successfully", response));
            
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                .body(new GenericApiResponse<>(false, e.getMessage(), null));
        }
    }
}
