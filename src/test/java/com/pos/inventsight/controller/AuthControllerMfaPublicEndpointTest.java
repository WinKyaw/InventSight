package com.pos.inventsight.controller;

import com.pos.inventsight.dto.MfaSendOtpRequest;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.model.sql.UserRole;
import com.pos.inventsight.model.sql.MfaSecret;
import com.pos.inventsight.service.UserService;
import com.pos.inventsight.service.MfaService;
import com.pos.inventsight.exception.ResourceNotFoundException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for public MFA send-login-otp endpoint - ensures OTP can be sent during login without authentication
 */
@SpringBootTest
@ActiveProfiles("test")
public class AuthControllerMfaPublicEndpointTest {

    @Autowired
    private AuthController authController;

    @MockBean
    private UserService userService;

    @MockBean
    private MfaService mfaService;

    private User testUser;
    private MockHttpServletRequest mockRequest;

    @BeforeEach
    void setUp() {
        // Create test user
        testUser = new User();
        testUser.setId(UUID.randomUUID());  // User uses UUID id
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(UserRole.USER);
        testUser.setEmailVerified(true);

        // Create mock HTTP request
        mockRequest = new MockHttpServletRequest();
        mockRequest.setRemoteAddr("127.0.0.1");
    }

    /**
     * Test that sendLoginOtp works without authentication for email delivery
     */
    @Test
    void testSendLoginOtpWithEmailDeliveryNoAuth() {
        // Given: User exists and MFA is enabled
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(mfaService.isMfaEnabled(testUser)).thenReturn(true);
        
        // Request to send OTP via email
        MfaSendOtpRequest request = new MfaSendOtpRequest();
        request.setDeliveryMethod(MfaSendOtpRequest.DeliveryMethod.EMAIL);
        request.setEmail("test@example.com");

        // When: Send login OTP (no authentication required)
        ResponseEntity<?> response = authController.sendLoginOtp(request, mockRequest);

        // Then: Response should be successful
        assertEquals(200, response.getStatusCodeValue());
        
        // And: OTP should be sent
        verify(mfaService, times(1)).sendOtpCode(
            eq(testUser),
            eq(MfaSecret.DeliveryMethod.EMAIL),
            eq("127.0.0.1")
        );
        
        // And: Response should contain success message
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertEquals("EMAIL", responseBody.get("deliveryMethod").toString());
    }

    /**
     * Test that sendLoginOtp returns error when email is missing for EMAIL delivery
     */
    @Test
    void testSendLoginOtpRequiresEmailForEmailDelivery() {
        // Given: Request without email
        MfaSendOtpRequest request = new MfaSendOtpRequest();
        request.setDeliveryMethod(MfaSendOtpRequest.DeliveryMethod.EMAIL);
        // email is null

        // When: Send login OTP
        ResponseEntity<?> response = authController.sendLoginOtp(request, mockRequest);

        // Then: Response should be bad request
        assertEquals(400, response.getStatusCodeValue());
        
        // And: Error message should indicate email is required
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertTrue(responseBody.get("message").toString().contains("Email"));
    }

    /**
     * Test that sendLoginOtp returns generic success when user doesn't exist (security)
     */
    @Test
    void testSendLoginOtpDoesNotRevealUserExistence() {
        // Given: User doesn't exist
        when(userService.getUserByEmail("nonexistent@example.com"))
            .thenThrow(new ResourceNotFoundException("User not found"));
        
        MfaSendOtpRequest request = new MfaSendOtpRequest();
        request.setDeliveryMethod(MfaSendOtpRequest.DeliveryMethod.EMAIL);
        request.setEmail("nonexistent@example.com");

        // When: Send login OTP
        ResponseEntity<?> response = authController.sendLoginOtp(request, mockRequest);

        // Then: Response should be successful (don't reveal user doesn't exist)
        assertEquals(200, response.getStatusCodeValue());
        
        // And: Generic success message should be returned
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertTrue((Boolean) responseBody.get("success"));
        assertTrue(responseBody.get("message").toString().contains("If an account exists"));
        
        // And: No OTP should actually be sent
        verify(mfaService, never()).sendOtpCode(any(), any(), any());
    }

    /**
     * Test that sendLoginOtp returns error when MFA is not enabled
     */
    @Test
    void testSendLoginOtpRequiresMfaEnabled() {
        // Given: User exists but MFA is not enabled
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(mfaService.isMfaEnabled(testUser)).thenReturn(false);
        
        MfaSendOtpRequest request = new MfaSendOtpRequest();
        request.setDeliveryMethod(MfaSendOtpRequest.DeliveryMethod.EMAIL);
        request.setEmail("test@example.com");

        // When: Send login OTP
        ResponseEntity<?> response = authController.sendLoginOtp(request, mockRequest);

        // Then: Response should be bad request
        assertEquals(400, response.getStatusCodeValue());
        
        // And: Error message should indicate MFA is not enabled
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertTrue(responseBody.get("message").toString().contains("MFA is not enabled"));
        
        // And: No OTP should be sent
        verify(mfaService, never()).sendOtpCode(any(), any(), any());
    }

    /**
     * Test that sendLoginOtp handles SMS delivery method properly
     */
    @Test
    void testSendLoginOtpSmsNotYetImplemented() {
        // Given: Request with SMS delivery
        MfaSendOtpRequest request = new MfaSendOtpRequest();
        request.setDeliveryMethod(MfaSendOtpRequest.DeliveryMethod.SMS);
        request.setPhoneNumber("+1234567890");

        // When: Send login OTP
        ResponseEntity<?> response = authController.sendLoginOtp(request, mockRequest);

        // Then: Response should indicate SMS is not yet implemented
        assertEquals(400, response.getStatusCodeValue());
        
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertTrue(responseBody.get("message").toString().toLowerCase().contains("sms"));
    }

    /**
     * Test that sendLoginOtp requires phone number for SMS delivery
     */
    @Test
    void testSendLoginOtpRequiresPhoneForSmsDelivery() {
        // Given: Request without phone number
        MfaSendOtpRequest request = new MfaSendOtpRequest();
        request.setDeliveryMethod(MfaSendOtpRequest.DeliveryMethod.SMS);
        // phoneNumber is null

        // When: Send login OTP
        ResponseEntity<?> response = authController.sendLoginOtp(request, mockRequest);

        // Then: Response should be bad request
        assertEquals(400, response.getStatusCodeValue());
        
        // And: Error message should indicate phone number is required
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertTrue(responseBody.get("message").toString().contains("Phone"));
    }

    /**
     * Test that sendLoginOtp handles MfaService exceptions gracefully
     */
    @Test
    void testSendLoginOtpHandlesMfaServiceExceptions() {
        // Given: User exists, MFA enabled, but MfaService throws exception
        when(userService.getUserByEmail("test@example.com")).thenReturn(testUser);
        when(mfaService.isMfaEnabled(testUser)).thenReturn(true);
        doThrow(new RuntimeException("Rate limit exceeded"))
            .when(mfaService).sendOtpCode(any(), any(), any());
        
        MfaSendOtpRequest request = new MfaSendOtpRequest();
        request.setDeliveryMethod(MfaSendOtpRequest.DeliveryMethod.EMAIL);
        request.setEmail("test@example.com");

        // When: Send login OTP
        ResponseEntity<?> response = authController.sendLoginOtp(request, mockRequest);

        // Then: Response should be internal server error
        assertEquals(500, response.getStatusCodeValue());
        
        // And: Generic error message should be returned
        Map<String, Object> responseBody = (Map<String, Object>) response.getBody();
        assertFalse((Boolean) responseBody.get("success"));
        assertTrue(responseBody.get("message").toString().contains("try again later"));
    }
}
