# New Features Documentation - InventSight

This document describes the newly implemented features in the InventSight system as of October 2025.

## 1. Offline Sync Runtime: Idempotency and Change Feed

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

---

## 8. CEO Role and Many-to-Many Role Management

### Overview

InventSight now supports a dedicated CEO role and allows users to have multiple roles per company membership through a many-to-many relationship model.

### CEO Role

**New Role Added:** `CEO` (Chief Executive Officer)

**Privileges:**
- Owner-level access (same as FOUNDER)
- Can manage all company resources (stores, users, warehouses)
- Can manage product pricing (original, owner-sell, retail prices)
- Full manager-level permissions

**Role Hierarchy:**
1. CEO (highest privilege)
2. FOUNDER
3. GENERAL_MANAGER
4. STORE_MANAGER
5. EMPLOYEE (lowest privilege)

### Many-to-Many Role Mapping

**Previous Model:** Each `CompanyStoreUser` had a single `role` field.

**New Model:** Users can have multiple roles via the `company_store_user_roles` mapping table.

**Benefits:**
- Flexibility: Users can hold multiple responsibilities (e.g., CEO + GENERAL_MANAGER)
- Granular permissions: Add/remove specific roles without recreating memberships
- Audit trail: Track when roles are assigned/revoked and by whom

### Implementation Details

**New Entity:** `CompanyStoreUserRole`
```java
- id: UUID
- companyStoreUser: FK to CompanyStoreUser
- role: CompanyRole enum
- isActive: Boolean
- assignedAt: Timestamp
- assignedBy: String
- revokedAt: Timestamp
- revokedBy: String
```

**Database Migration:** `V7__company_store_user_roles.sql`
- Creates `company_store_user_roles` table
- Backfills existing roles from `company_store_user.role`
- Maintains backward compatibility (legacy role column kept)

**Role Resolution:**
- Services first query the new mapping table
- Falls back to legacy role column if no mappings found
- Highest privilege role is used for authorization checks

### API Endpoints

#### Add User with Multiple Roles
```http
POST /api/companies/{companyId}/users/multi-role
Content-Type: application/json

{
  "usernameOrEmail": "user@example.com",
  "roles": ["CEO", "GENERAL_MANAGER"]
}
```

#### Add Role to Existing User
```http
POST /api/companies/{companyId}/users/add-role
Content-Type: application/json

{
  "userId": "user-uuid",
  "role": "CEO"
}
```

#### Remove Role from User
```http
POST /api/companies/{companyId}/users/remove-role
Content-Type: application/json

{
  "userId": "user-uuid",
  "role": "STORE_MANAGER"
}
```

### Updated Authorization

All manager-level endpoints now include CEO role in `@PreAuthorize` annotations:

**Examples:**
- Sales Orders: `@PreAuthorize("hasAnyRole('CEO','FOUNDER','GENERAL_MANAGER','STORE_MANAGER','EMPLOYEE')")`
- Warehouse Management: `@PreAuthorize("hasAnyAuthority('CEO','FOUNDER','GENERAL_MANAGER')")`
- Product Pricing: `@PreAuthorize("hasAnyRole('CEO','FOUNDER','GENERAL_MANAGER')")`

---

## 9. Product Pricing Management APIs

### Overview

Dedicated API endpoints for managing product prices with strict role-based access control and comprehensive audit logging.

### Pricing Tiers

Products support three distinct pricing levels:

1. **Original Price** - Cost/acquisition price from supplier
2. **Owner Set Sell Price** - Wholesale or bulk selling price
3. **Retail Price** - Customer-facing retail price

### API Endpoints

#### Update Original Price
```http
PUT /api/products/{productId}/price/original
Content-Type: application/json

{
  "amount": 100.00,
  "reason": "Supplier price increase"
}
```

**Authorization:** CEO, FOUNDER, or GENERAL_MANAGER only

**Response:**
```json
{
  "success": true,
  "message": "Original price updated successfully",
  "data": {
    "id": "product-uuid",
    "name": "Product Name",
    "originalPrice": 100.00,
    "ownerSetSellPrice": 120.00,
    "retailPrice": 150.00
  }
}
```

#### Update Owner Sell Price
```http
PUT /api/products/{productId}/price/owner-sell
Content-Type: application/json

{
  "amount": 120.00,
  "reason": "Adjust wholesale margin"
}
```

**Authorization:** CEO, FOUNDER, or GENERAL_MANAGER only

#### Update Retail Price
```http
PUT /api/products/{productId}/price/retail
Content-Type: application/json

{
  "amount": 150.00,
  "reason": "Market price adjustment"
}
```

**Authorization:** CEO, FOUNDER, or GENERAL_MANAGER only

### Security Features

**Role-Based Access Control:**
- Only CEO, FOUNDER, and GENERAL_MANAGER can update prices
- STORE_MANAGER and EMPLOYEE roles are denied access
- Authorization enforced via `@PreAuthorize` annotations and service-level checks

**Audit Logging:**
All price changes are logged via `AuditService` with:
- Actor username
- Action type (UPDATE_ORIGINAL_PRICE, UPDATE_OWNER_SELL_PRICE, UPDATE_RETAIL_PRICE)
- Old and new price values
- Optional reason provided by user
- Product details (name, SKU)
- Timestamp
- Tenant/Company context

**Example Audit Log Entry:**
```json
{
  "actor": "john.doe",
  "action": "UPDATE_RETAIL_PRICE",
  "entityType": "Product",
  "entityId": "product-uuid",
  "detailsJson": {
    "priceType": "retail",
    "oldPrice": 140.00,
    "newPrice": 150.00,
    "reason": "Market price adjustment",
    "productName": "Widget A",
    "productSku": "WDG-001"
  },
  "companyId": "company-uuid",
  "eventAt": "2025-10-30T10:30:00Z"
}
```

**Offline Sync Integration:**
- Price updates emit `SyncChange` events
- Offline clients can sync price changes via change feed
- Idempotency support prevents duplicate price updates

### Business Rules

1. **Validation:** Prices must be non-negative (≥ 0)
2. **Reason Field:** Optional but recommended for audit trail
3. **Atomic Updates:** Each price tier updated independently
4. **Company Context:** User must have pricing role in the product's company

### Implementation Notes

**Controller:** `ProductPricingController`
- Three dedicated endpoints (one per price tier)
- Uses `ProductRepository` for persistence
- Integrates with `AuditService` and `SyncChangeService`

**DTO:** `SetPriceRequest`
```java
{
  "amount": BigDecimal (required, min: 0.0),
  "reason": String (optional)
}
```

**Service Integration:**
- `CompanyAuthorizationService.hasProductPricingAccess()` verifies role
- `ProductRepository.save()` persists changes
- `AuditService.log()` creates audit record
- `SyncChangeService.recordChange()` emits sync event

---

## Testing

### New Tests Added

1. **CompanyRoleTest**
   - Validates CEO role permissions
   - Tests role hierarchy (CEO > FOUNDER > GM > SM > EMPLOYEE)
   - Verifies canManagePricing() for pricing roles

2. **Integration Tests** (Recommended)
   - Test pricing endpoints with different roles
   - Verify audit logs are created
   - Test multi-role assignment and resolution
   - Validate backward compatibility with legacy role column

### Test Coverage

Run existing tests to verify changes:
```bash
./mvnw test -Dtest=CompanyRoleTest
./mvnw test
```

All existing tests should pass with CEO role additions.

