# MFA OTP Guide - Email and SMS One-Time Password Support

## Overview

InventSight now supports multiple delivery methods for Multi-Factor Authentication (MFA):

- **TOTP (Time-based One-Time Password)**: Using authenticator apps like Google Authenticator, Authy, etc.
- **Email OTP**: Receiving 6-digit codes via email
- **SMS OTP**: Receiving 6-digit codes via SMS/text message

This guide explains how to setup and use Email and SMS OTP for MFA.

## Table of Contents

1. [Setup Email OTP](#setup-email-otp)
2. [Setup SMS OTP](#setup-sms-otp)
3. [Changing MFA Delivery Method](#changing-mfa-delivery-method)
4. [Login with OTP](#login-with-otp)
5. [API Endpoints Reference](#api-endpoints-reference)
6. [Configuration](#configuration)
7. [Security Considerations](#security-considerations)

## Setup Email OTP

### Prerequisites
- MFA must be enabled for your account
- Valid email address registered with your account

### Steps

1. **Enable MFA** (if not already enabled):
   ```bash
   POST /api/auth/mfa/setup
   Authorization: Bearer <your-jwt-token>
   ```

2. **Change delivery method to Email**:
   ```bash
   PUT /api/auth/mfa/delivery-method
   Authorization: Bearer <your-jwt-token>
   Content-Type: application/json

   {
     "deliveryMethod": "EMAIL"
   }
   ```

3. **Verify the change** (check current settings):
   ```bash
   GET /api/auth/mfa/delivery-methods
   Authorization: Bearer <your-jwt-token>
   ```

### Response Example
```json
{
  "success": true,
  "message": "MFA delivery method updated successfully",
  "deliveryMethod": "EMAIL"
}
```

## Setup SMS OTP

### Prerequisites
- MFA must be enabled for your account
- Valid phone number in E.164 format (e.g., +1234567890)
- SMS service must be configured on the server (Twilio credentials)

### Steps

1. **Enable MFA** (if not already enabled):
   ```bash
   POST /api/auth/mfa/setup
   Authorization: Bearer <your-jwt-token>
   ```

2. **Change delivery method to SMS with phone number**:
   ```bash
   PUT /api/auth/mfa/delivery-method
   Authorization: Bearer <your-jwt-token>
   Content-Type: application/json

   {
     "deliveryMethod": "SMS",
     "phoneNumber": "+1234567890"
   }
   ```

3. **Request verification code**:
   ```bash
   POST /api/auth/mfa/send-otp
   Authorization: Bearer <your-jwt-token>
   Content-Type: application/json

   {
     "deliveryMethod": "SMS",
     "phoneNumber": "+1234567890"
   }
   ```

4. **Verify phone number with received code**:
   ```bash
   POST /api/auth/mfa/verify-phone?code=123456
   Authorization: Bearer <your-jwt-token>
   ```

### Phone Number Format
- Must be in E.164 format: `+[country code][phone number]`
- Examples:
  - US: `+12025551234`
  - UK: `+447700900123`
  - Australia: `+61212345678`

## Changing MFA Delivery Method

You can switch between TOTP, Email, and SMS at any time.

### Switch to TOTP (Authenticator App)
```bash
PUT /api/auth/mfa/delivery-method
Authorization: Bearer <your-jwt-token>
Content-Type: application/json

{
  "deliveryMethod": "TOTP"
}
```

### Switch to Email
```bash
PUT /api/auth/mfa/delivery-method
Authorization: Bearer <your-jwt-token>
Content-Type: application/json

{
  "deliveryMethod": "EMAIL"
}
```

### Switch to SMS
```bash
PUT /api/auth/mfa/delivery-method
Authorization: Bearer <your-jwt-token>
Content-Type: application/json

{
  "deliveryMethod": "SMS",
  "phoneNumber": "+1234567890"
}
```

## Login with OTP

### Email OTP Login Flow

1. **Initiate login** (credentials only):
   ```bash
   POST /api/auth/login
   Content-Type: application/json

   {
     "email": "user@example.com",
     "password": "password123"
   }
   ```

2. **Response indicates MFA required**:
   ```json
   {
     "error": "MFA_REQUIRED",
     "message": "Multi-factor authentication code is required",
     "preferredMethod": "EMAIL"
   }
   ```

3. **Request OTP code** (optional, auto-sent during login):
   ```bash
   POST /api/auth/mfa/send-otp
   Authorization: Bearer <your-jwt-token>
   Content-Type: application/json

   {
     "deliveryMethod": "EMAIL",
     "email": "user@example.com"
   }
   ```

4. **Complete login with OTP code**:
   ```bash
   POST /api/auth/login
   Content-Type: application/json

   {
     "email": "user@example.com",
     "password": "password123",
     "otpCode": "123456"
   }
   ```

### SMS OTP Login Flow

Same as Email OTP, but the code is sent via SMS to your verified phone number.

```bash
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "otpCode": "654321"
}
```

### TOTP Login (Backward Compatible)

TOTP continues to work as before using the `totpCode` parameter:

```bash
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "password123",
  "totpCode": 123456
}
```

## API Endpoints Reference

### Send OTP Code
```
POST /api/auth/mfa/send-otp
Authorization: Bearer <jwt-token>

Request Body:
{
  "deliveryMethod": "EMAIL" | "SMS",
  "email": "user@example.com",        // Required for EMAIL
  "phoneNumber": "+1234567890"        // Required for SMS
}

Response:
{
  "success": true,
  "message": "OTP code sent successfully via EMAIL",
  "deliveryMethod": "EMAIL"
}
```

### Verify OTP Code
```
POST /api/auth/mfa/verify-otp
Authorization: Bearer <jwt-token>

Request Body:
{
  "otpCode": "123456",
  "deliveryMethod": "EMAIL" | "SMS"
}

Response:
{
  "success": true,
  "message": "OTP code verified successfully"
}
```

### Update Delivery Method
```
PUT /api/auth/mfa/delivery-method
Authorization: Bearer <jwt-token>

Request Body:
{
  "deliveryMethod": "TOTP" | "EMAIL" | "SMS",
  "phoneNumber": "+1234567890"        // Required for SMS
}

Response:
{
  "success": true,
  "message": "MFA delivery method updated successfully",
  "deliveryMethod": "SMS"
}
```

### Verify Phone Number
```
POST /api/auth/mfa/verify-phone?code=123456
Authorization: Bearer <jwt-token>

Response:
{
  "success": true,
  "message": "Phone number verified successfully"
}
```

### Get Available Delivery Methods
```
GET /api/auth/mfa/delivery-methods
Authorization: Bearer <jwt-token>

Response:
{
  "success": true,
  "enabled": true,
  "preferredMethod": "EMAIL",
  "phoneVerified": false,
  "maskedPhoneNumber": "****1234",
  "availableMethods": ["TOTP", "EMAIL", "SMS"]
}
```

## Configuration

### Server Configuration

Add to `application.yml`:

```yaml
inventsight:
  mfa:
    otp:
      enabled: true
      expiry-minutes: 5
      code-length: 6
      max-attempts: 3
      rate-limit-window-minutes: 10
      max-sends-per-window: 3
  
  sms:
    enabled: true
    provider: twilio
    twilio:
      account-sid: ${TWILIO_ACCOUNT_SID}
      auth-token: ${TWILIO_AUTH_TOKEN}
      from-number: ${TWILIO_FROM_NUMBER}
```

### Environment Variables

Set these environment variables for SMS functionality:

```bash
export TWILIO_ACCOUNT_SID="your-account-sid"
export TWILIO_AUTH_TOKEN="your-auth-token"
export TWILIO_FROM_NUMBER="+1234567890"
export SMS_ENABLED=true
```

## Security Considerations

### OTP Code Security
- **Expiration**: OTP codes expire after 5 minutes
- **One-time use**: Each code can only be used once
- **Hashed storage**: Codes are hashed using BCrypt before storage
- **Rate limiting**: Maximum 3 OTP requests per 10-minute window

### Rate Limiting
- **OTP Generation**: Max 3 codes per 10 minutes
- **Verification Attempts**: Max 3 failed attempts before temporary lockout
- **Automatic reset**: Rate limits reset after successful verification

### Best Practices
1. **Use SMS for high-security accounts**: SMS provides device verification
2. **Keep email secure**: Ensure your email account has strong security
3. **Verify phone numbers**: Always verify phone numbers before using SMS OTP
4. **Monitor failed attempts**: Review activity logs for suspicious behavior
5. **Backup codes**: Generate and store backup codes for account recovery

### Audit Logging
All MFA operations are logged:
- OTP code generation
- OTP code verification (success/failure)
- Delivery method changes
- Phone number verification
- Rate limit violations

### Phone Number Security
- Phone numbers are stored in database (encryption recommended in production)
- Only last 4 digits shown in API responses
- Requires verification before use
- Can be updated at any time

## Troubleshooting

### OTP not received
1. Check rate limits: `GET /api/auth/mfa/delivery-methods`
2. Verify email/phone number is correct
3. Check spam folder (for email)
4. Verify SMS service is configured (for SMS)

### Invalid OTP code
- Code may have expired (5-minute window)
- Code may have been already used
- Ensure correct code from most recent message
- Check rate limiting hasn't locked account

### Phone number not accepted
- Verify E.164 format: `+[country code][number]`
- Remove spaces, dashes, or parentheses
- Include country code with + prefix

### SMS not working
- Verify Twilio credentials are configured
- Check `SMS_ENABLED` environment variable is `true`
- Verify phone number is verified
- Check Twilio account has credits/balance

## Migration from TOTP

Existing users with TOTP can seamlessly switch to Email or SMS OTP:

1. Current TOTP setup remains functional
2. Change delivery method: `PUT /api/auth/mfa/delivery-method`
3. No need to disable/re-enable MFA
4. Can switch back to TOTP at any time

## Support

For issues or questions:
- Check application logs for detailed error messages
- Review audit logs for MFA activity
- Contact system administrator for server configuration issues
