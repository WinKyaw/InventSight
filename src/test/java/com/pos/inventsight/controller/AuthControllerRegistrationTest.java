package com.pos.inventsight.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.inventsight.dto.EmailVerificationRequest;
import com.pos.inventsight.dto.ResendVerificationRequest;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.service.ActivityLogService;
import com.pos.inventsight.service.EmailVerificationService;
import com.pos.inventsight.config.JwtUtils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
public class AuthControllerRegistrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private ActivityLogService activityLogService;
    
    @MockBean
    private EmailVerificationService emailVerificationService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testCheckEmailAvailability_EmailExists() throws Exception {
        // Given
        when(userService.emailExists("existing@inventsight.com")).thenReturn(true);

        // When & Then
        mockMvc.perform(get("/auth/check-email")
                .param("email", "existing@inventsight.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("existing@inventsight.com"))
                .andExpect(jsonPath("$.exists").value(true))
                .andExpect(jsonPath("$.available").value(false));

        verify(userService).emailExists("existing@inventsight.com");
    }

    @Test
    public void testCheckEmailAvailability_EmailAvailable() throws Exception {
        // Given
        when(userService.emailExists("new@inventsight.com")).thenReturn(false);

        // When & Then
        mockMvc.perform(get("/auth/check-email")
                .param("email", "new@inventsight.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("new@inventsight.com"))
                .andExpect(jsonPath("$.exists").value(false))
                .andExpect(jsonPath("$.available").value(true));

        verify(userService).emailExists("new@inventsight.com");
    }

    @Test
    public void testVerifyEmail_Success() throws Exception {
        // Given
        EmailVerificationRequest request = new EmailVerificationRequest();
        request.setToken("VALID_TOKEN_123");
        request.setEmail("test@inventsight.com");

        when(emailVerificationService.verifyEmail("VALID_TOKEN_123", "test@inventsight.com"))
                .thenReturn(true);

        // When & Then
        mockMvc.perform(post("/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Email verified successfully"))
                .andExpect(jsonPath("$.email").value("test@inventsight.com"));

        verify(emailVerificationService).verifyEmail("VALID_TOKEN_123", "test@inventsight.com");
    }

    @Test
    public void testVerifyEmail_InvalidToken() throws Exception {
        // Given
        EmailVerificationRequest request = new EmailVerificationRequest();
        request.setToken("INVALID_TOKEN");
        request.setEmail("test@inventsight.com");

        when(emailVerificationService.verifyEmail("INVALID_TOKEN", "test@inventsight.com"))
                .thenReturn(false);

        // When & Then
        mockMvc.perform(post("/auth/verify-email")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired verification token"));

        verify(emailVerificationService).verifyEmail("INVALID_TOKEN", "test@inventsight.com");
    }

    @Test
    public void testResendVerification_Success() throws Exception {
        // Given
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("test@inventsight.com");

        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("test@inventsight.com");
        mockUser.setEmailVerified(false);

        when(userService.emailExists("test@inventsight.com")).thenReturn(true);
        when(userService.getUserByEmail("test@inventsight.com")).thenReturn(mockUser);
        when(emailVerificationService.hasValidToken("test@inventsight.com")).thenReturn(false);
        when(emailVerificationService.generateVerificationToken("test@inventsight.com"))
                .thenReturn("NEW_TOKEN_123");

        // When & Then
        mockMvc.perform(post("/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Verification email sent successfully"))
                .andExpect(jsonPath("$.email").value("test@inventsight.com"));

        verify(emailVerificationService).generateVerificationToken("test@inventsight.com");
        verify(emailVerificationService).sendVerificationEmail("test@inventsight.com", "NEW_TOKEN_123");
    }

    @Test
    public void testResendVerification_UserNotFound() throws Exception {
        // Given
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("nonexistent@inventsight.com");

        when(userService.emailExists("nonexistent@inventsight.com")).thenReturn(false);

        // When & Then
        mockMvc.perform(post("/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found with this email address"));

        verify(userService).emailExists("nonexistent@inventsight.com");
        verify(emailVerificationService, never()).generateVerificationToken(anyString());
    }

    @Test
    public void testResendVerification_AlreadyVerified() throws Exception {
        // Given
        ResendVerificationRequest request = new ResendVerificationRequest();
        request.setEmail("verified@inventsight.com");

        User mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("verified@inventsight.com");
        mockUser.setEmailVerified(true);

        when(userService.emailExists("verified@inventsight.com")).thenReturn(true);
        when(userService.getUserByEmail("verified@inventsight.com")).thenReturn(mockUser);

        // When & Then
        mockMvc.perform(post("/auth/resend-verification")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email is already verified"));

        verify(userService).getUserByEmail("verified@inventsight.com");
        verify(emailVerificationService, never()).generateVerificationToken(anyString());
    }
}