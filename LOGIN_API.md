# InventSight Backend Login API Documentation

This document describes the login authentication endpoints implemented for the InventSight API, including the new email verification check feature.

## Login Endpoints

### 1. POST /auth/login - User Login

Main user login endpoint with email verification check.

**Request Body:**
```json
{
    "email": "user@example.com",
    "password": "userpassword"
}
```

**Features:**
- Email and password credential validation
- Email verification status check (after successful credential validation)
- JWT token generation for verified users
- Activity logging for all login attempts
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

**Response (Invalid Credentials - 401 Unauthorized):**
```json
{
    "message": "Invalid email or password"
}
```

**Response (Server Error - 500 Internal Server Error):**
```json
{
    "message": "Authentication service temporarily unavailable"
}
```

### 2. POST /auth/login/v2 - Structured User Login

Alternative login endpoint with structured response format for frontend compatibility.

**Request Body:**
```json
{
    "email": "user@example.com",
    "password": "userpassword"
}
```

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

**Response (Invalid Credentials - 401 Unauthorized):**
```json
{
    "message": "Invalid email or password",
    "success": false,
    "timestamp": "2025-09-12T23:35:00.000Z",
    "system": "InventSight"
}
```

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

## Activity Logging

All login attempts are logged with appropriate activity types:

- `USER_LOGIN` - Successful login with verified email
- `USER_LOGIN_V2` - Successful structured login with verified email  
- `USER_LOGIN_EMAIL_UNVERIFIED` - Failed login due to unverified email
- `USER_LOGIN_V2_EMAIL_UNVERIFIED` - Failed structured login due to unverified email
- `USER_LOGIN_FAILED` - Failed login due to invalid credentials

## Integration Notes

- Both endpoints require email verification to be enabled in the user's account (`emailVerified = true`)
- New user registrations have `emailVerified = false` by default
- Email verification can be completed via the `/auth/verify-email` endpoint
- Users with unverified emails cannot obtain JWT tokens through login endpoints