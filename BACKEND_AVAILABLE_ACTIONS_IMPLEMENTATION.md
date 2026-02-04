# Backend Available Actions Implementation

## Overview

This implementation adds `availableActions` field to all transfer API responses, centralizing permission logic on the backend and eliminating the need for frontend permission duplication.

## Architecture

### TransferPermissionService
Central service that calculates what actions a user can perform on a transfer request based on:
- **User Role**: OWNER, FOUNDER, ADMIN, MANAGER have GM+ privileges
- **Transfer Status**: PENDING, APPROVED, READY, IN_TRANSIT, DELIVERED, etc.
- **Location Access**: GM+ roles have access to all locations (future: user-location assignments)
- **Ownership**: Users can cancel their own pending transfers

### Permission Matrix

| Status | GM+ Actions | Requester Actions | Regular User Actions |
|--------|-------------|-------------------|---------------------|
| PENDING | approve, reject, cancel | cancel | - |
| APPROVED | markReady, cancel | - | - |
| READY | startDelivery, cancel | - | - |
| IN_TRANSIT | markDelivered, cancel | - | - |
| DELIVERED | receive, cancel | - | receive (if at TO location) |
| COMPLETED | - | - | - |
| CANCELLED | - | - | - |
| REJECTED | - | - | - |

### Controller Integration

All action endpoints now:
1. Validate permissions using `TransferPermissionService.canPerformAction()`
2. Return 403 Forbidden if permission denied
3. Return updated `availableActions` in response after state changes

## API Response Examples

### GET /transfers/{id}
```json
{
  "success": true,
  "request": {
    "id": "cf27e0cc-...",
    "status": "PENDING",
    ...
  },
  "availableActions": ["approve", "reject", "cancel"]
}
```

### GET /transfers (list)
```json
{
  "success": true,
  "requests": [
    {
      "transfer": {
        "id": "cf27e0cc-...",
        "status": "PENDING",
        ...
      },
      "availableActions": ["approve", "reject", "cancel"]
    }
  ],
  "pagination": {...}
}
```

### PUT /transfers/{id}/approve
```json
{
  "success": true,
  "message": "Transfer request approved",
  "request": {
    "id": "cf27e0cc-...",
    "status": "APPROVED",
    ...
  },
  "availableActions": ["markReady", "cancel"]
}
```

## Security

- All permission checks performed server-side
- Frontend cannot bypass permission checks
- CodeQL security scan: **0 vulnerabilities**
- Defense in depth: UI hiding + API validation

## Testing

### Unit Tests
- **TransferPermissionServiceTest**: 16 tests covering all permission scenarios
- **TransferRequestAvailableActionsControllerTest**: 4 tests verifying controller integration

### Integration Tests
- All existing transfer tests pass (no regressions)
- New tests verify permission validation and response structure

## Migration Guide

### Frontend Changes Required
For list endpoint, access transfer data via:
```javascript
// Before
response.data.requests[i].status

// After
response.data.requests[i].transfer.status
response.data.requests[i].availableActions
```

### Backward Compatibility
- GET /transfers/{id} - ✅ Backward compatible (adds new field)
- GET /transfers - ⚠️ Structure changed (wraps each transfer)
- All action endpoints - ✅ Backward compatible (adds new field)

## Future Enhancements

1. **User-Location Assignments**
   - Track which users have access to which stores/warehouses
   - Implement fine-grained location-based permissions
   - Replace simple GM+ check with actual assignment records

2. **Permission Caching**
   - Cache permission calculations for performance
   - Invalidate on role/assignment changes

3. **Audit Logging**
   - Log all permission checks
   - Track who performed what actions

4. **Dynamic Roles**
   - Move role definitions to database
   - Allow custom permission configurations per company

## Files Modified

### New Files
- `src/main/java/com/pos/inventsight/service/TransferPermissionService.java`
- `src/test/java/com/pos/inventsight/service/TransferPermissionServiceTest.java`
- `src/test/java/com/pos/inventsight/controller/TransferRequestAvailableActionsControllerTest.java`

### Modified Files
- `src/main/java/com/pos/inventsight/controller/TransferRequestController.java`

## Validation Results

✅ All transfer tests passing (20+ tests)
✅ No compilation errors
✅ No security vulnerabilities (CodeQL)
✅ Code review feedback addressed
✅ Permission logic centralized and tested
