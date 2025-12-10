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

InventSight supports multiple languages through two complementary systems:

### 1. Database-Based Frontend Translations (i18n API)

The primary i18n system for frontend UI translations uses a database-backed translation service.

#### Supported Languages

- **English** (`en`) - Default
- **Spanish** (`es`)
- **Chinese** (`zh`)
- **Japanese** (`ja`)
- **Myanmar/Burmese** (`my`)

#### API Endpoints

**Get Available Languages:**
```http
GET /api/i18n/languages
```

**Response:**
```json
{
  "languages": ["en", "es", "zh", "ja", "my"],
  "default": "en"
}
```

**Get All Translations for a Language:**
```http
GET /api/i18n/translations/{languageCode}
```

**Example:**
```http
GET /api/i18n/translations/my
```

**Response:**
```json
{
  "auth.login": "အကောင့်ဝင်ရန်",
  "auth.signup": "အကောင့်ဖွင့်ရန်",
  "tabs.dashboard": "ဒက်ရှ်ဘုတ်",
  "inventory.title": "ကုန်သိုလှောင်စီမံခန့်ခွဲမှု",
  ...
}
```

**Get Translations by Category:**
```http
GET /api/i18n/translations/{languageCode}/category/{category}
```

**Example:**
```http
GET /api/i18n/translations/my/category/auth
```

#### Translation Categories

- `auth` - Authentication screens
- `tabs` - Navigation tabs
- `dashboard` - Dashboard widgets
- `inventory` - Inventory management
- `employees` - Employee management
- `profile` - User profile settings
- `common` - Common UI elements
- `errors` - Error messages

#### Adding New Translations

To add a new language, add translations to the database seed migration file `V18__seed_base_translations.sql`:

```sql
INSERT INTO translations (key, language_code, value, category) VALUES
('auth.login', 'fr', 'Connexion', 'auth'),
('auth.signup', 'fr', 'S''inscrire', 'auth'),
...
ON CONFLICT (key, language_code) DO NOTHING;
```

### 2. Server-Side Message Localization (Accept-Language)

Server-side API responses support localization via the `Accept-Language` header.

#### Supported Locales

- **English** (`en`) - Default
- **Burmese** (`my-MM`)

#### Usage

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

#### Adding New Locales

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

---

## Sales Order API for Employees

InventSight now supports a comprehensive sales order API that allows store employees to view inventory, create orders, and manage sales with proper RBAC and approval workflows.

### Features

- **Employee Inventory View**: Employees can view current inventory and sale prices (cost fields are redacted)
- **Cross-Store Sourcing**: Check product availability across multiple warehouses
- **Order Creation**: Build orders with automatic stock reservation
- **Approval Workflows**: Manager approval required for high discounts or cross-store sourcing
- **Cancellation Management**: Employee-initiated cancellations with manager approval for confirmed orders
- **Idempotency**: All write endpoints support idempotent operations
- **Offline Sync**: Change feed integration for synchronization

### Sales Inventory Endpoints

#### 1. Get Warehouse Inventory

View available inventory for a specific warehouse.

**Request:**
```http
GET /api/sales/inventory/warehouse/{warehouseId}
Authorization: Bearer <jwt_token>
X-Tenant-ID: <company_uuid>
```

**Response:**
```json
[
  {
    "productId": "uuid",
    "productName": "Product Name",
    "productSku": "SKU-001",
    "warehouseId": "uuid",
    "warehouseName": "Main Warehouse",
    "available": 50,
    "reorderPoint": 10,
    "price": 99.99,
    "currencyCode": "USD"
  }
]
```

**Access:** FOUNDER, GENERAL_MANAGER, STORE_MANAGER, EMPLOYEE

#### 2. Get Product Availability

Check availability of a product across all warehouses.

**Request:**
```http
GET /api/sales/inventory/availability?productId={uuid}
Authorization: Bearer <jwt_token>
X-Tenant-ID: <company_uuid>
```

**Response:**
```json
[
  {
    "productId": "uuid",
    "productName": "Product Name",
    "productSku": "SKU-001",
    "warehouseId": "uuid",
    "warehouseName": "Main Warehouse",
    "available": 50,
    "reorderPoint": 10,
    "price": 99.99,
    "currencyCode": "USD"
  },
  {
    "productId": "uuid",
    "productName": "Product Name",
    "productSku": "SKU-001",
    "warehouseId": "uuid",
    "warehouseName": "Branch Warehouse",
    "available": 30,
    "reorderPoint": 5,
    "price": 99.99,
    "currencyCode": "USD"
  }
]
```

**Access:** FOUNDER, GENERAL_MANAGER, STORE_MANAGER, EMPLOYEE

### Sales Order Endpoints

#### 3. Create Sales Order

Create a new draft sales order.

**Request:**
```http
POST /api/sales/orders
Authorization: Bearer <jwt_token>
X-Tenant-ID: <company_uuid>
Idempotency-Key: <unique_key>
Content-Type: application/json

{
  "currencyCode": "USD",
  "customerName": "John Doe",
  "customerPhone": "123-456-7890",
  "customerEmail": "john@example.com"
}
```

**Response:**
```json
{
  "id": "uuid",
  "tenantId": "uuid",
  "status": "DRAFT",
  "requiresManagerApproval": false,
  "currencyCode": "USD",
  "customerName": "John Doe",
  "customerPhone": "123-456-7890",
  "customerEmail": "john@example.com",
  "createdAt": "2025-10-28T10:00:00",
  "updatedAt": "2025-10-28T10:00:00",
  "createdBy": "employee@example.com",
  "updatedBy": "employee@example.com",
  "items": [],
  "totalAmount": 0.00
}
```

**Access:** FOUNDER, GENERAL_MANAGER, STORE_MANAGER, EMPLOYEE

#### 4. Add Item to Order

Add a product to an order and reserve stock.

**Request:**
```http
POST /api/sales/orders/{orderId}/items
Authorization: Bearer <jwt_token>
X-Tenant-ID: <company_uuid>
Idempotency-Key: <unique_key>
Content-Type: application/json

{
  "warehouseId": "uuid",
  "productId": "uuid",
  "quantity": 2,
  "unitPrice": 99.99,
  "discountPercent": 5.0,
  "currencyCode": "USD"
}
```

**Response:**
```json
{
  "id": "uuid",
  "warehouseId": "uuid",
  "warehouseName": "Main Warehouse",
  "productId": "uuid",
  "productName": "Product Name",
  "productSku": "SKU-001",
  "quantity": 2,
  "unitPrice": 99.99,
  "discountPercent": 5.0,
  "currencyCode": "USD",
  "lineTotal": 189.98,
  "createdAt": "2025-10-28T10:00:00"
}
```

**Notes:**
- Stock is reserved immediately using pessimistic locking
- If employee discount exceeds threshold (default 10%), requiresManagerApproval is set to true
- If items are from multiple warehouses, requiresManagerApproval is set to true (configurable)

**Access:** FOUNDER, GENERAL_MANAGER, STORE_MANAGER, EMPLOYEE

#### 5. Submit Order

Submit order for processing.

**Request:**
```http
POST /api/sales/orders/{orderId}/submit
Authorization: Bearer <jwt_token>
X-Tenant-ID: <company_uuid>
```

**Response:**
```json
{
  "id": "uuid",
  "status": "CONFIRMED",
  ...
}
```

**Status Transitions:**
- If requiresManagerApproval=false: DRAFT → CONFIRMED
- If requiresManagerApproval=true: DRAFT → PENDING_MANAGER_APPROVAL

**Access:** FOUNDER, GENERAL_MANAGER, STORE_MANAGER, EMPLOYEE

#### 6. Request Order Cancellation

Request to cancel an order.

**Request:**
```http
POST /api/sales/orders/{orderId}/cancel-request
Authorization: Bearer <jwt_token>
X-Tenant-ID: <company_uuid>
```

**Response:**
```json
{
  "id": "uuid",
  "status": "CANCELLED",
  ...
}
```

**Status Transitions:**
- DRAFT/SUBMITTED: Cancels immediately, releases reservations → CANCELLED
- CONFIRMED/PENDING_MANAGER_APPROVAL: Requires approval → CANCEL_REQUESTED

**Access:** FOUNDER, GENERAL_MANAGER, STORE_MANAGER, EMPLOYEE

#### 7. Approve Order (Manager)

Manager approves order that requires approval.

**Request:**
```http
POST /api/sales/orders/{orderId}/approve
Authorization: Bearer <jwt_token>
X-Tenant-ID: <company_uuid>
```

**Response:**
```json
{
  "id": "uuid",
  "status": "CONFIRMED",
  ...
}
```

**Status Transition:** PENDING_MANAGER_APPROVAL → CONFIRMED

**Access:** FOUNDER, GENERAL_MANAGER, STORE_MANAGER

#### 8. Approve Cancellation (Manager)

Manager approves cancellation request.

**Request:**
```http
POST /api/sales/orders/{orderId}/cancel-approve
Authorization: Bearer <jwt_token>
X-Tenant-ID: <company_uuid>
```

**Response:**
```json
{
  "id": "uuid",
  "status": "CANCELLED",
  ...
}
```

**Status Transition:** CANCEL_REQUESTED → CANCELLED (releases all reservations)

**Access:** FOUNDER, GENERAL_MANAGER, STORE_MANAGER

#### 9. Get Order Details

Retrieve order information with items.

**Request:**
```http
GET /api/sales/orders/{orderId}
Authorization: Bearer <jwt_token>
X-Tenant-ID: <company_uuid>
```

**Response:**
```json
{
  "id": "uuid",
  "tenantId": "uuid",
  "status": "CONFIRMED",
  "requiresManagerApproval": false,
  "currencyCode": "USD",
  "customerName": "John Doe",
  "customerPhone": "123-456-7890",
  "items": [
    {
      "id": "uuid",
      "productName": "Product Name",
      "quantity": 2,
      "unitPrice": 99.99,
      "discountPercent": 5.0,
      "lineTotal": 189.98
    }
  ],
  "totalAmount": 189.98
}
```

**Access:** FOUNDER, GENERAL_MANAGER, STORE_MANAGER, EMPLOYEE

### Configuration

Add to `application.yml`:

```yaml
inventsight:
  sales:
    enabled: true
    max-employee-discount-percent: 10
    cross-store:
      employee-requires-approval: true
```

**Configuration Options:**
- `sales.enabled`: Enable/disable sales functionality (default: true)
- `sales.max-employee-discount-percent`: Maximum discount employees can apply without approval (default: 10)
- `sales.cross-store.employee-requires-approval`: Require manager approval for cross-store sourcing (default: true)

### Security Notes

1. **Price Redaction**: Employees never see cost prices; only sale prices are exposed
2. **RBAC**: All endpoints enforce role-based access control
3. **Tenancy**: All operations are tenant-scoped via CompanyTenantFilter
4. **Idempotency**: All write operations support Idempotency-Key header
5. **Pessimistic Locking**: Stock reservations use database locks to prevent overselling
6. **Change Feed**: All mutations emit SyncChange events for offline sync

### Order Status Flow

```
DRAFT → SUBMITTED → PENDING_MANAGER_APPROVAL → CONFIRMED → FULFILLED
     ↓                     ↓                      ↓
CANCEL_REQUESTED ← ← ← ← ← ←
     ↓
CANCELLED
```

### Testing

Comprehensive tests cover:
- Stock reservation with pessimistic locking
- Discount threshold triggering approval
- Cross-store sourcing triggering approval
- Cancellation request transitions
- Reservation release on cancellation
- RBAC enforcement
- Manager approval workflows

---

## CEO Role and Price Management

### Overview

InventSight now supports a **CEO** role with owner-level privileges and dedicated price management APIs restricted to CEO, Founder, and General Manager roles. The system also supports **many-to-many role assignment**, allowing users to hold multiple roles per company membership.

### CEO Role

The **CEO** role is a new company role with the following privileges:

- **Owner-level access**: Same privileges as FOUNDER
- **Manager-level operations**: Full manager privileges including approvals
- **Store management**: Can create and manage stores
- **User management**: Can add/remove users and manage roles
- **Warehouse management**: Can create and manage warehouses
- **Price management**: Can set all product price tiers

#### Role Hierarchy

```
FOUNDER     → Owner-level (highest)
CEO         → Owner-level
GENERAL_MANAGER → Manager-level
STORE_MANAGER   → Manager-level (store-specific)
EMPLOYEE        → Basic access
```

### Many-to-Many Role Assignment

Users can now have **multiple roles** for the same company membership. This is implemented via the `company_store_user_roles` table.

#### Database Schema

**New Table: `company_store_user_roles`**
```sql
CREATE TABLE company_store_user_roles (
    id UUID PRIMARY KEY,
    company_store_user_id UUID NOT NULL,  -- FK to company_store_user
    role VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT TRUE,
    assigned_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_by VARCHAR(100),
    revoked_at TIMESTAMP,
    revoked_by VARCHAR(100),
    UNIQUE (company_store_user_id, role)
);
```

#### Migration and Backward Compatibility

- **Flyway Migration V7** automatically backfills existing roles
- Legacy `role` field in `company_store_user` is **deprecated** but maintained for backward compatibility
- Services read from `company_store_user_roles` with fallback to legacy field

### Role Management Endpoints

#### Add Role to Membership

**Request:**
```http
POST /api/companies/{companyId}/memberships/{membershipId}/roles
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "role": "CEO"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Role added successfully",
  "data": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "membershipId": "660e8400-e29b-41d4-a716-446655440000",
    "role": "CEO",
    "roleDisplayName": "Chief Executive Officer",
    "isActive": true,
    "assignedAt": "2025-10-30T07:00:00",
    "assignedBy": "admin"
  }
}
```

**Access:** FOUNDER, CEO, GENERAL_MANAGER

#### Remove Role from Membership

**Request:**
```http
DELETE /api/companies/{companyId}/memberships/{membershipId}/roles/{role}
Authorization: Bearer <jwt_token>
```

**Response:**
```json
{
  "success": true,
  "message": "Role removed successfully"
}
```

**Access:** FOUNDER, CEO, GENERAL_MANAGER

#### Get Membership Roles

**Request:**
```http
GET /api/companies/{companyId}/memberships/{membershipId}/roles
Authorization: Bearer <jwt_token>
```

**Response:**
```json
{
  "success": true,
  "message": "Roles retrieved successfully",
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "membershipId": "660e8400-e29b-41d4-a716-446655440000",
      "role": "CEO",
      "roleDisplayName": "Chief Executive Officer",
      "isActive": true,
      "assignedAt": "2025-10-30T07:00:00",
      "assignedBy": "admin"
    }
  ]
}
```

### Price Management Endpoints

Three dedicated endpoints allow CEO, Founder, and General Manager roles to update product prices with full audit logging and sync support.

#### Set Original Price

**Request:**
```http
PUT /api/products/{productId}/price/original
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "amount": 150.00,
  "reason": "Supplier price increase"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Original price updated successfully",
  "data": {
    "productId": "770e8400-e29b-41d4-a716-446655440000",
    "priceType": "original",
    "oldPrice": 120.00,
    "newPrice": 150.00
  }
}
```

**Access:** CEO, FOUNDER, GENERAL_MANAGER

#### Set Owner-Sell Price

**Request:**
```http
PUT /api/products/{productId}/price/owner-sell
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "amount": 180.00,
  "reason": "Adjusted markup for Q4"
}
```

**Response:**
```json
{
  "success": true,
  "message": "Owner-sell price updated successfully",
  "data": {
    "productId": "770e8400-e29b-41d4-a716-446655440000",
    "priceType": "ownerSetSell",
    "oldPrice": 160.00,
    "newPrice": 180.00
  }
}
```

**Access:** CEO, FOUNDER, GENERAL_MANAGER

#### Set Retail Price

**Request:**
```http
PUT /api/products/{productId}/price/retail
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "amount": 199.99
}
```

**Response:**
```json
{
  "success": true,
  "message": "Retail price updated successfully",
  "data": {
    "productId": "770e8400-e29b-41d4-a716-446655440000",
    "priceType": "retail",
    "oldPrice": 189.99,
    "newPrice": 199.99
  }
}
```

**Access:** CEO, FOUNDER, GENERAL_MANAGER

### Audit Logging

All price changes are automatically logged to the audit trail with:
- Actor (username and user ID)
- Product details (ID and name)
- Price type (original, ownerSetSell, retail)
- Old and new values
- Optional reason
- Store information
- Timestamp

**Audit Event Example:**
```json
{
  "actor": "admin",
  "actorId": 12345,
  "action": "PRICE_CHANGE_RETAIL",
  "entityType": "Product",
  "entityId": "770e8400-e29b-41d4-a716-446655440000",
  "detailsJson": {
    "productId": "770e8400-e29b-41d4-a716-446655440000",
    "productName": "Widget Pro",
    "priceType": "retail",
    "oldPrice": 189.99,
    "newPrice": 199.99,
    "storeId": "880e8400-e29b-41d4-a716-446655440000",
    "storeName": "Main Store"
  }
}
```

### Sync Support

Price changes emit **SyncChange** events for offline synchronization:
- Entity Type: "Product"
- Operation: "UPDATE"
- Change Data: Full product snapshot

### RBAC Updates

All manager-level endpoints now include **CEO** in their `@PreAuthorize` annotations:

**Before:**
```java
@PreAuthorize("hasAnyAuthority('FOUNDER', 'GENERAL_MANAGER', 'STORE_MANAGER')")
```

**After:**
```java
@PreAuthorize("hasAnyAuthority('FOUNDER', 'CEO', 'GENERAL_MANAGER', 'STORE_MANAGER')")
```

**Updated Controllers:**
- WarehouseInventoryController
- SalesOrderController
- SalesInventoryController
- SyncController

### Security Notes

1. **Price Management**: Restricted to owner-level roles (CEO, FOUNDER, GENERAL_MANAGER)
2. **Role Assignment**: Only manager-level roles can assign/revoke roles
3. **Audit Trail**: All price changes are immutably logged
4. **Store Access**: Users can only update prices for products in stores they have access to
5. **Sync Events**: All price changes trigger offline sync events

### Migration Checklist

✅ Database migration V7 applied (creates `company_store_user_roles` table)
✅ Existing roles backfilled automatically
✅ Legacy `role` field maintained for backward compatibility
✅ Services updated to read from role mapping table
✅ CEO role added to all manager-level RBAC checks

### Testing

Run tests to verify the new features:
```bash
./mvnw test -Dtest=CompanyRoleTest
```

**Test Coverage:**
- CEO role permissions (owner-level, manager-level)
- Role hierarchy validation
- Many-to-many role assignment
- Price management endpoint authorization
- Audit logging for price changes
- Sync event generation

