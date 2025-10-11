# InventSight API Enhancements Guide

## New Security & Compliance Features

This guide covers the new backend-only security, compliance, and internationalization features added to InventSight.

## Table of Contents

1. [Multi-Factor Authentication (MFA)](#multi-factor-authentication-mfa)
2. [Password Reset](#password-reset)
3. [Audit Trail](#audit-trail)
4. [Internationalization](#internationalization)
5. [Configuration](#configuration)

---

## Multi-Factor Authentication (MFA)

InventSight now supports TOTP-based MFA compatible with Google Authenticator, Authy, and other authenticator apps.

### Endpoints

#### 1. Setup MFA

**Request:**
```http
POST /api/auth/mfa/setup
Authorization: Bearer <jwt_token>
X-Tenant-ID: <company_uuid>
```

**Response:**
```json
{
  "success": true,
  "message": "MFA setup completed successfully",
  "system": "InventSight System",
  "timestamp": "2025-10-11T09:45:00"
}
```

**Usage Flow:**
1. Call setup endpoint
2. User scans QR code with authenticator app
3. Call verify endpoint with code from app
4. MFA is enabled

#### 2. Verify MFA Code

**Request:**
```http
POST /api/auth/mfa/verify
Authorization: Bearer <jwt_token>
X-Tenant-ID: <company_uuid>
Content-Type: application/json

{
  "code": 123456
}
```

**Response:**
```json
{
  "success": true,
  "message": "MFA verification successful"
}
```

#### 3. Generate Backup Codes

**Request:**
```http
POST /api/auth/mfa/backup-codes
Authorization: Bearer <jwt_token>
X-Tenant-ID: <company_uuid>
```

**Response:**
```json
{
  "success": true,
  "message": "Backup codes generated successfully. Codes: ABC12345, DEF67890, ...",
  "timestamp": "2025-10-11T09:45:00"
}
```

**Important:** Backup codes are one-time use. Store them securely.

#### 4. Check MFA Status

**Request:**
```http
GET /api/auth/mfa/status
Authorization: Bearer <jwt_token>
X-Tenant-ID: <company_uuid>
```

**Response:**
```json
{
  "success": true,
  "message": "MFA status: enabled"
}
```

#### 5. Disable MFA

**Request:**
```http
DELETE /api/auth/mfa/disable
Authorization: Bearer <jwt_token>
X-Tenant-ID: <company_uuid>
```

**Response:**
```json
{
  "success": true,
  "message": "MFA disabled successfully"
}
```

---

## Password Reset

Secure email-based password reset flow with time-limited, single-use tokens.

### Endpoints

#### 1. Request Password Reset

**Request:**
```http
POST /api/auth/password-reset/request
Content-Type: application/json

{
  "email": "user@example.com"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Password reset email sent"
}
```

**Note:** Response is always success to prevent email enumeration attacks.

#### 2. Validate Reset Token

**Request:**
```http
GET /api/auth/password-reset/validate?token=<reset_token>
```

**Response:**
```json
{
  "success": true,
  "message": "Token is valid"
}
```

#### 3. Confirm Password Reset

**Request:**
```http
POST /api/auth/password-reset/confirm
Content-Type: application/json

{
  "token": "<reset_token>",
  "newPassword": "NewSecurePassword123!"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Password reset successful"
}
```

---

## Audit Trail

All sensitive operations are automatically logged to an append-only audit trail with hash chaining for tamper-evidence.

### Automatically Logged Events

- User login/logout
- MFA setup/verification/disable
- Password reset requests
- Tenant switches
- Warehouse inventory movements
- Permission changes

### Event Structure

```json
{
  "id": "uuid",
  "eventAt": "2025-10-11T09:45:00",
  "actor": "user@example.com",
  "actorId": 123,
  "action": "MFA_ENABLED",
  "entityType": "User",
  "entityId": "123",
  "tenantId": "company-uuid",
  "companyId": "company-uuid",
  "ipAddress": "192.168.1.1",
  "userAgent": "Mozilla/5.0...",
  "detailsJson": "{}",
  "prevHash": "abc123...",
  "hash": "def456..."
}
```

### Query Audit Events

Audit events can be queried via the `AuditService`:

```java
Page<AuditEvent> events = auditService.findByTenant(tenantId, pageable);
Page<AuditEvent> events = auditService.findByCompany(companyId, pageable);
Page<AuditEvent> events = auditService.findByEntity(entityType, entityId, pageable);
```

---

## Internationalization

InventSight now supports multiple languages via the `Accept-Language` header.

### Supported Languages

- **English** (`en`) - Default
- **Burmese** (`my-MM`)

### Usage

**Request:**
```http
GET /api/auth/mfa/status
Accept-Language: my-MM
Authorization: Bearer <jwt_token>
```

**Response:**
```json
{
  "success": true,
  "message": "MFA အခြေအနေ: ဖွင့်ထား"
}
```

### Adding New Languages

1. Create `messages_<locale>.properties` in `src/main/resources/`
2. Add locale to `LocaleConfig.supportedLocales`
3. Restart application

---

## Configuration

### Email Service

Configure email provider in `application.yml`:

```yaml
inventsight:
  email:
    provider: smtp  # or ses, sendgrid
    from-address: noreply@yourdomain.com
    from-name: InventSight

spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${EMAIL_USERNAME}
    password: ${EMAIL_PASSWORD}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
```

### MFA Settings

```yaml
inventsight:
  mfa:
    issuer: "Your Company Name"
    totp:
      window-size: 3  # Allow 3 time windows for clock skew
```

### JWT Settings

```yaml
inventsight:
  security:
    jwt:
      secret: ${JWT_SECRET}
      expiration: 300000  # 5 minutes
      refresh-expiration: 604800000  # 7 days
```

### Feature Flags

```yaml
inventsight:
  security:
    oauth2:
      resource-server:
        enabled: false  # Enable OAuth2 JWT validation
    saml:
      enabled: false
      
  tenancy:
    header:
      enabled: false  # Enable X-Tenant-ID validation
```

---

## Error Handling

All endpoints return consistent error responses:

### Error Response Format

```json
{
  "success": false,
  "message": "Error description",
  "system": "InventSight System",
  "timestamp": "2025-10-11T09:45:00"
}
```

### HTTP Status Codes

- `200` - Success
- `400` - Bad Request (invalid input)
- `401` - Unauthorized (invalid/expired token)
- `403` - Forbidden (insufficient permissions)
- `422` - Unprocessable Entity (business logic error)
- `500` - Internal Server Error

---

## Security Best Practices

### MFA
1. Enable MFA for all privileged accounts
2. Store backup codes securely (offline, encrypted)
3. Regenerate backup codes periodically
4. Never share TOTP secrets

### Password Reset
1. Tokens expire after 1 hour
2. Tokens are single-use only
3. All requests are logged with IP address
4. Use HTTPS in production

### Audit Trail
1. Events are append-only (cannot be deleted)
2. Hash chaining provides tamper-evidence
3. Query audit logs regularly for security monitoring
4. Retain logs according to compliance requirements

### JWT Tokens
1. Access tokens expire after 5 minutes
2. Use refresh tokens for long-lived sessions
3. Store tokens securely (HttpOnly cookies recommended)
4. Never expose tokens in URLs

---

## Testing

### Manual Testing with cURL

#### MFA Setup
```bash
curl -X POST http://localhost:8080/api/auth/mfa/setup \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -H "X-Tenant-ID: YOUR_COMPANY_UUID"
```

#### Password Reset Request
```bash
curl -X POST http://localhost:8080/api/auth/password-reset/request \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com"}'
```

### Unit Tests

Run tests:
```bash
mvn test -Dtest=MfaServiceTest,AuditServiceTest
```

---

## Migration Guide

### Enabling Features

1. **Run Database Migrations**
   ```bash
   # Flyway will automatically run V2-V5 migrations
   mvn flyway:migrate
   ```

2. **Configure Email Service**
   - Set up SMTP/SES/SendGrid credentials
   - Test with password reset flow

3. **Enable Feature Flags**
   - Update `application.yml` or environment variables
   - Restart application

4. **Announce to Users**
   - MFA is optional by default
   - Users can enable via API or future UI

### Rollback Plan

If issues arise:
1. Disable feature flags
2. Migrations are forward-only (create rollback scripts if needed)
3. Existing functionality unaffected

---

## Support

For issues or questions:
1. Check `SECURITY_ENHANCEMENTS_IMPLEMENTATION.md`
2. Review application logs
3. Check database migration status: `SELECT * FROM flyway_schema_history;`
4. Contact development team

---

## Changelog

### Version 1.1.0 (2025-10-11)

**Added:**
- Multi-Factor Authentication (TOTP)
- Email-based password reset
- Append-only audit trail with hash chaining
- Internationalization (English, Burmese)
- JWT tenant binding
- Email service abstraction
- Comprehensive unit tests

**Changed:**
- JWT access token expiration reduced to 5 minutes
- Added feature flags for controlled rollout

**Deprecated:**
- None

**Security:**
- Enhanced authentication with MFA
- Tamper-evident audit logging
- Improved token hygiene
