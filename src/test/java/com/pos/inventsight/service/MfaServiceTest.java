package com.pos.inventsight.service;

import com.pos.inventsight.model.sql.MfaSecret;
import com.pos.inventsight.model.sql.User;
import com.pos.inventsight.repository.sql.MfaBackupCodeRepository;
import com.pos.inventsight.repository.sql.MfaSecretRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for MfaService
 */
@ExtendWith(MockitoExtension.class)
class MfaServiceTest {
    
    @Mock
    private MfaSecretRepository mfaSecretRepository;
    
    @Mock
    private MfaBackupCodeRepository mfaBackupCodeRepository;
    
    @Mock
    private PasswordEncoder passwordEncoder;
    
    @Mock
    private AuditService auditService;
    
    @InjectMocks
    private MfaService mfaService;
    
    private User testUser;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setUsername("testuser");
    }
    
    @Test
    void testSetupMfa_Success() {
        // Given
        when(mfaSecretRepository.findByUser(testUser)).thenReturn(Optional.empty());
        when(mfaSecretRepository.save(any(MfaSecret.class))).thenAnswer(i -> i.getArguments()[0]);
        
        // When
        MfaService.MfaSetupResponse response = mfaService.setupMfa(testUser);
        
        // Then
        assertNotNull(response);
        assertNotNull(response.getSecret());
        assertNotNull(response.getQrCodeUrl());
        assertTrue(response.getQrCodeUrl().contains("otpauth://totp/"));
        
        // Verify QR code image is generated (Base64 encoded PNG)
        assertNotNull(response.getQrCodeImage());
        assertTrue(response.getQrCodeImage().length() > 0);
        
        verify(mfaSecretRepository, times(1)).save(any(MfaSecret.class));
        verify(auditService, times(1)).logAsync(anyString(), any(UUID.class), eq("MFA_SETUP_INITIATED"), anyString(), anyString(), any());
    }
    
    @Test
    void testSetupMfa_AlreadyEnabled() {
        // Given
        MfaSecret existingSecret = new MfaSecret(testUser, "existing-secret");
        existingSecret.setEnabled(true);
        when(mfaSecretRepository.findByUser(testUser)).thenReturn(Optional.of(existingSecret));
        
        // When & Then
        assertThrows(IllegalStateException.class, () -> mfaService.setupMfa(testUser));
    }
    
    @Test
    void testIsMfaEnabled() {
        // Given
        MfaSecret secret = new MfaSecret(testUser, "secret");
        secret.setEnabled(true);
        when(mfaSecretRepository.findByUser(testUser)).thenReturn(Optional.of(secret));
        
        // When
        boolean isEnabled = mfaService.isMfaEnabled(testUser);
        
        // Then
        assertTrue(isEnabled);
    }
    
    @Test
    void testIsMfaEnabled_NotEnabled() {
        // Given
        when(mfaSecretRepository.findByUser(testUser)).thenReturn(Optional.empty());
        
        // When
        boolean isEnabled = mfaService.isMfaEnabled(testUser);
        
        // Then
        assertFalse(isEnabled);
    }
    
    @Test
    void testGenerateBackupCodes() {
        // Given
        MfaSecret secret = new MfaSecret(testUser, "secret");
        secret.setEnabled(true);
        when(mfaSecretRepository.findByUser(testUser)).thenReturn(Optional.of(secret));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-code");
        
        // When
        List<String> backupCodes = mfaService.generateBackupCodes(testUser);
        
        // Then
        assertNotNull(backupCodes);
        assertEquals(10, backupCodes.size());
        
        // Verify all codes are alphanumeric and correct length
        for (String code : backupCodes) {
            assertEquals(8, code.length());
            assertTrue(code.matches("[A-Z2-9]+"));
        }
        
        verify(mfaBackupCodeRepository, times(1)).deleteByUser(testUser);
        verify(mfaBackupCodeRepository, times(10)).save(any());
    }
    
    @Test
    void testGenerateBackupCodes_MfaNotEnabled() {
        // Given
        MfaSecret secret = new MfaSecret(testUser, "secret");
        secret.setEnabled(false);
        when(mfaSecretRepository.findByUser(testUser)).thenReturn(Optional.of(secret));
        
        // When & Then
        assertThrows(IllegalStateException.class, () -> mfaService.generateBackupCodes(testUser));
    }
    
    @Test
    void testDisableMfa() {
        // Given
        MfaSecret secret = new MfaSecret(testUser, "secret");
        secret.setEnabled(true);
        when(mfaSecretRepository.findByUser(testUser)).thenReturn(Optional.of(secret));
        
        // When
        mfaService.disableMfa(testUser);
        
        // Then
        assertFalse(secret.getEnabled());
        verify(mfaSecretRepository, times(1)).save(secret);
        verify(mfaBackupCodeRepository, times(1)).deleteByUser(testUser);
        verify(auditService, times(1)).logAsync(anyString(), any(UUID.class), eq("MFA_DISABLED"), anyString(), anyString(), any());
    }
}
