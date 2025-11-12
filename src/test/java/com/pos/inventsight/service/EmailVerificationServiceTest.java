package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.EmailVerificationToken;
import com.pos.inventsight.repository.sql.EmailVerificationTokenRepository;
import com.pos.inventsight.repository.sql.UserRepository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for EmailVerificationService - ensures real emails are sent
 */
@SpringBootTest
@ActiveProfiles("test")
public class EmailVerificationServiceTest {

    @Autowired
    private EmailVerificationService emailVerificationService;

    @MockBean
    private EmailService emailService;

    @MockBean
    private EmailVerificationTokenRepository tokenRepository;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private ActivityLogService activityLogService;

    @BeforeEach
    void setUp() {
        // Reset mocks
        reset(emailService, tokenRepository, activityLogService);
    }

    /**
     * Test that sendVerificationEmail calls EmailService.sendEmail with correct parameters
     */
    @Test
    void testSendVerificationEmailCallsRealEmailService() {
        // Given
        String testEmail = "test@example.com";
        String testToken = "TEST_TOKEN_123";

        // When
        emailVerificationService.sendVerificationEmail(testEmail, testToken);

        // Then: EmailService.sendEmail should be called once
        verify(emailService, times(1)).sendEmail(
            eq(testEmail),
            anyString(),  // subject
            anyString()   // body
        );

        // And: Activity should be logged
        verify(activityLogService, times(1)).logActivity(
            isNull(),
            eq("WinKyaw"),
            eq("EMAIL_VERIFICATION_SENT"),
            eq("AUTHENTICATION"),
            contains(testEmail)
        );
    }

    /**
     * Test that email body contains verification link with token
     */
    @Test
    void testVerificationEmailContainsTokenAndLink() {
        // Given
        String testEmail = "test@example.com";
        String testToken = "TEST_TOKEN_123";

        // Capture the email body
        doAnswer(invocation -> {
            String email = invocation.getArgument(0);
            String subject = invocation.getArgument(1);
            String body = invocation.getArgument(2);

            // Verify email body contains token and email
            assertTrue(body.contains(testToken), "Email body should contain token");
            assertTrue(body.contains(testEmail), "Email body should contain email address");
            assertTrue(body.contains("verify"), "Email body should mention verification");
            
            return null;
        }).when(emailService).sendEmail(anyString(), anyString(), anyString());

        // When
        emailVerificationService.sendVerificationEmail(testEmail, testToken);

        // Then: Email was sent with correct content
        verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString());
    }

    /**
     * Test that email sending failure is properly handled
     */
    @Test
    void testEmailSendingFailureIsHandled() {
        // Given
        String testEmail = "test@example.com";
        String testToken = "TEST_TOKEN_123";

        // Mock email service to throw exception
        doThrow(new RuntimeException("Email service unavailable"))
            .when(emailService).sendEmail(anyString(), anyString(), anyString());

        // When/Then: Exception should be thrown
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            emailVerificationService.sendVerificationEmail(testEmail, testToken);
        });

        // And: Error message should indicate email failure
        assertTrue(exception.getMessage().contains("Failed to send verification email"));

        // And: Failed activity should be logged
        verify(activityLogService, times(1)).logActivity(
            isNull(),
            eq("WinKyaw"),
            eq("EMAIL_VERIFICATION_FAILED"),
            eq("AUTHENTICATION"),
            contains(testEmail)
        );
    }

    /**
     * Test that email subject is appropriate
     */
    @Test
    void testVerificationEmailHasCorrectSubject() {
        // Given
        String testEmail = "test@example.com";
        String testToken = "TEST_TOKEN_123";

        // Capture the email subject
        doAnswer(invocation -> {
            String email = invocation.getArgument(0);
            String subject = invocation.getArgument(1);
            String body = invocation.getArgument(2);

            // Verify subject is appropriate
            assertTrue(subject.contains("Verification") || subject.contains("Email"), 
                "Subject should mention verification or email");
            assertTrue(subject.contains("InventSight"), 
                "Subject should mention InventSight");
            
            return null;
        }).when(emailService).sendEmail(anyString(), anyString(), anyString());

        // When
        emailVerificationService.sendVerificationEmail(testEmail, testToken);

        // Then: Email was sent
        verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString());
    }

    /**
     * Test that no mock console logging is used (method should use real email service)
     */
    @Test
    void testNoMockConsoleLogging() {
        // Given
        String testEmail = "test@example.com";
        String testToken = "TEST_TOKEN_123";

        // When
        emailVerificationService.sendVerificationEmail(testEmail, testToken);

        // Then: EmailService.sendEmail must be called (not console logging)
        verify(emailService, times(1)).sendEmail(anyString(), anyString(), anyString());
        
        // This test ensures the method actually calls EmailService rather than
        // just using System.out.println for mock email sending
    }
}
