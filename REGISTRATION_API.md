# InventSight Backend Registration API Documentation

This document describes the comprehensive backend registration endpoints implemented for the InventSight API.

## Authentication Endpoints

### 1. POST /auth/register - User Registration

Main user registration endpoint with enhanced security features.

**Request Body:**
```json
{
    "username": "johndoe",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Doe"
}
```

**Features:**
- Password strength validation (8+ chars, uppercase, lowercase, digit, special char)
- Rate limiting (5 attempts per hour per IP/email combination)
- Duplicate email/username checking
- Secure password hashing with BCrypt
- Immediate JWT token generation for login
- Email verification token generation

**Response (Success - 201 Created):**
```json
{
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "type": "Bearer",
    "id": 1,
    "username": "johndoe",
    "email": "john@example.com",
    "fullName": "John Doe",
    "role": "USER",
    "system": "InventSight",
    "timestamp": "2025-08-28T06:16:45.123Z",
    "expiresIn": 86400000
}
```

**Response (Password Validation Error - 400 Bad Request):**
```json
{
    "success": false,
    "message": "Password does not meet security requirements",
    "errors": [
        "Password must contain at least one uppercase letter",
        "Password must contain at least one special character"
    ],
    "strengthScore": 45,
    "strength": "Fair",
    "timestamp": "2025-08-28T06:16:45.123Z"
}
```

---

### 2. GET /auth/check-email - Email Availability Check

Check if an email address is already registered.

**Parameters:**
- `email` (query parameter): Email address to check

**Example:**
```
GET /auth/check-email?email=john@example.com
```

**Response:**
```json
{
    "email": "john@example.com",
    "exists": false,
    "available": true,
    "timestamp": "2025-08-28T06:16:45.123Z",
    "system": "InventSight"
}
```

---

### 3. POST /auth/verify-email - Email Verification

Verify a user's email address using a verification token.

**Request Body:**
```json
{
    "token": "ABCD1234EFGH5678IJKL9012MNOP3456",
    "email": "john@example.com"
}
```

**Response (Success - 200 OK):**
```json
{
    "success": true,
    "message": "Email verified successfully",
    "email": "john@example.com",
    "timestamp": "2025-08-28T06:16:45.123Z",
    "system": "InventSight"
}
```

**Response (Invalid Token - 400 Bad Request):**
```json
{
    "message": "Invalid or expired verification token",
    "timestamp": "2025-08-28T06:16:45.123Z",
    "system": "InventSight"
}
```

---

### 4. POST /auth/resend-verification - Resend Verification Email

Resend email verification token to a user.

**Request Body:**
```json
{
    "email": "john@example.com"
}
```

**Features:**
- Rate limiting (10 attempts per hour per IP/email combination)
- Checks if user exists
- Checks if email is already verified
- Prevents duplicate token generation within valid timeframe

**Response (Success - 200 OK):**
```json
{
    "success": true,
    "message": "Verification email sent successfully",
    "email": "john@example.com",
    "timestamp": "2025-08-28T06:16:45.123Z",
    "system": "InventSight"
}
```

**Response (Already Verified - 400 Bad Request):**
```json
{
    "message": "Email is already verified",
    "timestamp": "2025-08-28T06:16:45.123Z",
    "system": "InventSight"
}
```

---

### 5. POST /auth/validate-password - Password Strength Validation

Validate password strength for frontend real-time feedback.

**Request Body:**
```json
{
    "password": "mypassword123"
}
```

**Response:**
```json
{
    "valid": false,
    "strengthScore": 65,
    "strength": "Fair",
    "errors": [
        "Password must contain at least one uppercase letter",
        "Password must contain at least one special character"
    ],
    "timestamp": "2025-08-28T06:16:45.123Z",
    "system": "InventSight"
}
```

---

## Security Features

### Password Requirements
- Minimum 8 characters, maximum 128 characters
- At least one lowercase letter (a-z)
- At least one uppercase letter (A-Z)
- At least one digit (0-9)
- At least one special character (!@#$%^&*()_+-=[]{}|;':\"\\,.<>/?)
- Cannot be a common password
- Cannot contain sequential characters (123, abc, etc.)

### Rate Limiting
- **Registration**: 5 attempts per hour per IP/email combination
- **Email Verification**: 10 attempts per hour per IP/email combination
- Rate limits reset automatically after 60 minutes
- Returns rate limit status in error responses

### Email Verification System
- Secure random token generation (32 characters)
- Tokens expire after 24 hours
- Tokens are single-use only
- Automatic cleanup of expired tokens
- Mock email sending (ready for real email service integration)

### Input Validation
- Jakarta Bean Validation annotations
- Email format validation
- Username and name length constraints
- SQL injection prevention through JPA
- Cross-site scripting (XSS) protection

---

## Database Schema Updates

### Users Table
New field added:
- `email_verified` (BOOLEAN, default: false): Indicates if user's email is verified

### New Table: email_verification_tokens
- `id` (BIGINT, Primary Key): Auto-generated token ID
- `token` (VARCHAR, Unique, Not Null): Verification token
- `email` (VARCHAR, Not Null): Associated email address
- `expires_at` (TIMESTAMP, Not Null): Token expiration time
- `created_at` (TIMESTAMP, Not Null): Token creation time
- `used_at` (TIMESTAMP, Nullable): When token was used
- `used` (BOOLEAN, Default: false): Whether token has been used

---

## Error Handling

### HTTP Status Codes
- `200 OK`: Successful operation
- `201 Created`: User registered successfully
- `400 Bad Request`: Validation errors, weak password, invalid token
- `404 Not Found`: User/resource not found
- `409 Conflict`: Duplicate email or username
- `429 Too Many Requests`: Rate limit exceeded
- `500 Internal Server Error`: Server-side errors

### Error Response Format
```json
{
    "message": "Error description",
    "timestamp": "2025-08-28T06:16:45.123Z",
    "system": "InventSight"
}
```

---

## Integration Notes

### Frontend Integration (React Native)
- All endpoints support CORS for cross-origin requests
- Consistent JSON response format
- Real-time password validation endpoint for better UX
- Rate limiting information provided for handling delays

### Email Service Integration
- Mock email sending is implemented in `EmailVerificationService`
- Ready for integration with SendGrid, AWS SES, or SMTP
- Email templates can be customized
- Verification links include frontend URL for seamless UX

### Production Considerations
- Replace in-memory rate limiting with Redis for scalability
- Configure proper JWT secrets in production
- Set up real email service with proper templates
- Implement proper logging and monitoring
- Consider adding CAPTCHA for additional bot protection