# InventSight Backend Login API Documentation

This document describes the login authentication endpoints implemented for the InventSight API, including automatic tenant binding, email verification, and MFA support.

> **🎯 New Feature: Automatic Tenant Binding**  
> Users no longer need to provide `tenantId` in login requests! The system automatically resolves the appropriate tenant based on user memberships. See [TENANT_BINDING.md](./TENANT_BINDING.md) for detailed documentation.

## Login Endpoints

### 1. POST /auth/login - User Login

Main user login endpoint with **automatic tenant binding**, email verification check, and MFA support.

**Request Body:**
```json
{
    "email": "user@example.com",
    "password": "userpassword",
    "totpCode": 123456
}
```

**Request Fields:**
- `email` (required): User's email address
- `password` (required): User's password
- `totpCode` (optional): 6-digit TOTP code for multi-factor authentication. Required when user has MFA enabled

**Features:**
- Email and password credential validation
- Multi-factor authentication (MFA) enforcement for users with MFA enabled
- **Automatic tenant resolution** - no tenantId required in request
- Email verification status check (after successful credential validation)
- JWT token generation with automatic `tenant_id` claim
- Activity logging for all login attempts (including MFA events)
- Security against user enumeration attacks

**Response (Success - 200 OK):**
```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "type": "Bearer",
    "id": 1,
    "username": "johndoe", 
    "email": "user@example.com",
    "fullName": "John Doe",
    "role": "USER",
    "system": "InventSight",
    "expiresIn": 86400000
}
```

**Response (Email Not Verified - 401 Unauthorized):**
```json
{
    "message": "Email not verified. Please verify your email before logging in."
}
```

**Response (MFA Required - 401 Unauthorized):**
When user has MFA enabled but no MFA code is provided:
```json
{
    "error": "MFA_REQUIRED",
    "message": "Multi-factor authentication code is required",
    "preferredMethod": "EMAIL"
}
```

**Note on MFA Delivery Methods:**
- `preferredMethod` indicates the user's preferred MFA method: `TOTP`, `EMAIL`, or `SMS`
- For TOTP: Provide `totpCode` (6-digit number) from authenticator app
- For EMAIL/SMS: Provide `otpCode` (6-digit string) received via email or SMS

**Request Body with OTP (Email/SMS):**
```json
{
    "email": "user@example.com",
    "password": "userpassword",
    "otpCode": "123456"
}
```

**Request Body with TOTP (Authenticator App):**
```json
{
    "email": "user@example.com",
    "password": "userpassword",
    "totpCode": 123456
}
```

**Response (Invalid MFA Code - 401 Unauthorized):**
When user has MFA enabled and the provided code (TOTP or OTP) is invalid:
```json
{
    "error": "MFA_INVALID_CODE",
    "message": "Invalid multi-factor authentication code"
}
```

**Response (Invalid Credentials - 401 Unauthorized):**
```json
{
    "message": "Invalid email or password"
}
```

**Response (Tenant Selection Required - 409 Conflict):**
When user has multiple company memberships and no default tenant set:
```json
{
    "error": "TENANT_SELECTION_REQUIRED",
    "message": "Multiple tenant memberships found. Please select a tenant to continue.",
    "companies": [
        {
            "companyId": "123e4567-e89b-12d3-a456-426614174000",
            "displayName": "Acme Corp",
            "role": "MANAGER",
            "isActive": true
        },
        {
            "companyId": "987e6543-e21b-98d7-b654-426614174001",
            "displayName": "Tech Solutions Inc",
            "role": "EMPLOYEE",
            "isActive": true
        }
    ],
    "timestamp": "2025-11-02T11:00:00"
}
```
> **Action Required:** Call `POST /auth/tenant-select` with chosen `tenantId` to complete login.

**Response (No Tenant Membership - 403 Forbidden):**
When user is not a member of the specified company:
```json
{
    "message": "Access denied: user is not a member of the specified company."
}
```

**Response (Server Error - 500 Internal Server Error):**
```json
{
    "message": "Authentication service temporarily unavailable"
}
```

### 2. POST /auth/login/v2 - Structured User Login

Alternative login endpoint with structured response format for frontend compatibility. Supports the same MFA and tenant binding features as `/auth/login`.

**Request Body:**
```json
{
    "email": "user@example.com",
    "password": "userpassword",
    "tenantId": "123e4567-e89b-12d3-a456-426614174000",
    "totpCode": 123456
}
```

**Request Fields:**
- `email` (required): User's email address
- `password` (required): User's password
- `tenantId` (optional): Company UUID for tenant-bound JWT (offline mode)
- `totpCode` (optional): 6-digit TOTP code for multi-factor authentication. Required when user has MFA enabled

**Response (Success - 200 OK):**
```json
{
    "user": {
        "id": 1,
        "email": "user@example.com",
        "firstName": "John",
        "lastName": "Doe",
        "username": "johndoe",
        "role": "USER"
    },
    "tokens": {
        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
        "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
        "accessTokenExpiresIn": 86400000,
        "refreshTokenExpiresIn": 604800000
    },
    "message": "Login successful",
    "success": true,
    "timestamp": "2025-09-12T23:35:00.000Z",
    "system": "InventSight"
}
```

**Response (Email Not Verified - 401 Unauthorized):**
```json
{
    "message": "Email not verified. Please verify your email before logging in.",
    "success": false,
    "timestamp": "2025-09-12T23:35:00.000Z",
    "system": "InventSight"
}
```

**Response (MFA Required - 401 Unauthorized):**
```json
{
    "message": "Multi-factor authentication code is required",
    "success": false
}
```

**Response (Invalid MFA Code - 401 Unauthorized):**
```json
{
    "message": "Invalid multi-factor authentication code",
    "success": false
}
```

**Response (Invalid Credentials - 401 Unauthorized):**
```json
{
    "message": "Invalid email or password",
    "success": false,
    "timestamp": "2025-09-12T23:35:00.000Z",
    "system": "InventSight"
}
```

## Multi-Factor Authentication (MFA)

InventSight supports TOTP-based multi-factor authentication using authenticator apps like Google Authenticator, Authy, or Microsoft Authenticator.

### MFA Login Flow

1. **User enters credentials**: Email and password are validated first
2. **MFA check**: If user has MFA enabled, the system checks for `totpCode` in the request
3. **TOTP validation**:
   - If `totpCode` is missing → return `401 Unauthorized` with error code `MFA_REQUIRED`
   - If `totpCode` is invalid → return `401 Unauthorized` with error code `MFA_INVALID_CODE`
   - If `totpCode` is valid → proceed with login
4. **Audit logging**: All MFA events are logged (`MFA_REQUIRED`, `MFA_VERIFIED`, `MFA_FAILED`)

### TOTP Code Requirements

- 6-digit numeric code
- Valid within a time window (typically 30 seconds)
- Supports clock skew tolerance (3 time windows)

### Example: Login with MFA

**First attempt (no TOTP code):**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123"
  }'
```

**Response (MFA Required):**
```json
{
  "error": "MFA_REQUIRED",
  "message": "Multi-factor authentication code is required"
}
```

**Second attempt (with TOTP code):**
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123",
    "totpCode": 123456
  }'
```

**Response (Success):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "email": "user@example.com",
  ...
}
```

## Tenant Binding for Offline Mode

InventSight supports tenant-bound JWTs for offline usage, allowing the mobile app to work without network connectivity.

### How Tenant Binding Works

1. **Login with tenantId**: Include the company UUID in the login request
2. **Validation**: System validates:
   - `tenantId` is a valid UUID format
   - Company exists
   - User has active membership in the company
3. **Tenant-bound JWT**: If valid, JWT includes `tenant_id` claim
4. **Protected routes**: All subsequent API calls use JWT for tenant identification (no `X-Tenant-ID` header needed)

### Example: Login with Tenant Binding

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "password123",
    "tenantId": "123e4567-e89b-12d3-a456-426614174000"
  }'
```

**Response:**
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",  // JWT includes tenant_id claim
  "email": "user@example.com",
  ...
}
```

### JWT Payload (with tenant_id)

```json
{
  "sub": "user@example.com",
  "userId": 1,
  "username": "johndoe",
  "role": "USER",
  "tenant_id": "123e4567-e89b-12d3-a456-426614174000",
  "iat": 1699891234,
  "exp": 1699977634
}
```

### Tenant Resolution

- **JWT-only mode (default)**: Tenant is always resolved from the `tenant_id` claim in the JWT
- **No header required**: `X-Tenant-ID` header is ignored
- **CompanyTenantFilter**: Validates JWT tenant claim and ensures user membership before granting access

## Email Verification Security Model

The login endpoints implement a security-conscious approach to email verification:

### Security/UX Tradeoff Implementation

1. **Credential Validation First**: Both email and password are validated using Spring Security's standard authentication mechanism
2. **Email Verification Check**: Only after successful credential validation, the system checks the user's email verification status
3. **Differentiated Error Messages**: 
   - Users with correct credentials but unverified email receive: "Email not verified. Please verify your email before logging in."
   - Users with incorrect credentials receive: "Invalid email or password"

### Security Benefits

- **Prevents User Enumeration**: Invalid credentials don't reveal whether an email exists in the system
- **Maintains Privacy**: Only users who know the correct password can learn about email verification status
- **Consistent Security Model**: Failed authentication attempts are logged consistently

### User Experience Benefits  

- **Clear Guidance**: Verified users with correct credentials get specific instructions about email verification
- **Actionable Feedback**: Users know exactly what action to take (verify email vs. check credentials)
- **Reduced Confusion**: Eliminates ambiguity about login failures

## Tenant Selection Endpoint

### POST /auth/tenant-select - Select Default Tenant

When a user receives `TENANT_SELECTION_REQUIRED` (409) response from login, they must call this endpoint to select their preferred default tenant.

**Authentication Required:** Yes (Bearer token from initial login attempt)

**Request Body:**
```json
{
    "tenantId": "123e4567-e89b-12d3-a456-426614174000"
}
```

**Request Fields:**
- `tenantId` (required): UUID of the company to set as default tenant

**Response (Success - 200 OK):**
```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "type": "Bearer",
    "id": 1,
    "username": "johndoe",
    "email": "user@example.com",
    "fullName": "John Doe",
    "role": "USER",
    "system": "InventSight",
    "expiresIn": 86400000
}
```

**Response (Invalid Tenant ID - 400 Bad Request):**
```json
{
    "message": "Invalid tenant ID format. Must be a valid UUID."
}
```

**Response (Tenant Not Found - 404 Not Found):**
```json
{
    "message": "Company not found for the specified tenant ID."
}
```

**Response (No Membership - 403 Forbidden):**
```json
{
    "message": "Access denied: user is not a member of the specified company."
}
```

**Features:**
- Sets the selected tenant as user's default for future logins
- Returns tenant-bound JWT with `tenant_id` claim
- Validates user has active membership in selected company
- Subsequent logins will automatically use this default tenant

## Activity Logging

All login attempts are logged with appropriate activity types:

- `USER_LOGIN` - Successful login with verified email
- `USER_LOGIN_V2` - Successful structured login with verified email  
- `USER_LOGIN_EMAIL_UNVERIFIED` - Failed login due to unverified email
- `USER_LOGIN_V2_EMAIL_UNVERIFIED` - Failed structured login due to unverified email
- `USER_LOGIN_FAILED` - Failed login due to invalid credentials
- `TENANT_SELECTED` - User selected default tenant via /auth/tenant-select
- `MFA_REQUIRED` - Login attempt without MFA code when MFA enabled
- `MFA_FAILED` - Login attempt with invalid MFA code
- `MFA_VERIFIED` - Successful MFA verification

## Integration Notes

- Login endpoints no longer require `tenantId` in request body
- Automatic tenant binding occurs after MFA verification (if enabled)
- Email verification is required for login (`emailVerified = true`)
- New user registrations automatically set default tenant to created company
- For detailed tenant binding information, see [TENANT_BINDING.md](./TENANT_BINDING.md)
- Users with unverified emails cannot obtain JWT tokens through login endpoints
## MFA OTP Support (Email and SMS)

InventSight now supports three MFA delivery methods:
- **TOTP**: Time-based One-Time Password using authenticator apps (Google Authenticator, Authy)
- **Email OTP**: 6-digit codes sent via email
- **SMS OTP**: 6-digit codes sent via SMS/text message

### MFA OTP Endpoints

#### POST /auth/mfa/send-otp - Send OTP Code

Send OTP code via email or SMS.

**Request:**
```json
{
    "deliveryMethod": "EMAIL",
    "email": "user@example.com"
}
```
or
```json
{
    "deliveryMethod": "SMS",
    "phoneNumber": "+1234567890"
}
```

**Response:**
```json
{
    "success": true,
    "message": "OTP code sent successfully via EMAIL",
    "deliveryMethod": "EMAIL"
}
```

#### POST /auth/mfa/verify-otp - Verify OTP Code

Verify an OTP code.

**Request:**
```json
{
    "otpCode": "123456",
    "deliveryMethod": "EMAIL"
}
```

**Response:**
```json
{
    "success": true,
    "message": "OTP code verified successfully"
}
```

#### PUT /auth/mfa/delivery-method - Update MFA Delivery Method

Change preferred MFA delivery method.

**Request:**
```json
{
    "deliveryMethod": "EMAIL"
}
```
or (for SMS):
```json
{
    "deliveryMethod": "SMS",
    "phoneNumber": "+1234567890"
}
```

**Response:**
```json
{
    "success": true,
    "message": "MFA delivery method updated successfully",
    "deliveryMethod": "EMAIL"
}
```

#### POST /auth/mfa/verify-phone - Verify Phone Number

Verify phone number for SMS OTP delivery.

**Request:**
```
POST /auth/mfa/verify-phone?code=123456
```

**Response:**
```json
{
    "success": true,
    "message": "Phone number verified successfully"
}
```

#### GET /auth/mfa/delivery-methods - Get Available Methods

Get available MFA delivery methods and current settings.

**Response:**
```json
{
    "success": true,
    "enabled": true,
    "preferredMethod": "EMAIL",
    "phoneVerified": false,
    "maskedPhoneNumber": "****1234",
    "availableMethods": ["TOTP", "EMAIL", "SMS"]
}
```

### OTP Login Flow

1. **Attempt login with credentials only**:
   ```bash
   POST /auth/login
   {
     "email": "user@example.com",
     "password": "password123"
   }
   ```

2. **Receive MFA_REQUIRED response**:
   ```json
   {
     "error": "MFA_REQUIRED",
     "message": "Multi-factor authentication code is required",
     "preferredMethod": "EMAIL"
   }
   ```

3. **Complete login with OTP code** (received via email/SMS):
   ```bash
   POST /auth/login
   {
     "email": "user@example.com",
     "password": "password123",
     "otpCode": "123456"
   }
   ```

### OTP Security Features

- **5-minute expiration**: OTP codes are valid for 5 minutes only
- **One-time use**: Each OTP code can only be used once
- **Rate limiting**: Maximum 3 OTP requests per 10 minutes
- **Hashed storage**: OTP codes are hashed with BCrypt before storage
- **Audit logging**: All OTP operations are logged for security monitoring

### Configuration

OTP and SMS settings in `application.yml`:

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

For detailed MFA OTP setup and usage, see [MFA_OTP_GUIDE.md](./MFA_OTP_GUIDE.md).

## Backward Compatibility

All existing TOTP users continue to work without any changes:
- `totpCode` parameter remains supported
- Default delivery method is TOTP for existing users
- Users can switch to Email/SMS OTP at any time
- No breaking changes to existing API contracts
