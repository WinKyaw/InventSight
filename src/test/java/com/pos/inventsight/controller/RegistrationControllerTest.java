package com.pos.inventsight.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.inventsight.dto.RegisterRequest;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.service.ActivityLogService;
import com.pos.inventsight.service.PasswordValidationService;
import com.pos.inventsight.service.RateLimitingService;
import com.pos.inventsight.config.JwtUtils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for RegistrationController
 * 
 * Tests the new /register endpoint to ensure it handles POST requests correctly
 * and provides appropriate responses for successful and failed registration attempts.
 */
@WebMvcTest(RegistrationController.class)
public class RegistrationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private ActivityLogService activityLogService;

    @MockBean
    private PasswordValidationService passwordValidationService;

    @MockBean
    private RateLimitingService rateLimitingService;

    /**
     * Test successful user registration via /register endpoint
     */
    @Test
    public void testRegisterUserSuccess() throws Exception {
        // Given - prepare test data
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@inventsight.com");
        registerRequest.setPassword("TestPassword123!");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");

        User savedUser = new User();
        savedUser.setId(1L);
        savedUser.setEmail("test@inventsight.com");
        savedUser.setUsername("testuser");
        savedUser.setFirstName("Test");
        savedUser.setLastName("User");
        savedUser.setRole(UserRole.USER);

        // Mock rate limiting
        when(rateLimitingService.isRegistrationAllowed(anyString(), anyString())).thenReturn(true);

        // Mock password validation
        PasswordValidationService.PasswordValidationResult validationResult = 
            new PasswordValidationService.PasswordValidationResult();
        validationResult.setValid(true);
        when(passwordValidationService.validatePassword(anyString())).thenReturn(validationResult);

        // Mock user service
        when(userService.createUser(any(User.class))).thenReturn(savedUser);
        when(jwtUtils.generateJwtToken(any(User.class))).thenReturn("mock-jwt-token");

        // When & Then - perform request and verify response
        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.email").value("test@inventsight.com"))
                .andExpect(jsonPath("$.username").value("testuser"));
    }

    /**
     * Test registration failure due to weak password
     */
    @Test
    public void testRegisterUserWeakPassword() throws Exception {
        // Given - prepare test data with weak password
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("testuser");
        registerRequest.setEmail("test@inventsight.com");
        registerRequest.setPassword("weak");
        registerRequest.setFirstName("Test");
        registerRequest.setLastName("User");

        // Mock rate limiting
        when(rateLimitingService.isRegistrationAllowed(anyString(), anyString())).thenReturn(true);

        // Mock password validation to fail
        PasswordValidationService.PasswordValidationResult validationResult = 
            new PasswordValidationService.PasswordValidationResult();
        validationResult.setValid(false);
        validationResult.getErrors().add("Password is too weak");
        when(passwordValidationService.validatePassword(anyString())).thenReturn(validationResult);

        // When & Then - perform request and verify error response
        mockMvc.perform(post("/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Password does not meet security requirements"));
    }
}