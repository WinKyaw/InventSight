# Subscription-Based Company Creation System

This document describes the subscription-based quota system implemented for company creation in InventSight.

## Overview

The system enforces limits on the number of companies a user can create based on their subscription level. This prevents abuse and enables a freemium business model.

## Subscription Levels

| Level | Max Companies | Description |
|-------|--------------|-------------|
| FREE | 1 | Default level for all new users |
| PRO | 3 | Professional plan |
| BUSINESS | 10 | Business plan |
| ENTERPRISE | Unlimited | Enterprise plan with no limits |

## API Endpoints

### 1. POST /api/companies
Creates a new company with quota enforcement.

**Request:**
```json
{
  "name": "My Company",
  "description": "Company description",
  "email": "company@example.com"
}
```

**Success Response (201 Created):**
```json
{
  "success": true,
  "message": "Company created successfully",
  "data": {
    "id": "uuid",
    "name": "My Company",
    "description": "Company description",
    "email": "company@example.com"
  }
}
```

**Error Response (403 Forbidden) - Quota Exceeded:**
```json
{
  "timestamp": "2025-10-04T01:00:00",
  "message": "You have reached the maximum number of companies (1) allowed for your Free plan. Please upgrade your subscription to create more companies.",
  "details": "uri=/api/companies",
  "errorCode": "PLAN_LIMIT_EXCEEDED",
  "system": "InventSight System"
}
```

### 2. GET /api/subscription
Returns the current user's subscription information.

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Subscription info retrieved successfully",
  "data": {
    "plan": "FREE",
    "maxCompanies": 1,
    "currentUsage": 0,
    "remaining": 1
  }
}
```

For ENTERPRISE users (unlimited):
```json
{
  "success": true,
  "message": "Subscription info retrieved successfully",
  "data": {
    "plan": "ENTERPRISE",
    "maxCompanies": null,
    "currentUsage": 100,
    "remaining": null
  }
}
```

### 3. PUT /api/users/{userId}/subscription
Updates a user's subscription level (Admin only).

**Request:**
```json
{
  "subscriptionLevel": "PRO"
}
```

**Success Response (200 OK):**
```json
{
  "userId": 1,
  "username": "john.doe",
  "subscriptionLevel": "PRO"
}
```

**Error Response (403 Forbidden) - Non-Admin:**
```json
{
  "success": false,
  "message": "Only administrators can update subscriptions"
}
```

## Implementation Details

### Exception Handling

The system throws a `PlanLimitExceededException` when a user attempts to create a company beyond their quota. This exception is handled globally and returns a 403 Forbidden status code with a descriptive error message.

### Database Schema

The `subscriptionLevel` field has been added to the `users` table:

```sql
ALTER TABLE users ADD COLUMN subscription_level VARCHAR(20) DEFAULT 'FREE';
```

**Note:** Database migration scripts are not included per requirements. The schema change should be applied manually.

### Quota Enforcement Logic

1. When a user attempts to create a company, the system retrieves their subscription level
2. If the subscription level is ENTERPRISE (unlimited), the check is skipped
3. Otherwise, the system counts active companies where the user is the FOUNDER
4. If the count is >= the max allowed for their plan, a `PlanLimitExceededException` is thrown
5. The exception handler returns a 403 status with a detailed error message

### Default Subscription Level

All users are assigned the FREE subscription level by default when created. This ensures backward compatibility with existing users.

## Testing

The implementation includes comprehensive unit tests:

1. **SubscriptionServiceTest** - Tests subscription info retrieval and updates
   - Tests for all subscription levels (FREE, PRO, BUSINESS, ENTERPRISE)
   - Tests for unlimited plans (null values)
   - Tests for invalid subscription levels

2. **CompanyServiceSubscriptionTest** - Tests quota enforcement during company creation
   - Tests successful creation within limits
   - Tests quota exceeded scenarios
   - Tests unlimited enterprise plans
   - Tests default subscription level behavior

## Usage Examples

### Example 1: Free User Creating First Company

```bash
curl -X POST https://api.inventsight.com/api/companies \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Startup",
    "description": "My first company",
    "email": "startup@example.com"
  }'
```

**Result:** Success (201 Created)

### Example 2: Free User Attempting Second Company

```bash
curl -X POST https://api.inventsight.com/api/companies \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Another Company",
    "description": "Second company",
    "email": "another@example.com"
  }'
```

**Result:** Error (403 Forbidden) - "You have reached the maximum number of companies (1) allowed for your Free plan..."

### Example 3: Checking Subscription Status

```bash
curl -X GET https://api.inventsight.com/api/subscription \
  -H "Authorization: Bearer <token>"
```

**Result:**
```json
{
  "success": true,
  "message": "Subscription info retrieved successfully",
  "data": {
    "plan": "FREE",
    "maxCompanies": 1,
    "currentUsage": 1,
    "remaining": 0
  }
}
```

### Example 4: Admin Upgrading User Subscription

```bash
curl -X PUT https://api.inventsight.com/api/users/123/subscription \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "subscriptionLevel": "PRO"
  }'
```

**Result:** Success (200 OK)

## Security Considerations

1. Only users with ADMIN role can update subscription levels
2. Users can only view their own subscription information
3. Quota checks are performed server-side and cannot be bypassed
4. Company creation is protected by authentication

## Future Enhancements

Possible future improvements:
- Subscription expiration dates
- Automatic downgrade when subscription expires
- Payment integration
- Usage analytics dashboard
- Email notifications when approaching limits
