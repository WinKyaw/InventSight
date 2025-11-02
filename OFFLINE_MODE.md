# InventSight Offline Mode

## Overview

InventSight supports **offline mode** for scenarios where internet connectivity or Identity Provider (IdP) access is unavailable. This mode enables local authentication with tenant-bound JWT tokens while maintaining security and multi-tenancy isolation.

## Configuration

### Default (Production) Mode
By default, InventSight runs in **OAuth2-only mode**:
- OAuth2 login is enabled (Google, Microsoft, Okta)
- Local email/password login endpoints are **disabled**
- JWT-only tenant resolution (tenant_id must be in JWT claims)
- X-Tenant-ID header is **ignored**

### Enabling Offline Mode

To enable offline mode, activate the `offline` profile:

```bash
java -jar inventsight.jar --spring.profiles.active=offline
```

Or with Maven:

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=offline
```

### Offline Profile Configuration

The offline profile (`application-offline.yml`) configures:
- **Local login enabled**: `inventsight.security.local-login.enabled=true`
- **OAuth2 login disabled**: `inventsight.security.oauth2.login.enabled=false`
- **Resource server disabled**: `inventsight.security.oauth2.resource-server.enabled=false`
- **JWT-only tenancy**: `inventsight.tenancy.header.enabled=false` (still enforced)
- **Extended JWT TTL**: 24 hours for access tokens (configurable)

## Authentication in Offline Mode

### Login with Tenant Binding

In offline mode, you can authenticate and receive a **tenant-bound JWT** by including the `tenantId` in your login request:

#### Request

```bash
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "yourpassword",
  "tenantId": "123e4567-e89b-12d3-a456-426614174000"
}
```

#### Response

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "id": 1,
  "username": "user",
  "email": "user@example.com",
  "fullName": "User Name",
  "role": "USER",
  "system": "InventSight",
  "expiresIn": 86400000
}
```

The returned JWT token will contain a `tenant_id` claim that identifies the company context.

### Login without Tenant (Not Recommended)

You can also login without specifying a `tenantId`, but subsequent API calls will **fail** because the JWT-only tenant mode requires the `tenant_id` claim:

```bash
POST /api/auth/login
Content-Type: application/json

{
  "email": "user@example.com",
  "password": "yourpassword"
}
```

**Note**: This JWT cannot access tenant-scoped resources and will return `400 Bad Request` with error: "tenant_id claim is required in JWT token".

## Tenant Validation

When you provide a `tenantId` during login, the system validates:

1. **UUID Format**: The `tenantId` must be a valid UUID format
2. **Company Exists**: The company (tenant) must exist in the database
3. **User Membership**: The authenticated user must have an active membership in the specified company

If any validation fails, you'll receive an appropriate error response.

## JWT Token Structure

### Tenant-Bound JWT Claims

```json
{
  "sub": "user@example.com",
  "userId": 1,
  "username": "user",
  "fullName": "User Name",
  "role": "USER",
  "tenant_id": "123e4567-e89b-12d3-a456-426614174000",
  "system": "InventSight",
  "tokenType": "access",
  "iat": 1698765432,
  "exp": 1698851832
}
```

### Regular JWT (No Tenant)

```json
{
  "sub": "user@example.com",
  "userId": 1,
  "username": "user",
  "fullName": "User Name",
  "role": "USER",
  "system": "InventSight",
  "tokenType": "access",
  "iat": 1698765432,
  "exp": 1698851832
}
```

## Using Tenant-Bound JWT

Once you have a tenant-bound JWT, include it in the `Authorization` header for all API requests:

```bash
GET /api/products
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

The `CompanyTenantFilter` will:
1. Extract the `tenant_id` from the JWT
2. Validate the company exists
3. Verify user membership
4. Set the PostgreSQL search path to the company's schema (`company_<uuid>`)
5. Execute the request in the tenant context

## Security Considerations

### JWT-Only Tenancy
Even in offline mode, **JWT-only tenancy is enforced**:
- The `X-Tenant-ID` header is **ignored** (default: `inventsight.tenancy.header.enabled=false`)
- All tenant identification comes from the `tenant_id` claim in the JWT
- This prevents tenant impersonation attacks

### Token Expiration
- **Access Token**: 24 hours (offline default)
- **Refresh Token**: 7 days

You can customize these values in `application-offline.yml`:

```yaml
inventsight:
  security:
    jwt:
      expiration: 86400000 # 24 hours in milliseconds
      refresh-expiration: 604800000 # 7 days in milliseconds
```

For longer offline periods, increase these values appropriately.

### User Membership Validation
The system validates user membership at:
1. **Login time**: When issuing the tenant-bound JWT
2. **Request time**: Before executing any tenant-scoped operation

This ensures users can only access companies they're authorized for.

## Environment Variables

### Required for Offline Mode
None. Offline mode uses local authentication and doesn't require external services.

### Optional Customizations
- `JWT_SECRET`: Custom JWT signing secret (default: configured in application.yml)
- `JWT_EXPIRATION`: Token expiration in milliseconds (default: 86400000 = 24 hours)

## Switching Between Modes

### Production to Offline
```bash
# Stop the application
# Restart with offline profile
java -jar inventsight.jar --spring.profiles.active=offline
```

### Offline to Production
```bash
# Stop the application
# Restart with oauth-login profile and required env vars
export GOOGLE_CLIENT_ID=your-google-client-id
export GOOGLE_CLIENT_SECRET=your-google-client-secret
# ... other OAuth2 provider credentials

java -jar inventsight.jar --spring.profiles.active=oauth-login
```

## Troubleshooting

### "tenant_id claim is required in JWT token"
**Cause**: You're using a JWT without a `tenant_id` claim in JWT-only tenant mode.

**Solution**: Login with `tenantId` in the request body to receive a tenant-bound JWT.

### "Company not found for the specified tenant ID"
**Cause**: The provided `tenantId` doesn't exist in the database.

**Solution**: Verify the company UUID is correct and exists in the system.

### "Access denied: user is not a member of the specified company"
**Cause**: The authenticated user doesn't have membership in the specified company.

**Solution**: Ensure the user has an active `CompanyStoreUser` record for the company.

### "Invalid tenant ID format"
**Cause**: The provided `tenantId` is not a valid UUID.

**Solution**: Ensure the `tenantId` follows UUID format: `xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx`

## API Endpoints

### Available in Offline Mode
- `POST /api/auth/login` - Local authentication with optional tenant binding
- `POST /api/auth/login/v2` - Structured login response
- `POST /api/auth/register` - User registration
- `POST /api/auth/signup` - Alternative registration endpoint
- `GET /api/auth/check-email` - Email availability check
- `POST /api/auth/verify-email` - Email verification
- `POST /api/auth/resend-verification` - Resend verification email
- `POST /api/auth/validate-password` - Password strength validation
- `POST /api/auth/refresh` - Token refresh
- `GET /api/auth/me` - Current user profile
- `POST /api/auth/logout` - Logout

### Not Available in Offline Mode
- `/oauth2/**` - OAuth2 login endpoints (disabled)
- `/login/**` - OAuth2 callback endpoints (disabled)

## Examples

### Complete Offline Login Flow

1. **Login with tenant binding**:
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@company.com",
    "password": "SecurePass123!",
    "tenantId": "123e4567-e89b-12d3-a456-426614174000"
  }'
```

2. **Use the JWT for tenant-scoped operations**:
```bash
curl -X GET http://localhost:8080/api/products \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

3. **Refresh the token before expiration**:
```bash
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
```

## Best Practices

1. **Always specify tenantId during login** in offline mode to receive tenant-bound JWTs
2. **Monitor token expiration** and refresh before they expire
3. **Use strong passwords** - password validation is still enforced
4. **Secure the JWT secret** - change the default secret in production
5. **Plan for token renewal** - tokens expire after 24 hours by default
6. **Test offline mode** in development before deploying

## Related Documentation

- [OAuth2 Login Configuration](OAUTH2_LOGIN_CONFIGURATION.md)
- [Company Tenancy Documentation](COMPANY_TENANCY_DOCUMENTATION.md)
- [Security Enhancements Implementation](SECURITY_ENHANCEMENTS_IMPLEMENTATION.md)
- [JWT Configuration](README.md#jwt-configuration)
