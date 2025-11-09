# Multi-Factor Authentication (MFA) Implementation Guide

## Overview

InventSight supports secure Two-Factor Authentication (2FA/MFA) using Time-based One-Time Passwords (TOTP). This implementation is compatible with popular authenticator apps like Google Authenticator, Authy, Microsoft Authenticator, and others.

## Features

### Core MFA Functionality
- ✅ **TOTP Generation**: Uses industry-standard TOTP algorithm (RFC 6238)
- ✅ **QR Code Setup**: Generates both QR code URL and Base64-encoded PNG image
- ✅ **Authenticator App Support**: Compatible with Google Authenticator, Authy, Microsoft Authenticator, etc.
- ✅ **Backup Codes**: 10 single-use recovery codes for account recovery
- ✅ **Rate Limiting**: Prevents brute-force attacks (5 attempts per 60 minutes)
- ✅ **Secure Storage**: MFA secrets stored separately with encryption support

### Security Features
- **Rate Limiting**: Maximum 5 MFA verification attempts per 60 minutes
- **Automatic Cleanup**: Failed attempts are cleared on successful verification
- **Secure Secret Storage**: MFA secrets stored in separate table with encryption capability
- **Audit Logging**: All MFA operations are logged for security audits
- **Backup Codes**: Hashed using bcrypt, single-use only

## API Endpoints

### 1. MFA Setup - Generate QR Code
**POST** `/auth/mfa/setup`

Initiates MFA setup for authenticated user. Returns secret key and QR code for scanning.

**Authentication Required**: Yes (Bearer token)

**Response**:
```json
{
  "success": true,
  "message": "MFA setup initiated successfully. Scan the QR code with your authenticator app.",
  "data": {
    "secret": "JBSWY3DPEHPK3PXP",
    "qrCodeUrl": "otpauth://totp/InventSight:user@example.com?secret=JBSWY3DPEHPK3PXP&issuer=InventSight",
    "qrCodeImage": "iVBORw0KGgoAAAANSUhEUgAA..." // Base64 encoded PNG
  }
}
```

**Error Response**:
```json
{
  "success": false,
  "message": "MFA is already enabled for this user"
}
```

---

### 2. Enable MFA - Verify TOTP Code
**POST** `/auth/mfa/enable`

Verifies TOTP code and enables MFA for the user.

**Authentication Required**: Yes (Bearer token)

**Request Body**:
```json
{
  "code": 123456
}
```

**Success Response**:
```json
{
  "success": true,
  "message": "MFA enabled successfully. Your account is now secured with two-factor authentication."
}
```

**Error Responses**:
```json
// Invalid code
{
  "success": false,
  "message": "Invalid verification code. Please try again."
}

// Rate limited
{
  "success": false,
  "message": "Too many verification attempts. Please try again after 2025-11-08T13:30:00."
}

// Setup not found
{
  "success": false,
  "message": "MFA setup not found. Please initiate MFA setup first."
}
```

---

### 3. Verify MFA (Alias)
**POST** `/auth/mfa/verify`

Alternative endpoint for enabling MFA. Same functionality as `/auth/mfa/enable`.

---

### 4. Check MFA Status
**GET** `/auth/mfa/status`

Checks if MFA is enabled for the authenticated user.

**Authentication Required**: Yes (Bearer token)

**Response**:
```json
{
  "success": true,
  "message": "MFA is enabled for this account",
  "data": {
    "enabled": true
  }
}
```

---

### 5. Generate Backup Codes
**POST** `/auth/mfa/backup-codes`

Generates 10 single-use backup codes for account recovery. Previous codes are invalidated.

**Authentication Required**: Yes (Bearer token)

**Response**:
```json
{
  "success": true,
  "message": "Backup codes generated successfully. Store these codes securely. They can only be used once.",
  "data": {
    "backupCodes": [
      "A2B4C6D8",
      "E3F5G7H9",
      "J4K6L8M2",
      // ... 7 more codes
    ]
  }
}
```

**Important**: 
- Store backup codes securely - they cannot be retrieved again
- Each code can only be used once
- Generating new codes invalidates all previous backup codes

---

### 6. Disable MFA
**DELETE** `/auth/mfa/disable`

Disables MFA for the authenticated user and removes all backup codes.

**Authentication Required**: Yes (Bearer token)

**Response**:
```json
{
  "success": true,
  "message": "MFA disabled successfully. Your account is no longer protected by two-factor authentication."
}
```

---

## Login Flow with MFA

### Without MFA Enabled
**POST** `/auth/login`

```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

Response: Standard login response with JWT token.

---

### With MFA Enabled
**POST** `/auth/login`

**Step 1**: Attempt login without TOTP code
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!"
}
```

Response (401 Unauthorized):
```json
{
  "error": "MFA_REQUIRED",
  "message": "Multi-factor authentication code is required"
}
```

**Step 2**: Login with TOTP code
```json
{
  "email": "user@example.com",
  "password": "SecurePassword123!",
  "totpCode": 123456
}
```

Response: Standard login response with JWT token.

---

## Setup Guide for Users

### Setting Up MFA

1. **Initiate Setup**
   - Call `POST /auth/mfa/setup` with your authentication token
   - Receive QR code and secret key

2. **Configure Authenticator App**
   - Option A: Scan the QR code using your authenticator app
   - Option B: Manually enter the secret key
   
   Supported apps:
   - Google Authenticator (iOS/Android)
   - Authy (iOS/Android/Desktop)
   - Microsoft Authenticator (iOS/Android)
   - 1Password, LastPass Authenticator, etc.

3. **Enable MFA**
   - Enter the 6-digit code from your authenticator app
   - Call `POST /auth/mfa/enable` with the code
   - MFA is now active on your account

4. **Generate Backup Codes**
   - Call `POST /auth/mfa/backup-codes`
   - Store the 10 backup codes securely
   - Use them if you lose access to your authenticator app

### Using MFA

1. **Login Process**
   - Enter your email and password as usual
   - You'll be prompted for a 6-digit TOTP code
   - Enter the code from your authenticator app
   - Successfully authenticated

2. **Using Backup Codes**
   - If you don't have access to your authenticator app
   - Enter a backup code instead of the TOTP code
   - Each backup code can only be used once
   - Generate new backup codes after using several

### Disabling MFA

1. **Disable Process**
   - Must be authenticated
   - Call `DELETE /auth/mfa/disable`
   - MFA is disabled and backup codes are removed
   - You can re-enable MFA at any time

---

## Security Considerations

### Rate Limiting
- MFA verification attempts are limited to 5 per 60 minutes
- Applies to both login attempts and MFA setup verification
- Successful verification clears the rate limit counter
- Prevents brute-force attacks on TOTP codes

### Secret Storage
- MFA secrets are stored in a separate database table
- Secrets can be encrypted at rest (configuration required)
- Backup codes are hashed using bcrypt
- Each backup code can only be used once

### Best Practices
1. **For Users**:
   - Store backup codes in a secure location (password manager, encrypted file)
   - Use a reputable authenticator app
   - Don't share your QR code or secret key
   - Regenerate backup codes periodically

2. **For Administrators**:
   - Monitor MFA-related audit logs
   - Consider implementing MFA enforcement for admin accounts
   - Set up alerting for unusual MFA-related activities
   - Regularly review and update security configurations

---

## Technical Implementation

### Dependencies
```xml
<!-- TOTP for MFA -->
<dependency>
    <groupId>com.warrenstrange</groupId>
    <artifactId>googleauth</artifactId>
    <version>1.5.0</version>
</dependency>

<!-- QR Code Generation -->
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>core</artifactId>
    <version>3.5.3</version>
</dependency>
<dependency>
    <groupId>com.google.zxing</groupId>
    <artifactId>javase</artifactId>
    <version>3.5.3</version>
</dependency>

<!-- Rate Limiting -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.3.0</version>
</dependency>
```

### Database Schema

**mfa_secrets** table:
- `id` (UUID, Primary Key)
- `user_id` (Foreign Key to users table, Unique)
- `secret` (String, Encrypted)
- `enabled` (Boolean)
- `created_at` (Timestamp)
- `verified_at` (Timestamp)

**mfa_backup_codes** table:
- `id` (UUID, Primary Key)
- `user_id` (Foreign Key to users table)
- `code_hash` (String, bcrypt hashed)
- `used` (Boolean)
- `used_at` (Timestamp)
- `created_at` (Timestamp)

### Configuration

Add to `application.properties`:
```properties
# MFA Configuration
inventsight.mfa.issuer=InventSight
inventsight.mfa.totp.window-size=3

# Rate Limiting
inventsight.security.mfa.max-attempts=5
inventsight.security.mfa.window-minutes=60
```

---

## Error Codes

| Error Code | HTTP Status | Description |
|-----------|-------------|-------------|
| MFA_REQUIRED | 401 | MFA code is required for login |
| MFA_INVALID_CODE | 401 | Invalid TOTP code provided |
| MFA_RATE_LIMITED | 429 | Too many verification attempts |
| MFA_ALREADY_ENABLED | 400 | MFA is already enabled for user |
| MFA_NOT_SETUP | 400 | MFA setup not initiated |
| MFA_NOT_ENABLED | 400 | MFA must be enabled first |

---

## Testing

### Unit Tests
Run MFA service tests:
```bash
mvn test -Dtest=MfaServiceTest
```

### Integration Tests
Run MFA login flow tests:
```bash
mvn test -Dtest=MfaLoginFlowIntegrationTest
```

### Manual Testing
1. Setup MFA and scan QR code with authenticator app
2. Verify codes are working correctly
3. Test login with valid/invalid codes
4. Test rate limiting (5+ failed attempts)
5. Test backup codes generation and usage
6. Test MFA disable functionality

---

## Troubleshooting

### Common Issues

**QR Code Won't Scan**
- Ensure QR code image is displayed at adequate size
- Try manually entering the secret key
- Check that the issuer name doesn't contain special characters

**TOTP Code Always Invalid**
- Verify device time is synchronized (TOTP is time-based)
- Check the window size configuration (default: 3 = ±90 seconds)
- Ensure the secret was entered correctly

**Rate Limit Reached**
- Wait 60 minutes for the limit to reset
- Contact administrator if legitimate lockout
- Consider using a backup code

**Lost Authenticator Access**
- Use one of the backup codes to login
- Generate new backup codes after logging in
- Re-setup MFA with a new device if needed

---

## Future Enhancements

Potential improvements for future versions:
- SMS-based 2FA as alternative
- Push notification-based 2FA
- Hardware token support (YubiKey, etc.)
- Trusted device management
- MFA enforcement policies per user role
- Admin ability to reset user MFA
- MFA setup during registration flow
- Location-based MFA requirements

---

## Related Documentation

- [Login API Documentation](LOGIN_API.md)
- [Security Enhancements](SECURITY_ENHANCEMENTS_IMPLEMENTATION.md)
- [Tenant Binding](TENANT_BINDING.md)
- [Rate Limiting Configuration](RATE_LIMITING.md)
