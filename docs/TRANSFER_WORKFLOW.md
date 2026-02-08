# Transfer Workflow Guide

Complete guide to the inventory transfer workflow in InventSight v0.2.0.

## Overview

The transfer workflow enables moving inventory between locations (warehouses and stores) with full tracking, approval, and accountability.

## Transfer Lifecycle

```
PENDING → APPROVED → READY → IN_TRANSIT → DELIVERED → COMPLETED
    ↓         ↓                    ↓
REJECTED  CANCELLED            CANCELLED
```

## Step-by-Step Workflow

### 1. Create Transfer Request (PENDING)

**Who:** Store Manager or Warehouse Manager  
**Action:** Create a transfer request

```http
POST /api/transfers
{
  "fromLocationType": "WAREHOUSE",
  "fromLocationId": "warehouse-uuid",
  "toLocationType": "STORE",
  "toLocationId": "store-uuid",
  "productId": "product-uuid",
  "requestedQuantity": 100,
  "priority": "NORMAL",
  "notes": "Regular restock"
}
```

**Result:** Transfer created with status PENDING

### 2. Review and Approve (APPROVED)

**Who:** Warehouse Manager or GM+  
**Action:** Review and approve with quantity

```http
PUT /api/transfers/{id}/approve-and-send
{
  "approvedQuantity": 100,
  "carrierName": "ABC Transport",
  "carrierPhone": "+1234567890",
  "carrierVehicle": "Truck-001",
  "estimatedDeliveryAt": "2026-02-09T14:00:00Z"
}
```

**Result:** 
- Status changes to IN_TRANSIT
- ✅ Warehouse inventory deducted
- ✅ Warehouse withdrawal log created
- Carrier assigned

**Or Reject:**
```http
PUT /api/transfers/{id}/reject
{
  "reason": "Insufficient stock"
}
```

### 3. Mark as Ready (READY) - Optional

**Who:** Warehouse Staff  
**Action:** Mark items packed and ready

```http
PUT /api/transfers/{id}/ready
{
  "packedBy": "John Doe",
  "notes": "Packed in 2 boxes"
}
```

**Result:** Status changes to READY

### 4. Start Delivery (IN_TRANSIT)

**Who:** Carrier/Driver  
**Action:** Pick up and start delivery

```http
PUT /api/transfers/{id}/pickup
{
  "carrierName": "ABC Transport",
  "qrCode": "generated-qr-code"
}
```

**Result:**
- Status changes to IN_TRANSIT
- ✅ Warehouse inventory already deducted (at approval)
- QR code stored for verification

### 5. Mark as Delivered (DELIVERED)

**Who:** Carrier/Driver  
**Action:** Confirm delivery

```http
PUT /api/transfers/{id}/delivered
{
  "proofOfDeliveryUrl": "https://...",
  "condition": "GOOD"
}
```

**Result:** Status changes to DELIVERED

### 6. Receive and Complete (COMPLETED)

**Who:** Store Receiver  
**Action:** Confirm receipt with quantities

```http
PUT /api/transfers/{id}/receive
{
  "receivedQuantity": 95,
  "damagedQuantity": 5,
  "receiverName": "Jane Smith",
  "receiverSignatureUrl": "https://...",
  "receiptNotes": "5 units damaged in transit"
}
```

**Result:**
- Status changes to COMPLETED
- ✅ Store product quantity updated (+95)
- ✅ Store restock record created
- Transfer marked complete

## Inventory Impact

### When Transfer is IN_TRANSIT (Approved/Pickup)
```
Warehouse:
  - Inventory: -100 units
  - Withdrawal Log: Created (TRANSFER_OUT)

Store:
  - Inventory: No change yet
  - Restock Log: None yet
```

### When Transfer is COMPLETED (Received)
```
Warehouse:
  - Inventory: Already deducted
  - Withdrawal Log: Already created

Store:
  - Inventory: +95 units (good quantity)
  - Restock Log: Created (TRANSFER_IN)
```

## Product Handling

### New Store Product
If store doesn't have product with same SKU:
- ✅ New product created in store
- ✅ Quantity set to received amount
- ✅ Attributes copied from source

### Existing Store Product
If store already has product with same SKU:
- ✅ Existing product found
- ✅ Quantity increased
- ✅ No duplicate created

## Error Handling

### Common Errors

**Insufficient Inventory:**
```json
{
  "error": "Insufficient inventory in warehouse. Available: 50, Required: 100"
}
```

**Invalid Status Transition:**
```json
{
  "error": "Only DELIVERED transfers can be received"
}
```

**Duplicate Receipt:**
```json
{
  "error": "Transfer already completed"
}
```

## Best Practices

1. **Always approve before pickup** - Ensures inventory is reserved
2. **Use damage tracking** - Separate damaged from good quantity
3. **Include notes** - Document any issues or special handling
4. **Verify QR codes** - Use for delivery verification
5. **Check quantities** - Validate received vs approved amounts

## API Reference

Full API documentation: [API.md](API.md)

## Troubleshooting

See [TROUBLESHOOTING.md](TROUBLESHOOTING.md) for common issues and solutions.
