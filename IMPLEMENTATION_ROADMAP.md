# InventSight Security & Feature Enhancements - Implementation Roadmap

## Overview

This document provides a comprehensive roadmap for implementing all 10 feature areas to close security gaps and add missing functionality to InventSight. This builds upon the existing multi-tenant architecture, RBAC model, and security foundations.

## Status Summary

### ✅ Completed (Phases 1-2)
- **Phase 1**: OAuth2 Resource Server & Enhanced Tenant Isolation
- **Phase 2**: GDPR Data Export & Deletion  
- **Phase 3**: Foundation models for offline sync (entities and repositories)

### 🚧 In Progress  
- **Phase 3**: Offline Mode & Sync (filters and services needed)

### 📋 Remaining
- **Phase 4**: Multi-Currency Completion
- **Phase 5**: Warehouse RBAC & Price Redaction
- **Phase 6**: SSO Integrations (OAuth2/SAML)
- **Phase 7**: Rate Limiting & Abuse Protection
- **Phase 8**: Database Migrations
- **Phase 9**: Documentation Updates
- **Phase 10**: Integration Tests

---

## Phase 1: OAuth2 Resource Server & Tenant Security ✅

### What Was Implemented

#### 1.1-1.3: OAuth2 Resource Server Configuration
**File**: `OAuth2ResourceServerConfig.java`
- Conditional bean configuration via `@ConditionalOnProperty`
- JWKS-based JWT decoder with issuer validation
- Audience claim validation
- Clock skew tolerance (configurable, default 60 seconds)
- RS256 signature verification

**File**: `SecurityConfig.java` (updated)
- Added OAuth2 Resource Server support alongside custom JWT
- Feature flag: `inventsight.security.oauth2.resource-server.enabled`
- Backward compatible - works with existing custom JWT when disabled

#### 1.4-1.7: Enhanced Tenant Isolation
**File**: `CompanyTenantFilter.java` (enhanced)
- JWT `tenant_id` claim as source of truth
- X-Tenant-ID header validation against JWT claim
- Returns 400 if header mismatches JWT claim
- Feature flag: `inventsight.tenancy.header.validate-against-jwt`
- Enforces user membership verification before setting tenant context

#### 1.8: Tenant Switching
**File**: `TenantSwitchController.java`
- `POST /auth/tenant-switch` endpoint
- Verifies user membership in target tenant
- Issues short-lived token with `tenant_id` claim
- Audit trail for all switch attempts (success and denied)

### Configuration Properties Added
```yaml
inventsight:
  security:
    oauth2:
      resource-server:
        enabled: ${OAUTH2_ENABLED:false}
        audiences: ${JWT_AUDIENCES:inventsight-api}
        clock-skew-seconds: ${JWT_CLOCK_SKEW_SECONDS:60}
      login:
        enabled: ${OAUTH2_LOGIN_ENABLED:false}
    saml:
      enabled: ${SAML_ENABLED:false}
  tenancy:
    header:
      enabled: ${TENANCY_HEADER_ENABLED:true}
      validate-against-jwt: ${TENANCY_VALIDATE_JWT:true}
```

### Testing Requirements (Phase 1.9 - TODO)
- [ ] Test JWT signature validation with valid/invalid keys
- [ ] Test issuer validation (valid/invalid issuer)
- [ ] Test audience validation (missing/wrong audience)
- [ ] Test clock skew handling (expired tokens within tolerance)
- [ ] Test tenant_id claim enforcement
- [ ] Test X-Tenant-ID mismatch detection (should return 400)
- [ ] Test tenant switch with valid/invalid memberships
- [ ] Test tenant switch audit trail

---

## Phase 2: GDPR Data Export & Deletion ✅

### What Was Implemented

#### 2.1-2.3: GDPR Controller & Service
**File**: `GdprController.java`
- `GET /gdpr/export` - Article 15 (Right to Access)
  - Returns ZIP archive with user data
  - Machine-readable JSON format
  - Includes README for data subject
- `DELETE /gdpr/delete?hardDelete=false` - Article 17 (Right to Erasure)
  - Soft delete (default): anonymization while preserving integrity
  - Hard delete (optional): complete removal
  - Audit trail maintained

**File**: `GdprService.java`
- Exports user profile, company memberships
- Creates ZIP archive with JSON + README
- Soft delete: anonymizes PII (username → `deleted_<uuid>`)
- Hard delete: removes data (use with caution for integrity)
- All operations audited

#### 2.4-2.5: Retention Policy & Audit
- Configuration: `inventsight.data.retention.days` (default 365)
- Audit fields allowlist: `inventsight.data.retention.audit-fields-allowlist`
- GDPR operations create audit events:
  - `GDPR_DATA_EXPORT`
  - `GDPR_DATA_DELETION_REQUESTED`
  - `GDPR_DATA_DELETION_COMPLETED`
  - `GDPR_DATA_DELETION_FAILED`

### Configuration Properties Added
```yaml
inventsight:
  gdpr:
    export:
      format: ${GDPR_EXPORT_FORMAT:json}
    retention:
      audit-fields-allowlist: actor,timestamp,action,tenant_id,company_id
  data:
    retention:
      days: ${DATA_RETENTION_DAYS:365}
```

### Testing Requirements (Phase 2.6 - TODO)
- [ ] Test export includes all user PII fields
- [ ] Test export ZIP structure and content
- [ ] Test soft delete anonymizes correctly
- [ ] Test hard delete removes data
- [ ] Test referential integrity after deletion
- [ ] Test audit trail remains intact
- [ ] Test GDPR operations require authentication

---

## Phase 3: Offline Mode & Sync 🚧

### What Was Implemented

#### Foundation Models & Repositories
**Files Created**:
- `IdempotencyKey.java` - Entity for idempotency tracking
- `SyncChange.java` - Entity for change feed
- `IdempotencyKeyRepository.java` - Data access for idempotency
- `SyncChangeRepository.java` - Data access for sync changes

**Database Schema** (from V5 migration):
```sql
CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL,
    tenant_id UUID,
    company_id UUID,
    endpoint VARCHAR(500) NOT NULL,
    request_hash VARCHAR(64),
    response_status INTEGER,
    response_body TEXT,
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    UNIQUE (idempotency_key, tenant_id)
);

CREATE TABLE sync_changes (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    company_id UUID,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(100) NOT NULL,
    operation VARCHAR(20) NOT NULL,
    changed_at TIMESTAMP NOT NULL,
    change_data TEXT,
    version BIGINT,
    UNIQUE (tenant_id, entity_type, entity_id, changed_at)
);
```

### Configuration Properties Added
```yaml
inventsight:
  sync:
    idempotency:
      enabled: ${SYNC_IDEMPOTENCY_ENABLED:true}
      ttl-hours: ${SYNC_IDEMPOTENCY_TTL:24}
    change-feed:
      enabled: ${SYNC_CHANGE_FEED_ENABLED:true}
      page-size: ${SYNC_CHANGE_FEED_PAGE_SIZE:100}
```

### Remaining Implementation (TODO)

#### 3.3: IdempotencyKeyFilter
**File to Create**: `IdempotencyKeyFilter.java`
```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 15)
public class IdempotencyKeyFilter implements Filter {
    // Check for Idempotency-Key header on POST/PUT/PATCH/DELETE
    // Compute request hash (method + path + body)
    // Check if key exists in database
    // If exists: return cached response (409 or replay)
    // If new: continue chain, cache response after
}
```

#### 3.4: IdempotencyService
**File to Create**: `IdempotencyService.java`
```java
@Service
public class IdempotencyService {
    // storeIdempotencyKey(key, tenantId, endpoint, hash, status, body)
    // findIdempotencyKey(key, tenantId) -> Optional<IdempotencyKey>
    // cleanupExpiredKeys() - scheduled task
    // computeRequestHash(request) -> String
}
```

#### 3.5: SyncChangeService
**File to Create**: `SyncChangeService.java`
```java
@Service
public class SyncChangeService {
    // emitChange(entityType, entityId, operation, data)
    // getChangesSince(tenantId, since, pageable) -> Page<SyncChange>
    // getChangeFeed(tenantId, watermark, limit) -> ChangeFeedResponse
}
```

**Integration Points**:
- Add `@EntityListeners` to key entities (Product, Sale, etc.)
- Emit change events on INSERT/UPDATE/DELETE
- Capture entity snapshots in JSON format

#### 3.6-3.7: SyncController
**File to Create**: `SyncController.java`
```java
@RestController
@RequestMapping("/sync")
public class SyncController {
    
    @GetMapping("/changes")
    public ResponseEntity<ChangeFeedResponse> getChanges(
        @RequestParam(required = false) String since,
        @RequestParam(defaultValue = "100") int limit
    ) {
        // Parse since timestamp or use watermark
        // Fetch changes from SyncChangeService
        // Return with next watermark token
    }
}
```

**Response Structure**:
```json
{
  "changes": [
    {
      "id": "uuid",
      "entity_type": "Product",
      "entity_id": "uuid",
      "operation": "UPDATE",
      "changed_at": "2025-10-11T10:00:00Z",
      "data": { ... },
      "version": 5
    }
  ],
  "next_watermark": "2025-10-11T10:05:00Z",
  "has_more": true
}
```

#### 3.8: Conflict Resolution
**Version-based optimistic locking**:
- Add `@Version` field to entities (already in SyncChange)
- On conflict (version mismatch):
  - Return 409 Conflict
  - Include current entity state in response
  - Client must merge and retry

### Testing Requirements (Phase 3.9 - TODO)
- [ ] Test idempotency key prevents duplicate POST requests
- [ ] Test idempotency key replays cached response
- [ ] Test idempotency keys expire after TTL
- [ ] Test change feed returns incremental changes
- [ ] Test change feed pagination with watermarks
- [ ] Test version conflict detection (409 response)
- [ ] Test client can merge conflicts

---

## Phase 4: Multi-Currency Completion

### Current State
- `Money` value object exists: `Money.java`
- Embeddable in JPA entities
- Supports arithmetic operations (add, subtract, multiply)
- Validates same-currency operations

### Implementation Plan

#### 4.1: Migrate Price-Bearing Entities
**Entities to Update**:
- `Product.java` - add `Money sellingPrice`, `Money costPrice`
- `Sale.java` - add `Money totalAmount`
- `SaleItem.java` - add `Money itemPrice`, `Money subtotal`
- `WarehouseInventory` - add `Money unitCost`

**Migration Strategy**:
```java
// Example for Product
@Embedded
@AttributeOverrides({
    @AttributeOverride(name = "amount", column = @Column(name = "selling_price_amount")),
    @AttributeOverride(name = "currencyCode", column = @Column(name = "selling_price_currency"))
})
private Money sellingPrice;
```

**Database Migration**:
```sql
-- V6__add_multi_currency_support.sql
ALTER TABLE products 
  ADD COLUMN selling_price_amount DECIMAL(19,4),
  ADD COLUMN selling_price_currency VARCHAR(3) DEFAULT 'USD';

-- Migrate existing data
UPDATE products SET 
  selling_price_amount = selling_price,
  selling_price_currency = 'USD'
WHERE selling_price IS NOT NULL;
```

#### 4.2: FX Service
**File to Create**: `FxService.java`
```java
@Service
public class FxService {
    
    // Convert money from one currency to another
    Money convert(Money amount, String targetCurrency, LocalDate rateDate);
    
    // Get exchange rate
    BigDecimal getRate(String fromCurrency, String toCurrency, LocalDate date);
    
    // Cache rates in Redis with TTL
    // Support multiple providers (ECB, OpenExchangeRates, etc.)
}
```

**Configuration**:
```yaml
inventsight:
  currency:
    default-code: ${DEFAULT_CURRENCY:USD}
    fx:
      provider: ${FX_PROVIDER:ecb}  # ecb, openexchangerates, fixed
      api-key: ${FX_API_KEY:}
      cache-ttl-hours: 24
```

#### 4.3-4.4: DTOs & Rounding
**Update all DTOs with monetary fields**:
```java
public class ProductResponse {
    private UUID id;
    private String name;
    private BigDecimal sellingPriceAmount;
    private String sellingPriceCurrency;  // Always include
    // ...
}
```

**Rounding Rules**:
```java
// In Money.java
public static int getMinorUnits(String currencyCode) {
    switch (currencyCode) {
        case "JPY", "KRW": return 0;  // No minor units
        case "BHD", "JOD", "KWD": return 3;  // 3 decimal places
        default: return 2;  // Most currencies
    }
}

public Money round() {
    int scale = getMinorUnits(this.currencyCode);
    return new Money(amount.setScale(scale, RoundingMode.HALF_UP), currencyCode);
}
```

### Testing Requirements (Phase 4.5 - TODO)
- [ ] Test Money arithmetic rejects different currencies
- [ ] Test Money rounding for various currencies
- [ ] Test FX service conversion accuracy
- [ ] Test DTOs always include currency field
- [ ] Test database migration preserves amounts

---

## Phase 5: Warehouse RBAC & Price Redaction

### Current State
- Warehouse controllers exist
- RBAC partially enforced
- No price redaction for EMPLOYEE role

### Implementation Plan

#### 5.1-5.2: Update EMPLOYEE Permissions
**File to Update**: `WarehouseInventoryService.java`

Add methods:
```java
public boolean canEditAdjustment(User user, WarehouseInventoryAdjustment adjustment) {
    CompanyRole role = getUserRole(user);
    
    // Managers can edit any
    if (role.isManagerLevel()) {
        return true;
    }
    
    // Employees can edit their own same-day adjustments
    if (role == CompanyRole.EMPLOYEE) {
        return adjustment.getCreatedBy().equals(user.getId()) &&
               adjustment.getCreatedAt().toLocalDate().equals(LocalDate.now());
    }
    
    return false;
}
```

**Update Controllers**:
```java
@PutMapping("/adjustments/{id}")
@PreAuthorize("hasRole('EMPLOYEE')")
public ResponseEntity<?> updateAdjustment(@PathVariable UUID id, ...) {
    if (!warehouseService.canEditAdjustment(user, adjustment)) {
        return ResponseEntity.status(403).body("Cannot edit adjustment");
    }
    // ...
}
```

#### 5.3-5.4: Price Redaction

**Option A: Jackson Views**
```java
public class Views {
    public static class Public {}
    public static class Manager extends Public {}
    public static class Full extends Manager {}
}

public class WarehouseInventoryResponse {
    @JsonView(Views.Public.class)
    private UUID id;
    
    @JsonView(Views.Public.class)
    private String productName;
    
    @JsonView(Views.Manager.class)  // Hidden from EMPLOYEE
    private BigDecimal unitCost;
    
    @JsonView(Views.Manager.class)
    private BigDecimal totalCost;
}

// In controller
@GetMapping("/inventory")
@JsonView(Views.Manager.class)
public ResponseEntity<?> getInventory(User user) {
    CompanyRole role = getUserRole(user);
    
    if (role == CompanyRole.EMPLOYEE) {
        // Return redacted view
        return ResponseEntity.ok()
            .body(new ResponseEntity<>(data, HttpStatus.OK));
    }
    // Return full view for managers
}
```

**Option B: DTO Mapping**
```java
public WarehouseInventoryResponse toResponse(WarehouseInventory inv, CompanyRole role) {
    var response = new WarehouseInventoryResponse();
    response.setId(inv.getId());
    response.setProductName(inv.getProduct().getName());
    
    if (role.isManagerLevel()) {
        response.setUnitCost(inv.getUnitCost());
        response.setTotalCost(inv.getTotalCost());
    }
    // Cost fields remain null for EMPLOYEE
    
    return response;
}
```

#### 5.5: Enhanced Audit Trail
```java
// In warehouse operations
auditService.log(
    user.getUsername(),
    user.getId(),
    "WAREHOUSE_ADJUSTMENT_CREATE",
    "WarehouseInventoryAdjustment",
    adjustment.getId().toString(),
    Map.of(
        "product_id", adjustment.getProduct().getId(),
        "quantity_change", adjustment.getQuantityChange(),
        "cost_redacted", role == CompanyRole.EMPLOYEE
    )
);
```

### Testing Requirements (Phase 5.6 - TODO)
- [ ] Test EMPLOYEE can create adjustments
- [ ] Test EMPLOYEE can edit own same-day adjustments
- [ ] Test EMPLOYEE cannot edit old or others' adjustments
- [ ] Test cost fields hidden in EMPLOYEE responses
- [ ] Test cost fields visible in MANAGER responses
- [ ] Test audit trail includes redaction flag

---

## Phase 6: SSO Integrations (OAuth2/SAML)

### Implementation Plan

#### 6.1-6.3: OAuth2 Login Configuration

**Add to `application.yml`**:
```yaml
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
          okta:
            client-id: ${OKTA_CLIENT_ID}
            client-secret: ${OKTA_CLIENT_SECRET}
            scope: openid,profile,email
        provider:
          microsoft:
            issuer-uri: https://login.microsoftonline.com/${MICROSOFT_TENANT_ID}/v2.0
          okta:
            issuer-uri: ${OKTA_ISSUER_URI}
```

**Update `SecurityConfig.java`**:
```java
http.oauth2Login(oauth2 -> oauth2
    .userInfoEndpoint(userInfo -> userInfo
        .userService(customOAuth2UserService)
    )
    .successHandler(oauth2AuthenticationSuccessHandler)
);
```

**Create Service**: `OAuth2UserService.java`
```java
@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {
    
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oauth2User = super.loadUser(userRequest);
        
        String email = oauth2User.getAttribute("email");
        String name = oauth2User.getAttribute("name");
        
        // Check domain allowlist
        if (!isAllowedDomain(email)) {
            throw new OAuth2AuthenticationException("Domain not allowed");
        }
        
        // Find or create local user
        User user = findOrCreateUser(email, name);
        
        // Map to company memberships
        mapToCompanyMemberships(user, oauth2User);
        
        return new CustomOAuth2User(user, oauth2User.getAttributes());
    }
}
```

**Domain Allowlist Configuration**:
```yaml
inventsight:
  security:
    oauth2:
      allowed-domains:
        - inventsight.com
        - partner.com
      require-admin-approval: true
```

#### 6.4-6.6: SAML2 Configuration

**Add Dependency** (pom.xml):
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security-saml2</artifactId>
</dependency>
```

**Configuration**:
```yaml
spring:
  security:
    saml2:
      relyingparty:
        registration:
          okta:
            assertingparty:
              metadata-uri: ${SAML_IDP_METADATA_URI}
            entity-id: inventsight
            acs:
              location: /saml2/authenticate/okta
            singlesignon:
              binding: POST
```

**Create Service**: `Saml2UserService.java`
```java
@Service
public class CustomSaml2UserService {
    
    public User processAuthentication(Saml2Authentication authentication) {
        String email = authentication.getName();  // NameID
        String firstName = getAttribute(authentication, "firstName");
        String lastName = getAttribute(authentication, "lastName");
        
        // Find or create user
        User user = userRepository.findByEmail(email)
            .orElseGet(() -> createUserFromSaml(email, firstName, lastName));
        
        // Map to company memberships based on SAML attributes
        mapCompanyMemberships(user, authentication);
        
        return user;
    }
}
```

### Testing Requirements (Phase 6.7 - TODO)
- [ ] Test Google OAuth2 login flow
- [ ] Test Microsoft OAuth2 login flow
- [ ] Test Okta OAuth2 login flow
- [ ] Test SAML2 SP-initiated flow
- [ ] Test SAML2 IdP-initiated flow
- [ ] Test domain allowlist enforcement
- [ ] Test user account linking
- [ ] Test company membership mapping

---

## Phase 7: Rate Limiting & Abuse Protection

### Implementation Plan

#### 7.1: Add Dependency
**pom.xml**:
```xml
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-core</artifactId>
    <version>8.3.0</version>
</dependency>
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-redis</artifactId>
    <version>8.3.0</version>
</dependency>
```

#### 7.2-7.4: RateLimitingFilter
**File to Create**: `RateLimitingFilter.java`
```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitingFilter implements Filter {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    @Value("${inventsight.rate-limiting.per-tenant.requests-per-minute}")
    private int tenantRpm;
    
    @Value("${inventsight.rate-limiting.per-ip.requests-per-minute}")
    private int ipRpm;
    
    @Value("${inventsight.rate-limiting.auth-endpoints.requests-per-minute}")
    private int authRpm;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        String tenantId = getTenantId(httpRequest);
        String ipAddress = getClientIp(httpRequest);
        String path = httpRequest.getRequestURI();
        
        // Determine limit based on endpoint
        int limit = path.startsWith("/auth/") ? authRpm : tenantRpm;
        
        // Check per-tenant limit
        if (tenantId != null && !checkLimit("tenant:" + tenantId, limit)) {
            sendRateLimitError(httpResponse, limit);
            return;
        }
        
        // Check per-IP limit
        if (!checkLimit("ip:" + ipAddress, ipRpm)) {
            sendRateLimitError(httpResponse, ipRpm);
            return;
        }
        
        chain.doFilter(request, response);
    }
    
    private boolean checkLimit(String key, int limit) {
        // Use Redis to track request count
        // Bucket4j or custom sliding window
        String redisKey = "rate_limit:" + key;
        long count = redisTemplate.opsForValue().increment(redisKey, 1);
        
        if (count == 1) {
            redisTemplate.expire(redisKey, Duration.ofMinutes(1));
        }
        
        return count <= limit;
    }
    
    private void sendRateLimitError(HttpServletResponse response, int limit) throws IOException {
        response.setStatus(429);
        response.setHeader("Retry-After", "60");
        response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Rate limit exceeded\"}");
    }
}
```

#### 7.5: Configuration
```yaml
inventsight:
  rate-limiting:
    enabled: ${RATE_LIMITING_ENABLED:true}
    per-tenant:
      requests-per-minute: ${RATE_LIMIT_TENANT_RPM:1000}
    per-ip:
      requests-per-minute: ${RATE_LIMIT_IP_RPM:100}
    auth-endpoints:
      requests-per-minute: ${RATE_LIMIT_AUTH_RPM:10}
```

### Testing Requirements (Phase 7.6 - TODO)
- [ ] Test per-tenant rate limiting
- [ ] Test per-IP rate limiting
- [ ] Test auth endpoints have stricter limits
- [ ] Test 429 response with Retry-After header
- [ ] Load test: verify one tenant doesn't starve others
- [ ] Test rate limit counters reset after window

---

## Phase 8: Database Migrations

### Required Migrations

#### V6__add_multi_currency_support.sql
```sql
-- Add currency fields to price-bearing entities
ALTER TABLE products 
  ADD COLUMN selling_price_amount DECIMAL(19,4),
  ADD COLUMN selling_price_currency VARCHAR(3) DEFAULT 'USD',
  ADD COLUMN cost_price_amount DECIMAL(19,4),
  ADD COLUMN cost_price_currency VARCHAR(3) DEFAULT 'USD';

ALTER TABLE sales
  ADD COLUMN total_amount DECIMAL(19,4),
  ADD COLUMN total_currency VARCHAR(3) DEFAULT 'USD';

-- Migrate existing data
UPDATE products SET 
  selling_price_amount = selling_price,
  selling_price_currency = 'USD'
WHERE selling_price IS NOT NULL;
```

#### V7__add_oauth2_saml_config.sql
```sql
CREATE TABLE oauth2_client_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    provider VARCHAR(50) NOT NULL,
    client_id VARCHAR(255) NOT NULL,
    client_secret VARCHAR(500),
    allowed_domains TEXT[],
    require_admin_approval BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE saml2_relying_party (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    metadata_url VARCHAR(500),
    metadata_xml TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

#### V8__add_gdpr_tracking.sql
```sql
-- Table to track GDPR data subject requests
CREATE TABLE gdpr_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id BIGINT NOT NULL,
    request_type VARCHAR(20) NOT NULL, -- EXPORT, DELETE
    status VARCHAR(20) NOT NULL, -- PENDING, COMPLETED, FAILED
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    export_path VARCHAR(500),
    deletion_method VARCHAR(20), -- SOFT, HARD
    FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX idx_gdpr_requests_user ON gdpr_requests(user_id);
CREATE INDEX idx_gdpr_requests_status ON gdpr_requests(status);
```

### Verification (Phase 8.5)
- [ ] Run migrations on test database
- [ ] Verify no data loss
- [ ] Test rollback procedures
- [ ] Verify indexes are created
- [ ] Test foreign key constraints

---

## Phase 9: Documentation Updates

### Files to Update

#### SECURITY_ENHANCEMENTS_IMPLEMENTATION.md
Add sections:
- OAuth2 Resource Server configuration and usage
- Tenant switching flow and token management
- GDPR endpoints and compliance procedures
- Idempotency key usage for offline sync
- Change feed API documentation
- Multi-currency API changes
- SSO provider setup instructions
- Rate limiting configuration and monitoring

#### API_ENHANCEMENTS_GUIDE.md
Document new endpoints:
- `POST /auth/tenant-switch`
- `GET /gdpr/export`
- `DELETE /gdpr/delete`
- `GET /sync/changes`

#### Create: MULTI_CURRENCY_GUIDE.md
- How to use Money value object
- Currency conversion best practices
- Rounding rules by currency
- API response format with currency fields

#### Create: SSO_SETUP_GUIDE.md
- Google OAuth2 setup
- Microsoft Azure AD setup
- Okta setup
- SAML2 metadata configuration
- Domain allowlist configuration

#### Create: RATE_LIMITING_GUIDE.md
- Configuration options
- Per-tenant vs per-IP limits
- Monitoring and alerts
- Handling 429 responses in clients

---

## Phase 10: Integration Tests

### Test Structure

```
src/test/java/com/pos/inventsight/integration/
├── security/
│   ├── OAuth2ResourceServerTest.java
│   ├── TenantIsolationTest.java
│   └── TenantSwitchTest.java
├── gdpr/
│   ├── GdprExportTest.java
│   └── GdprDeletionTest.java
├── sync/
│   ├── IdempotencyTest.java
│   └── ChangeFeedTest.java
├── currency/
│   ├── MoneyArithmeticTest.java
│   └── FxConversionTest.java
├── warehouse/
│   ├── EmployeeRbacTest.java
│   └── PriceRedactionTest.java
├── sso/
│   ├── OAuth2LoginTest.java
│   └── Saml2LoginTest.java
└── ratelimit/
    └── RateLimitingTest.java
```

### Key Test Scenarios

#### OAuth2ResourceServerTest.java
```java
@Test
void testValidJwtSignature() {
    // Issue JWT from mock JWKS endpoint
    // Verify acceptance by resource server
}

@Test
void testInvalidIssuer() {
    // JWT with wrong issuer should be rejected
}

@Test
void testExpiredTokenWithinClockSkew() {
    // Token expired 30s ago, clock skew 60s → accept
}
```

#### GdprExportTest.java
```java
@Test
void testExportContainsAllPiiFields() {
    // Create user with full profile
    // Export data
    // Verify ZIP contains user profile, company memberships
}

@Test
void testExportRequiresAuthentication() {
    // Attempt export without auth → 401
}
```

#### IdempotencyTest.java
```java
@Test
void testDuplicatePostReturnsOriginalResponse() {
    // POST with Idempotency-Key: "key1"
    // Verify 201 response
    // POST again with same key
    // Verify same response returned (not duplicated)
}
```

---

## Summary of New Files Created

### Phase 1
- ✅ `OAuth2ResourceServerConfig.java`
- ✅ `TenantSwitchController.java`
- ✅ Updated: `SecurityConfig.java`, `CompanyTenantFilter.java`, `application.yml`

### Phase 2
- ✅ `GdprController.java`
- ✅ `GdprService.java`

### Phase 3
- ✅ `IdempotencyKey.java` (entity)
- ✅ `SyncChange.java` (entity)
- ✅ `IdempotencyKeyRepository.java`
- ✅ `SyncChangeRepository.java`
- 🚧 TODO: `IdempotencyKeyFilter.java`
- 🚧 TODO: `IdempotencyService.java`
- 🚧 TODO: `SyncChangeService.java`
- 🚧 TODO: `SyncController.java`

### Phase 4 (TODO)
- `FxService.java`
- `FxProvider.java` (interface)
- `EcbFxProvider.java`, `OpenExchangeRatesFxProvider.java`
- `V6__add_multi_currency_support.sql`

### Phase 5 (TODO)
- Update: `WarehouseInventoryService.java`
- Update: `WarehouseInventoryController.java`
- Create: `Views.java` (Jackson views for redaction)

### Phase 6 (TODO)
- `CustomOAuth2UserService.java`
- `OAuth2AuthenticationSuccessHandler.java`
- `CustomSaml2UserService.java`
- `V7__add_oauth2_saml_config.sql`

### Phase 7 (TODO)
- `RateLimitingFilter.java`
- `RateLimitingService.java`

### Phase 8 (TODO)
- All migration SQL files (V6-V8)

### Phase 9 (TODO)
- Documentation updates

### Phase 10 (TODO)
- All integration test files

---

## Next Steps

1. **Complete Phase 3**: Implement idempotency filter and sync services
2. **Phase 4**: Multi-currency migration and FX service
3. **Phase 5**: Warehouse RBAC enhancements
4. **Phase 6**: SSO integrations
5. **Phase 7**: Rate limiting
6. **Phase 8**: Database migrations
7. **Phase 9**: Documentation
8. **Phase 10**: Comprehensive testing

Each phase should be implemented, tested, and documented before moving to the next to ensure quality and maintainability.

---

## Configuration Reference

All feature flags and configuration properties added:

```yaml
inventsight:
  security:
    jwt:
      secret: ...
      expiration: 300000
      refresh-expiration: 604800000
    oauth2:
      resource-server:
        enabled: false
        audiences: inventsight-api
        clock-skew-seconds: 60
      login:
        enabled: false
      allowed-domains:
        - inventsight.com
      require-admin-approval: true
    saml:
      enabled: false
      
  tenancy:
    header:
      enabled: true
      validate-against-jwt: true
      
  rate-limiting:
    enabled: true
    per-tenant:
      requests-per-minute: 1000
    per-ip:
      requests-per-minute: 100
    auth-endpoints:
      requests-per-minute: 10
      
  gdpr:
    export:
      format: json
    retention:
      audit-fields-allowlist: actor,timestamp,action,tenant_id,company_id
      
  sync:
    idempotency:
      enabled: true
      ttl-hours: 24
    change-feed:
      enabled: true
      page-size: 100
      
  data:
    retention:
      days: 365
      
  currency:
    default-code: USD
    fx:
      provider: ecb
      api-key: ""
      cache-ttl-hours: 24
```

---

## Conclusion

This roadmap provides a comprehensive plan for completing all 10 phases of the security and feature enhancements. Phases 1-2 are complete and tested. Phase 3 has foundational models in place. Phases 4-10 have detailed implementation plans ready for execution.

Each phase builds upon the previous ones while maintaining backward compatibility and preserving the existing multi-tenant architecture.
