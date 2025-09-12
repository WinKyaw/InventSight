package com.pos.inventsight.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pos.inventsight.dto.LoginRequest;
import com.pos.inventsight.dto.RegisterRequest;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.service.ActivityLogService;
import com.pos.inventsight.config.JwtUtils;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
public class AuthControllerTest {

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

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    public void testLoginSuccess() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("test@inventsight.com");
        loginRequest.setPassword("password123");

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@inventsight.com");
        mockUser.setUsername("testuser");
        mockUser.setFirstName("Test");
        mockUser.setLastName("User");
        mockUser.setRole(UserRole.USER);

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(mockUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);
        when(jwtUtils.generateJwtToken(any(User.class))).thenReturn("mock-jwt-token");

        // When & Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("mock-jwt-token"))
                .andExpect(jsonPath("$.email").value("test@inventsight.com"))
                .andExpect(jsonPath("$.role").value("USER"));

        // Verify interactions
        verify(userService).updateLastLogin(1L);
        verify(activityLogService).logActivityWithMetadata(eq("1"), eq("testuser"), 
                eq("USER_LOGIN"), eq("AUTHENTICATION"), anyString(), any());
    }

    @Test
    public void testRegisterSuccess() throws Exception {
        // Given
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("newuser@inventsight.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("New");
        registerRequest.setLastName("User");

        User savedUser = new User();
        savedUser.setId(2L);
        savedUser.setEmail("newuser@inventsight.com");
        savedUser.setUsername("newuser");
        savedUser.setFirstName("New");
        savedUser.setLastName("User");
        savedUser.setRole(UserRole.USER);

        when(userService.createUser(any(User.class))).thenReturn(savedUser);
        when(jwtUtils.generateJwtToken(any(User.class))).thenReturn("mock-jwt-token-new");

        // When & Then
        mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.token").value("mock-jwt-token-new"))
                .andExpect(jsonPath("$.email").value("newuser@inventsight.com"))
                .andExpect(jsonPath("$.username").value("newuser"));

        // Verify interactions
        verify(userService).createUser(any(User.class));
        verify(activityLogService).logActivity(eq("2"), eq("newuser"), 
                eq("USER_REGISTERED"), eq("AUTHENTICATION"), anyString());
    }

    @Test
    public void testLoginInvalidCredentials() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("invalid@inventsight.com");
        loginRequest.setPassword("wrongpassword");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new org.springframework.security.core.AuthenticationException("Invalid credentials") {});

        // When & Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"));

        // Verify failed login was logged
        verify(activityLogService).logActivity(isNull(), eq("WinKyaw"), 
                eq("USER_LOGIN_FAILED"), eq("AUTHENTICATION"), anyString());
    }

    @Test
    public void testLoginWithUnverifiedEmail() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("unverified@inventsight.com");
        loginRequest.setPassword("password123");

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("unverified@inventsight.com");
        mockUser.setUsername("unverifieduser");
        mockUser.setFirstName("Unverified");
        mockUser.setLastName("User");
        mockUser.setRole(UserRole.USER);
        mockUser.setEmailVerified(false); // Email not verified

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(mockUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);

        // When & Then
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email not verified. Please verify your email before logging in."));

        // Verify email verification failure was logged
        verify(activityLogService).logActivity(eq("1"), eq("unverifieduser"), 
                eq("USER_LOGIN_EMAIL_UNVERIFIED"), eq("AUTHENTICATION"), anyString());
        
        // Verify that last login was NOT updated for unverified users
        verify(userService, never()).updateLastLogin(anyLong());
    }

    @Test
    public void testLoginV2WithUnverifiedEmail() throws Exception {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setEmail("unverified@inventsight.com");
        loginRequest.setPassword("password123");

        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("unverified@inventsight.com");
        mockUser.setUsername("unverifieduser");
        mockUser.setFirstName("Unverified");
        mockUser.setLastName("User");
        mockUser.setRole(UserRole.USER);
        mockUser.setEmailVerified(false); // Email not verified

        Authentication mockAuth = mock(Authentication.class);
        when(mockAuth.getPrincipal()).thenReturn(mockUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(mockAuth);

        // When & Then
        mockMvc.perform(post("/auth/login/v2")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Email not verified. Please verify your email before logging in."))
                .andExpect(jsonPath("$.success").value(false));

        // Verify email verification failure was logged
        verify(activityLogService).logActivity(eq("1"), eq("unverifieduser"), 
                eq("USER_LOGIN_V2_EMAIL_UNVERIFIED"), eq("AUTHENTICATION"), anyString());
    }

    @Test
    public void testLogoutSuccess() throws Exception {
        // Given
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@inventsight.com");
        mockUser.setUsername("testuser");

        when(jwtUtils.validateJwtToken("valid-token")).thenReturn(true);
        when(jwtUtils.getUsernameFromJwtToken("valid-token")).thenReturn("test@inventsight.com");
        when(userService.getUserByEmail("test@inventsight.com")).thenReturn(mockUser);

        // When & Then
        mockMvc.perform(post("/auth/logout")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("User logged out successfully"));

        // Verify logout was logged
        verify(activityLogService).logActivity(eq("1"), eq("testuser"), 
                eq("USER_LOGOUT"), eq("AUTHENTICATION"), anyString());
    }
}