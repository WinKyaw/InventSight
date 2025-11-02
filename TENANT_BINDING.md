# Automatic Tenant Binding

## Overview

InventSight implements automatic tenant binding to provide a seamless multi-tenant experience. Users no longer need to specify a `tenantId` in login requests. The system automatically resolves the appropriate tenant based on user memberships and preferences.

## Key Features

- **No tenantId in Login Requests**: Users authenticate without specifying a tenant
- **Default Tenant Management**: Each user has a default tenant that is automatically selected
- **Automatic Registration Binding**: New users are automatically associated with their created company
- **Multi-Membership Support**: Users with multiple company memberships can select their preferred default
- **JWT-Only Tenancy**: Tenant context is exclusively managed via JWT claims, no headers required
- **MFA Integration**: Tenant resolution occurs after successful MFA verification

## How It Works

### 1. Registration Flow

When a user registers:
1. A new company is automatically created
2. The user is assigned as FOUNDER with full access
3. The user's `default_tenant_id` is set to the new company
4. A tenant-bound JWT is returned with `tenant_id` claim

**Example Request:**
```bash
POST /api/auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePass123!",
  "firstName": "John",
  "lastName": "Doe"
}
```

**Response:**
```json
{
  "token": "eyJhbGc...",
  "type": "Bearer",
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "fullName": "John Doe",
  "role": "USER",
  "system": "InventSight",
  "expiresIn": 86400000
}
```

The returned JWT contains a `tenant_id` claim set to the newly created company's UUID.

### 2. Login Flow

When a user logs in:

#### Scenario A: User has Default Tenant
- System checks `default_tenant_id`
- Validates user still has active membership
- Returns tenant-bound JWT with that tenant

#### Scenario B: User has Single Membership (No Default Set)
- System finds the single membership
- Automatically sets it as default tenant
- Returns tenant-bound JWT

#### Scenario C: User has Multiple Memberships (No Default Set)
- System returns HTTP 409 (Conflict) with `TENANT_SELECTION_REQUIRED`
- Response includes list of available companies
- User must call `/auth/tenant-select` to choose

#### Scenario D: User has No Memberships
- System returns HTTP 403 (Forbidden) with `NO_TENANT_MEMBERSHIP`
- User needs to be invited to a company

**Example Login Request:**
```bash
POST /api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "SecurePass123!",
  "totpCode": 123456  // Optional, required if MFA enabled
}
```

**Successful Response (Scenario A or B):**
```json
{
  "token": "eyJhbGc...",
  "type": "Bearer",
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "fullName": "John Doe",
  "role": "USER",
  "system": "InventSight",
  "expiresIn": 86400000
}
```

**Tenant Selection Required Response (Scenario C):**
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

### 3. Tenant Selection Endpoint

When a user receives `TENANT_SELECTION_REQUIRED`, they must call:

```bash
POST /api/auth/tenant-select
Authorization: Bearer <existing_jwt>
Content-Type: application/json

{
  "tenantId": "123e4567-e89b-12d3-a456-426614174000"
}
```

**Response:**
```json
{
  "token": "eyJhbGc...",
  "type": "Bearer",
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "fullName": "John Doe",
  "role": "USER",
  "system": "InventSight",
  "expiresIn": 86400000
}
```

The selected tenant is:
- Set as the user's `default_tenant_id`
- Included in the returned JWT's `tenant_id` claim
- Automatically used for all subsequent logins

### 4. Invite Acceptance Flow

When a user is invited to join an existing company:

```bash
POST /api/auth/invite/accept
Content-Type: application/json

{
  "inviteToken": "invite-token-string",
  "password": "SecurePass123!"  // Optional for existing users
}
```

This endpoint will:
1. Validate the invite token
2. Create user account if needed (or verify existing user)
3. Create company membership
4. Set default tenant to the invited company (if first company)
5. Return tenant-bound JWT

**Note:** Full invite flow implementation is pending invite service integration.

## JWT Structure

All authenticated requests use JWTs with the following structure:

```json
{
  "sub": "john@example.com",
  "userId": 1,
  "username": "john_doe",
  "fullName": "John Doe",
  "role": "USER",
  "tenant_id": "123e4567-e89b-12d3-a456-426614174000",
  "system": "InventSight",
  "tokenType": "access",
  "iat": 1730548800,
  "exp": 1730635200
}
```

The `tenant_id` claim is used by `CompanyTenantFilter` to:
- Set the database search path to the company's schema
- Validate user membership in the company
- Enforce multi-tenant isolation

## Security Considerations

### No X-Tenant-ID Header Required

By default, InventSight operates in **JWT-only mode** (`inventsight.tenancy.header.enabled=false`):
- The `X-Tenant-ID` header is NOT required
- Tenant context is exclusively derived from the JWT `tenant_id` claim
- This prevents header manipulation attacks

### MFA Integration

Multi-factor authentication is enforced before tenant resolution:
1. Username/password validation
2. TOTP code verification (if MFA enabled)
3. Tenant resolution and JWT generation
4. Return tenant-bound token

This ensures tenant information is only disclosed after full authentication.

### Membership Validation

Every request validates:
- Company exists and is active
- User has active membership in the company
- JWT signature is valid and not expired

## Database Schema

The `users` table includes:

```sql
ALTER TABLE public.users 
ADD COLUMN default_tenant_id UUID NULL;

ALTER TABLE public.users
ADD CONSTRAINT fk_users_default_tenant_id 
FOREIGN KEY (default_tenant_id) 
REFERENCES public.companies(id) 
ON DELETE SET NULL;

CREATE INDEX idx_users_default_tenant_id 
ON public.users(default_tenant_id);
```

## Configuration

Default configuration in `application.yml`:

```yaml
inventsight:
  security:
    local-login:
      enabled: true  # Enable local authentication
    jwt:
      expiration: 86400000  # 24 hours
  tenancy:
    header:
      enabled: false  # JWT-only mode (no X-Tenant-ID header)
      validate-against-jwt: true  # If header enabled, validate against JWT
```

## Client Implementation Guide

### Frontend Login Flow

```javascript
// 1. Login attempt
const loginResponse = await fetch('/api/auth/login', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    email: 'john@example.com',
    password: 'SecurePass123!',
    totpCode: totpCode // if MFA enabled
  })
});

if (loginResponse.status === 409) {
  // Tenant selection required
  const selectionData = await loginResponse.json();
  
  // Display company selection UI to user
  const selectedTenantId = await showTenantSelection(selectionData.companies);
  
  // 2. Select tenant
  const selectResponse = await fetch('/api/auth/tenant-select', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${temporaryToken}`  // From initial login
    },
    body: JSON.stringify({ tenantId: selectedTenantId })
  });
  
  const authData = await selectResponse.json();
  storeToken(authData.token);
  
} else if (loginResponse.ok) {
  // Login successful with automatic tenant
  const authData = await loginResponse.json();
  storeToken(authData.token);
} else {
  // Handle other errors
  handleError(loginResponse);
}
```

### Making Authenticated Requests

```javascript
// All subsequent requests simply include the JWT
const response = await fetch('/api/products', {
  method: 'GET',
  headers: {
    'Authorization': `Bearer ${storedToken}`
    // No X-Tenant-ID header needed!
  }
});
```

## Migration Notes

### From Header-Based to JWT-Only Tenancy

If migrating from a header-based system:

1. Ensure all users have `default_tenant_id` set
2. Update frontend to stop sending `X-Tenant-ID` headers
3. Verify JWT generation includes `tenant_id` claim
4. Set `inventsight.tenancy.header.enabled=false`

### Backward Compatibility

To maintain backward compatibility temporarily:

```yaml
inventsight:
  tenancy:
    header:
      enabled: true  # Allow X-Tenant-ID header
      validate-against-jwt: true  # But validate it matches JWT
```

This allows gradual migration of client applications.

## Troubleshooting

### "tenant_id claim is required but not found in JWT"

**Cause:** JWT was generated without tenant_id claim.

**Solution:** Ensure user has completed login flow properly. User may need to:
1. Call `/auth/tenant-select` if multiple memberships
2. Complete registration flow
3. Be invited to a company

### "Access denied: user is not a member of the specified company"

**Cause:** User's membership was revoked or company deleted.

**Solution:** User needs to re-authenticate to get fresh tenant resolution.

### "NO_TENANT_MEMBERSHIP: No active tenant membership found"

**Cause:** User has no active company memberships.

**Solution:** User needs to be invited to a company by an existing member.

## API Reference

### POST /api/auth/login
Authenticate user and return tenant-bound JWT.

### POST /api/auth/register  
Register new user, create company, return tenant-bound JWT.

### POST /api/auth/tenant-select
Select default tenant for users with multiple memberships.

### POST /api/auth/invite/accept
Accept invite and join existing company (pending full implementation).

## Support

For questions or issues related to tenant binding, please contact the development team or refer to the main [LOGIN_API.md](./LOGIN_API.md) documentation.
