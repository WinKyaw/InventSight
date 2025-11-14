# Authentication Flow Debugging Guide

## Overview

This document describes the comprehensive logging added to the JWT authentication flow to help diagnose authentication issues.

## Problem Statement

The JWT authentication was failing with "401 Unauthorized" errors. The logs showed:
- JWT token was being extracted successfully
- Tenant ID was retrieved from JWT
- However, the Authentication object in SecurityContext was null
- This suggested the JWT filter was not properly setting authentication before CompanyTenantFilter ran

## Solution

Comprehensive DEBUG-level logging has been added throughout the authentication filter chain to help identify where authentication fails.

## Enabling Debug Logging

To enable detailed authentication flow logging, add this to your `application.properties` or `application.yml`:

### application.properties
```properties
logging.level.com.pos.inventsight.config.AuthTokenFilter=DEBUG
logging.level.com.pos.inventsight.config.SecurityConfig=DEBUG
logging.level.com.pos.inventsight.tenant.CompanyTenantFilter=DEBUG
```

### application.yml
```yaml
logging:
  level:
    com.pos.inventsight.config.AuthTokenFilter: DEBUG
    com.pos.inventsight.config.SecurityConfig: DEBUG
    com.pos.inventsight.tenant.CompanyTenantFilter: DEBUG
```

## Log Output Explained

### 1. SecurityConfig Initialization Logs

When the application starts, you'll see:
```
INFO  c.p.i.config.SecurityConfig - === Initializing Spring Security Configuration ===
INFO  c.p.i.config.SecurityConfig - OAuth2 Resource Server enabled: false
INFO  c.p.i.config.SecurityConfig - OAuth2 Login enabled: true
INFO  c.p.i.config.SecurityConfig - Local Login enabled: false
INFO  c.p.i.config.SecurityConfig - Adding RateLimitingFilter before UsernamePasswordAuthenticationFilter
INFO  c.p.i.config.SecurityConfig - Adding AuthTokenFilter before UsernamePasswordAuthenticationFilter
INFO  c.p.i.config.SecurityConfig - Adding CompanyTenantFilter after AuthTokenFilter
INFO  c.p.i.config.SecurityConfig - Adding IdempotencyKeyFilter after CompanyTenantFilter
INFO  c.p.i.config.SecurityConfig - === Filter chain configured: RateLimiting -> AuthToken -> CompanyTenant -> Idempotency ===
```

This confirms:
- Which authentication modes are enabled
- The order filters are added to the chain
- The complete filter execution order

### 2. AuthTokenFilter Request Processing Logs

For each authenticated request, you'll see:
```
DEBUG c.p.i.config.AuthTokenFilter - === AuthTokenFilter START === Request: GET /api/products
DEBUG c.p.i.config.AuthTokenFilter - Authorization header present: true
DEBUG c.p.i.config.AuthTokenFilter - JWT token extracted from Authorization header (length: 245)
DEBUG c.p.i.config.AuthTokenFilter - Validating JWT token...
DEBUG c.p.i.config.AuthTokenFilter - JWT validation successful
DEBUG c.p.i.config.AuthTokenFilter - Username from JWT: user@example.com
DEBUG c.p.i.config.AuthTokenFilter - Tenant ID from JWT: 87b6a00e-896a-4f69-b9cd-3349d50c1578
DEBUG c.p.i.config.AuthTokenFilter - Loading user details for username: user@example.com
DEBUG c.p.i.config.AuthTokenFilter - User details loaded successfully. User type: User
DEBUG c.p.i.config.AuthTokenFilter - User authorities: [ROLE_USER]
DEBUG c.p.i.config.AuthTokenFilter - Setting authentication in SecurityContext
DEBUG c.p.i.config.AuthTokenFilter - Authentication set in SecurityContext: true
DEBUG c.p.i.config.AuthTokenFilter - Principal type: User
INFO  c.p.i.config.AuthTokenFilter - Authentication successful for user: user@example.com on GET /api/products
DEBUG c.p.i.config.AuthTokenFilter - === AuthTokenFilter END === Proceeding to next filter
```

This shows:
- JWT token extraction and validation
- User loading from database
- SecurityContext population
- Authentication verification

### 3. CompanyTenantFilter Request Processing Logs

After AuthTokenFilter, you'll see:
```
DEBUG c.p.i.tenant.CompanyTenantFilter - === CompanyTenantFilter START === Request: GET /api/products
DEBUG c.p.i.tenant.CompanyTenantFilter - Authentication at CompanyTenantFilter entry: present
DEBUG c.p.i.tenant.CompanyTenantFilter -   - Principal type: User
DEBUG c.p.i.tenant.CompanyTenantFilter -   - Is authenticated: true
DEBUG c.p.i.tenant.CompanyTenantFilter -   - Authorities: [ROLE_USER]
DEBUG c.p.i.tenant.CompanyTenantFilter - Authorization header present: true
DEBUG c.p.i.tenant.CompanyTenantFilter - Extracted tenant_id from JWT: 87b6a00e-896a-4f69-b9cd-3349d50c1578
DEBUG c.p.i.tenant.CompanyTenantFilter - Operating in JWT-only mode
DEBUG c.p.i.tenant.CompanyTenantFilter - JWT-only mode: Using tenant_id from JWT: 87b6a00e-896a-4f69-b9cd-3349d50c1578
DEBUG c.p.i.tenant.CompanyTenantFilter - Verifying company exists: 87b6a00e-896a-4f69-b9cd-3349d50c1578
DEBUG c.p.i.tenant.CompanyTenantFilter - Company verified: 87b6a00e-896a-4f69-b9cd-3349d50c1578
DEBUG c.p.i.tenant.CompanyTenantFilter - SecurityContext Authentication: present
DEBUG c.p.i.tenant.CompanyTenantFilter -   - Is authenticated: true
DEBUG c.p.i.tenant.CompanyTenantFilter -   - Principal type: User
DEBUG c.p.i.tenant.CompanyTenantFilter - Authenticated user: user (ID: 1)
DEBUG c.p.i.tenant.CompanyTenantFilter - Verifying user membership in company: 87b6a00e-896a-4f69-b9cd-3349d50c1578
DEBUG c.p.i.tenant.CompanyTenantFilter - User has 1 active company memberships
DEBUG c.p.i.tenant.CompanyTenantFilter - User membership verified for company: 87b6a00e-896a-4f69-b9cd-3349d50c1578
DEBUG c.p.i.tenant.CompanyTenantFilter - Setting company tenant context to schema: company_87b6a00e_896a_4f69_b9cd_3349d50c1578
DEBUG c.p.i.tenant.CompanyTenantFilter - === CompanyTenantFilter END === Proceeding to next filter
DEBUG c.p.i.tenant.CompanyTenantFilter - Clearing company tenant context
```

This shows:
- Authentication state at filter entry
- Tenant ID extraction and validation
- Company verification
- User membership validation
- Tenant context setup

## Common Error Scenarios

### Scenario 1: Authentication Object is Null

If you see:
```
WARN  c.p.i.tenant.CompanyTenantFilter - No authenticated user found for company tenant request
WARN  c.p.i.tenant.CompanyTenantFilter -   Request URI: /api/products
WARN  c.p.i.tenant.CompanyTenantFilter -   Authorization header present: true
WARN  c.p.i.tenant.CompanyTenantFilter -   SecurityContext has authentication: false
```

**Problem:** JWT token was present but SecurityContext was not populated.

**Investigation Steps:**
1. Check AuthTokenFilter logs - did JWT validation succeed?
2. Check if user exists in database
3. Verify filter execution order in SecurityConfig logs
4. Check for any errors in AuthTokenFilter

### Scenario 2: JWT Token Missing or Invalid

If you see:
```
DEBUG c.p.i.config.AuthTokenFilter - No JWT token found in Authorization header for: GET /api/products
```
or
```
WARN  c.p.i.config.AuthTokenFilter - JWT token validation failed for request: GET /api/products
```

**Problem:** No valid JWT token in request.

**Solution:** Ensure Authorization header is set with format: `Bearer <token>`

### Scenario 3: Tenant ID Missing from JWT

If you see:
```
WARN  c.p.i.tenant.CompanyTenantFilter - JWT-only mode: tenant_id claim is required but not found in JWT for request: GET /api/products
```

**Problem:** JWT token doesn't contain tenant_id claim.

**Solution:** Ensure JWT tokens are generated with tenant_id claim (company UUID).

### Scenario 4: User Not Member of Company

If you see:
```
WARN  c.p.i.tenant.CompanyTenantFilter - User username does not have membership in company 87b6a00e-896a-4f69-b9cd-3349d50c1578
```

**Problem:** User is authenticated but not a member of the requested company.

**Solution:** 
1. Verify user has CompanyStoreUser record for this company
2. Check that membership is active (is_active = true)
3. Ensure correct company UUID in JWT token

## Filter Execution Order

The filters execute in this order:

1. **RateLimitingFilter** - Rate limiting checks
2. **AuthTokenFilter** - JWT authentication and SecurityContext population
3. **CompanyTenantFilter** - Tenant context setup and authorization
4. **IdempotencyKeyFilter** - Idempotency checks

**Critical:** AuthTokenFilter MUST run before CompanyTenantFilter, otherwise CompanyTenantFilter won't find an authenticated user in SecurityContext.

## Testing Authentication Flow

You can test the authentication flow with curl:

```bash
# 1. Login to get JWT token
curl -X POST http://localhost:8080/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'

# Response includes access_token with tenant_id

# 2. Test API endpoint with JWT
curl -X GET http://localhost:8080/api/products \
  -H "Authorization: Bearer <your-jwt-token>"
```

## Verifying the Fix

To verify authentication is working:

1. Enable DEBUG logging as shown above
2. Make an authenticated request to a protected endpoint
3. Check logs for:
   - ✅ "Authentication successful for user: X on METHOD /path"
   - ✅ "Authentication at CompanyTenantFilter entry: present"
   - ✅ "User membership verified for company: X"
4. Request should succeed (200 OK) or fail with appropriate error (not 401)

## Additional Notes

- All sensitive information (passwords, tokens) is NOT logged
- Only token lengths and user identifiers are logged
- Logs use SLF4J logger (via Logback in Spring Boot)
- Production deployments should use INFO level; use DEBUG only for troubleshooting
