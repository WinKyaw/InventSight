# New Features Documentation - InventSight

This document describes the newly implemented features in the InventSight system as of October 2025.

## Table of Contents
1. [CEO Role and Enhanced RBAC](#1-ceo-role-and-enhanced-rbac)
2. [Product Price Management APIs](#2-product-price-management-apis)
3. [Many-to-Many Role Assignments](#3-many-to-many-role-assignments)
4. [Offline Sync Runtime: Idempotency and Change Feed](#4-offline-sync-runtime-idempotency-and-change-feed)

---

## 1. CEO Role and Enhanced RBAC

### Overview
InventSight now includes a CEO (Chief Executive Officer) role with owner-level privileges, providing more granular access control for organizations.

### Role Hierarchy
1. **FOUNDER** - Original owner with full privileges
2. **CEO** - Owner-level privileges (NEW)
3. **GENERAL_MANAGER** - Can manage stores, users, and warehouses
4. **STORE_MANAGER** - Store-level management
5. **EMPLOYEE** - Basic access

### Key Changes
- CEO role has same privileges as FOUNDER (owner-level)
- All manager-only endpoints updated to include CEO
- `isOwnerLevel()` method now returns true for both FOUNDER and CEO
- `isManagerLevel()` returns true for FOUNDER, CEO, GENERAL_MANAGER, STORE_MANAGER

### RBAC Updates
All controllers have been updated with CEO role:
- `SalesOrderController` - Order approval and management
- `WarehouseInventoryController` - Inventory operations
- `ProductPricingController` - Price management
- `SalesInventoryController` - Sales operations
- `SyncController` - Sync operations

### Code Example
```java
// CEO role check
if (role.isOwnerLevel()) {
    // FOUNDER or CEO
}

if (role.isManagerLevel()) {
    // FOUNDER, CEO, GENERAL_MANAGER, or STORE_MANAGER
}
```

---

## 2. Product Price Management APIs

### Overview
New REST endpoints for managing product prices with audit logging and sync support. Access restricted to FOUNDER, CEO, and GENERAL_MANAGER roles only.

### Endpoints

#### Update Original Price
```bash
PUT /api/products/{productId}/price/original
Authorization: Bearer {token}
X-Tenant-ID: {companyId}

{
  "amount": 100.00,
  "reason": "Cost increase from supplier"
}
```

#### Update Owner-Set Sell Price
```bash
PUT /api/products/{productId}/price/owner-sell
Authorization: Bearer {token}
X-Tenant-ID: {companyId}

{
  "amount": 150.00,
  "reason": "Promotional pricing"
}
```

#### Update Retail Price
```bash
PUT /api/products/{productId}/price/retail
Authorization: Bearer {token}
X-Tenant-ID: {companyId}

{
  "amount": 175.00,
  "reason": "Market adjustment"
}
```

### Features
- **Audit Logging**: All price changes logged with old/new values, actor, and reason
- **Activity Tracking**: Changes recorded in activity log for user history
- **Sync Support**: Price changes pushed to sync feed for offline clients
- **RBAC**: Only FOUNDER, CEO, and GENERAL_MANAGER can update prices

### DTO Structure
```java
public class SetPriceRequest {
    @NotNull
    @DecimalMin("0.0")
    private BigDecimal amount;
    
    private String reason;  // Optional
}
```

---

## 3. Many-to-Many Role Assignments

### Overview
Users can now hold multiple roles within a single company or store membership via a new mapping table.

### Database Schema
```sql
CREATE TABLE company_store_user_roles (
    id UUID PRIMARY KEY,
    company_store_user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    assigned_at TIMESTAMP,
    assigned_by VARCHAR(100),
    revoked_at TIMESTAMP,
    revoked_by VARCHAR(100),
    UNIQUE (company_store_user_id, role)
);
```

### Migration V7
- Automatically creates `company_store_user_roles` table
- Backfills existing roles from `company_store_user.role` column
- Maintains backward compatibility

### Repository Methods
```java
// Get all active roles for a user membership
List<CompanyRole> findRolesByCompanyStoreUser(CompanyStoreUser csu);

// Get highest privilege role
List<CompanyRole> findRolesByCompanyStoreUserOrderedByPrivilege(CompanyStoreUser csu);

// Check if specific role exists
boolean existsByCompanyStoreUserAndRoleAndIsActiveTrue(CompanyStoreUser csu, CompanyRole role);
```

### Backward Compatibility
- Existing `company_store_user.role` column retained
- Code can be gradually updated to use mapping table
- Both approaches work during transition period

### Future Enhancements
- API endpoints to assign/revoke multiple roles
- Role history and audit trail
- Role-based permission customization

---

## 4. Offline Sync Runtime: Idempotency and Change Feed

### Idempotency Support

**Purpose:** Ensures duplicate requests with the same `Idempotency-Key` return identical responses without duplicate processing.

**Components:**
- `IdempotencyKeyFilter` - HTTP filter that intercepts write operations (POST/PUT/PATCH/DELETE)
- `IdempotencyService` - Service for managing idempotency keys
- `IdempotencyKey` entity - Stores idempotency keys with request hashes and cached responses

**Usage:**
```bash
# Include Idempotency-Key header in write requests
curl -X POST http://localhost:8080/api/warehouse-inventory/add \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-Tenant-ID: company-uuid" \
  -H "Idempotency-Key: unique-key-123" \
  -H "Content-Type: application/json" \
  -d '{"warehouseId":"uuid","productId":"uuid","quantity":10}'
```

**Behavior:**
- First request with a key: Processes normally and caches response
- Replay with same key and same request: Returns cached response (200 or original status)
- Replay with same key but different request: Returns 409 Conflict

**Configuration:**
```yaml
inventsight:
  sync:
    idempotency:
      enabled: true           # Enable/disable idempotency checking
      ttl-hours: 24          # Time-to-live for idempotency keys
```

**TTL Cleanup:**
Expired keys are automatically cleaned up every hour via scheduled task.

### Change Feed API

**Purpose:** Provides a chronologically ordered feed of changes for offline sync support.

**Endpoint:** `GET /sync/changes`

**Query Parameters:**
- `since` (optional) - ISO-8601 timestamp watermark for incremental sync
- `limit` (optional, max 500, default 100) - Maximum number of changes to return

**Example Request:**
```bash
curl -X GET "http://localhost:8080/api/sync/changes?since=2025-10-11T10:00:00&limit=100" \
  -H "Authorization: Bearer YOUR_TOKEN" \
  -H "X-Tenant-ID: company-uuid"
```

**Example Response:**
```json
{
  "changes": [
    {
      "id": "uuid",
      "entity_type": "Product",
      "entity_id": "product-uuid",
      "operation": "UPDATE",
      "changed_at": "2025-10-11T10:15:30",
      "change_data": "{\"name\":\"Updated Product\"}",
      "version": 1
    }
  ],
  "count": 1,
  "has_more": false,
  "next_watermark": "2025-10-11T10:15:30"
}

**Configuration:**
```yaml
inventsight:
  sync:
    change-feed:
      enabled: true          # Enable/disable change feed
      page-size: 100         # Default page size
```

**Recording Changes:**
Use `SyncChangeService.recordChange()` in your service layer:
```java
syncChangeService.recordChange("Product", productId, "UPDATE", productData);
```

---

## 2. Multi-Currency Support with FX Service

### FxService

**Purpose:** Provides currency conversion capabilities with configurable exchange rate providers.

**Components:**
- `FxService` - Currency conversion service
- `Money` value object - Represents monetary amounts with currency codes

**Usage:**
```java
// Convert between currencies
Money usd100 = Money.of(new BigDecimal("100.00"), "USD");
Money eur = fxService.convert(usd100, "EUR");

// Add money in different currencies (automatic conversion)
Money total = fxService.add(usd100, eur50);

// Subtract money in different currencies
Money remaining = fxService.subtract(usd100, eur20);
```

**Supported Operations:**
- `convert(Money, targetCurrency)` - Convert between currencies
- `add(Money, Money)` - Add two amounts (auto-converts if needed)
- `subtract(Money, Money)` - Subtract two amounts (auto-converts if needed)

**Currency Rounding:**
The service automatically handles currency-specific rounding:
- USD, EUR: 2 decimal places
- JPY: 0 decimal places (whole numbers)
- KWD, BHD: 3 decimal places

**Configuration:**
```yaml
inventsight:
  currency:
    default-code: USD
    fx:
      provider: mock        # Options: mock, ecb, openexchangerates
      api-key: ""          # API key for external provider
      cache-ttl-hours: 24  # Cache duration for rates
```

**Note:** Current implementation uses mock rates. In production, integrate with a real FX provider.

---

## 3. Rate Limiting and Abuse Protection

### RateLimitingFilter

**Purpose:** Protects APIs from abuse by enforcing per-IP and per-tenant rate limits.

**Implementation:**
- Uses Bucket4j token bucket algorithm
- Per-IP limits for general protection
- Per-tenant limits for authenticated requests
- Stricter limits on authentication endpoints

**Configuration:**
```yaml
inventsight:
  rate-limiting:
    enabled: true
    per-tenant:
      requests-per-minute: 1000
    per-ip:
      requests-per-minute: 100
    auth-endpoints:
      requests-per-minute: 10
```

**Response on Limit Exceeded:**
- Status: 429 Too Many Requests
- Header: `Retry-After: 60` (seconds)
- Body:
```json
{
  "error": "Rate limit exceeded",
  "limit": 100,
  "limit_type": "IP",
  "retry_after": 60
}
```

---

## 4. OAuth2 Login Integration

### OAuth2 Login Support

**Purpose:** Allows users to sign in with external OAuth2 providers (Google, Microsoft, Okta).

**Components:**
- `CustomOAuth2UserService` - Maps OAuth2 users to local accounts
- `CustomOAuth2User` - Wrapper for OAuth2 user with local user entity

**Supported Providers:**
- Google
- Microsoft (Azure AD)
- Okta

**Configuration:**
```yaml
inventsight:
  security:
    oauth2:
      login:
        enabled: true

spring:
  security:
    oauth2:
      client:
        registration:
          google:
            client-id: ${GOOGLE_CLIENT_ID}
            client-secret: ${GOOGLE_CLIENT_SECRET}
            scope: openid,profile,email
          microsoft:
            client-id: ${MICROSOFT_CLIENT_ID}
            client-secret: ${MICROSOFT_CLIENT_SECRET}
            scope: openid,profile,email
```

**Security Notes:**
- OAuth2 login maps to existing local users by email
- Does NOT automatically grant tenant access
- User must have existing membership in a company
- Failed to find local account results in authentication error

---

## 5. Tenant Filter Consolidation

### Changes

**TenantFilter:**
- Made no-op for protected routes
- Only handles public endpoints

**CompanyTenantFilter:**
- Sole authoritative source for tenant context on protected endpoints
- Enforces JWT `tenant_id` claim validation
- Validates X-Tenant-ID header against JWT claim when configured

**Configuration:**
```yaml
inventsight:
  tenancy:
    header:
      validate-against-jwt: true  # Strict validation of header vs JWT
```

**Security:**
- Protected endpoints MUST go through `CompanyTenantFilter`
- Tenant context only set after membership verification
- Prevents tenant context manipulation attacks

---

## 6. Warehouse EMPLOYEE Permissions and Price Redaction

### EMPLOYEE Role Enhancements

**New Permissions:**
- Can add inventory (`POST /api/warehouse-inventory/add`)
- Can withdraw inventory (`POST /api/warehouse-inventory/withdraw`)
- Can edit own same-day additions (`PUT /api/warehouse-inventory/additions/{id}`)
- Can edit own same-day withdrawals (`PUT /api/warehouse-inventory/withdrawals/{id}`)

**Restrictions:**
- Can only edit their own records
- Can only edit on the same day they created the record
- Cannot view or set cost/price fields

### Price Redaction

**Implementation:**
- `EmployeePriceRedactionAdvice` - Response body advice that redacts cost fields
- Automatic redaction for EMPLOYEE role
- Applies to all warehouse inventory responses

**Redacted Fields:**
- `unitCost` / `unit_cost`
- `totalCost` / `total_cost`
- `cost`
- `price`
- `unitPrice` / `unit_price`
- `totalPrice` / `total_price`

**Example:**
Manager response:
```json
{
  "productName": "Product A",
  "quantity": 100,
  "unitCost": 10.50,
  "totalCost": 1050.00
}
```

EMPLOYEE response (same data):
```json
{
  "productName": "Product A",
  "quantity": 100,
  "unitCost": null,
  "totalCost": null
}
```

**Request Validation:**
- EMPLOYEE attempts to set cost fields are silently ignored
- Audit log records the attempt
- No error returned (graceful degradation)

---

## Filter Ordering in SecurityConfig

Filters are registered in the following order for correct operation:

1. **RateLimitingFilter** (Ordered.HIGHEST_PRECEDENCE) - Apply rate limits before any processing
2. **CompanyTenantFilter** (+5) - Set tenant context from JWT claim and membership
3. **AuthTokenFilter** - JWT authentication
4. **IdempotencyKeyFilter** (+20) - Check idempotency after auth/tenant context is available

This ordering ensures:
- Rate limiting happens first
- Tenant context is available for idempotency cache keys
- Idempotency checks include tenant ID for proper scoping

---

## Testing

### Unit Tests

New test files:
- `IdempotencyServiceTest` - Tests for idempotency key management
- `FxServiceTest` - Tests for currency conversion

Run tests:
```bash
mvn test -Dtest=IdempotencyServiceTest,FxServiceTest
```

### Integration Testing

Recommended test scenarios:
1. **Idempotency:** Replay requests with same/different bodies
2. **Change Feed:** Verify pagination and watermark continuity
3. **Rate Limiting:** Exceed limits and verify 429 responses
4. **OAuth2 Login:** Test with real providers in staging
5. **Price Redaction:** Verify EMPLOYEE cannot see cost fields
6. **EMPLOYEE Edits:** Verify ownership and same-day checks

---

## Migration Notes

### Existing Deployments

1. **Database migrations:** Entities `IdempotencyKey` and `SyncChange` will be auto-created by Hibernate
2. **Feature flags:** All features are enabled by default in `application.yml` but can be disabled
3. **Backward compatibility:** Changes are backward compatible with existing functionality
4. **OAuth2:** Optional - only enabled when configured

### Recommended Rollout

1. Enable idempotency and monitor logs for conflicts
2. Enable rate limiting with high thresholds initially
3. Enable change feed for new tenants first
4. Configure OAuth2 providers when ready
5. Update EMPLOYEE permissions after informing users

---

## Troubleshooting

### Idempotency Issues

**Problem:** Getting 409 Conflict unexpectedly
**Solution:** Check that request body is identical; even whitespace matters

**Problem:** Keys not expiring
**Solution:** Verify `@EnableScheduling` is active and check logs for cleanup task

### Rate Limiting Issues

**Problem:** Legitimate users getting 429
**Solution:** Increase limits in `application.yml` or disable temporarily

**Problem:** Rate limiting not working
**Solution:** Verify `enabled: true` and check filter is registered in SecurityConfig

### OAuth2 Login Issues

**Problem:** User can login but has no access
**Solution:** User must have existing membership; OAuth2 doesn't grant access

**Problem:** Email not found
**Solution:** Ensure OAuth2 provider returns email in user info

### Price Redaction Issues

**Problem:** EMPLOYEE still seeing prices
**Solution:** Verify `EmployeePriceRedactionAdvice` is registered and user has EMPLOYEE role only

**Problem:** Managers not seeing prices
**Solution:** Check CompanyRole hierarchy; ensure user has manager-level role

---

## Future Enhancements

Planned improvements:
- Real FX provider integration (ECB, Open Exchange Rates)
- Redis-backed rate limiting for distributed deployments
- Real-time change notifications via WebSocket
- SAML2 support (pending dependency availability)
- Conflict resolution for offline sync
- Enhanced audit logging for price redactions
