# Receipt Type API Documentation

## Overview

This document provides comprehensive information about the Receipt Type feature in the InventSight backend API. The backend is **fully implemented** and ready to accept `receiptType` in requests.

## ✅ Backend Implementation Status

The backend API correctly:
1. Accepts `receiptType` in POST /receipts requests
2. Stores `receiptType` in the database
3. Returns `receiptType` in API responses
4. Supports filtering by `receiptType`

## Receipt Types

The system supports four receipt types:

```java
public enum ReceiptType {
    IN_STORE,    // Immediate purchase at store (default)
    DELIVERY,    // Requires delivery
    PICKUP,      // Customer will pick up
    HOLD         // Order on hold / pending
}
```

## API Endpoints

### Create Receipt

**Endpoint:** `POST /receipts`

**Request Body:**
```json
{
  "items": [
    {
      "productId": "uuid-here",
      "quantity": 2
    }
  ],
  "receiptType": "PICKUP",           // ✅ REQUIRED: Send this field
  "customerName": "John Doe",
  "customerEmail": "john@example.com",
  "customerPhone": "+1234567890",
  "status": "PENDING",               // PENDING or COMPLETED
  "paymentMethod": null,             // null for PENDING, required for COMPLETED
  "notes": "Optional notes",
  "deliveryPersonId": "uuid-here",   // Optional, only for DELIVERY type
  "deliveryNotes": "Special delivery instructions"
}
```

**Response:**
```json
{
  "id": 123,
  "receiptNumber": "INV-1234567890",
  "receiptType": "PICKUP",          // ✅ Backend returns this
  "status": "PENDING",
  "customerName": "John Doe",
  "customerEmail": "john@example.com",
  "customerPhone": "+1234567890",
  "subtotal": 100.00,
  "taxAmount": 10.00,
  "discountAmount": 0.00,
  "totalAmount": 110.00,
  "paymentMethod": null,
  "items": [
    {
      "id": 1,
      "productId": "uuid-here",
      "productName": "Sample Product",
      "productSku": "SKU-001",
      "quantity": 2,
      "unitPrice": 50.00,
      "totalPrice": 100.00
    }
  ],
  "processedById": "user-uuid",
  "processedByUsername": "cashier1",
  "companyId": "company-uuid",
  "storeId": "store-uuid",
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

### Get Receipts with Filtering

**Endpoint:** `GET /receipts`

**Query Parameters:**
- `page`: Page number (default: 0)
- `size`: Page size (default: 10)
- `sortBy`: Sort field (default: "createdAt")
- `sortDir`: Sort direction (default: "desc")
- `status`: Filter by status (PENDING, COMPLETED)
- `receiptType`: Filter by receipt type (IN_STORE, PICKUP, DELIVERY, HOLD)
- `cashierId`: Filter by cashier UUID
- `startDate`: Filter by start date (ISO format)
- `endDate`: Filter by end date (ISO format)

**Example Requests:**

```bash
# Get all PICKUP receipts
GET /receipts?receiptType=PICKUP

# Get all DELIVERY receipts with PENDING status
GET /receipts?receiptType=DELIVERY&status=PENDING

# Get all HOLD receipts
GET /receipts?receiptType=HOLD

# Get all IN_STORE receipts
GET /receipts?receiptType=IN_STORE
```

**Response:**
```json
{
  "content": [
    {
      "id": 123,
      "receiptNumber": "INV-1234567890",
      "receiptType": "PICKUP",
      "status": "PENDING",
      "totalAmount": 110.00,
      ...
    }
  ],
  "totalElements": 50,
  "totalPages": 5,
  "size": 10,
  "number": 0
}
```

## Frontend Implementation Guide

### Problem Description

**Current Issue:**
- Frontend does NOT send `receiptType` when creating receipts
- All receipts default to `IN_STORE` in the backend
- Filtering by PICKUP/DELIVERY returns empty results
- Badges show "Pickup" but data has `receiptType: "IN_STORE"`

### Solution (Frontend Changes Required)

#### 1. Add Receipt Type Selector to Create Receipt Form

**File:** `app/(tabs)/receipt.tsx` or your receipt creation component

```typescript
import { useState } from 'react';
import { Ionicons } from '@expo/vector-icons';

const CreateReceiptScreen = () => {
  const [receiptType, setReceiptType] = useState<'IN_STORE' | 'PICKUP' | 'DELIVERY' | 'HOLD'>('IN_STORE');

  return (
    <View>
      {/* Receipt Type Selector */}
      <View style={styles.receiptTypeSelector}>
        <Text style={styles.label}>Receipt Type:</Text>
        <View style={styles.typeButtons}>
          <TouchableOpacity
            style={[styles.typeBtn, receiptType === 'IN_STORE' && styles.typeBtnActive]}
            onPress={() => setReceiptType('IN_STORE')}
          >
            <Ionicons 
              name="storefront" 
              size={20} 
              color={receiptType === 'IN_STORE' ? '#F97316' : '#6B7280'} 
            />
            <Text style={styles.typeBtnText}>In-Store</Text>
          </TouchableOpacity>
          
          <TouchableOpacity
            style={[styles.typeBtn, receiptType === 'PICKUP' && styles.typeBtnActive]}
            onPress={() => setReceiptType('PICKUP')}
          >
            <Ionicons 
              name="cube" 
              size={20} 
              color={receiptType === 'PICKUP' ? '#F97316' : '#6B7280'} 
            />
            <Text style={styles.typeBtnText}>Pickup</Text>
          </TouchableOpacity>
          
          <TouchableOpacity
            style={[styles.typeBtn, receiptType === 'DELIVERY' && styles.typeBtnActive]}
            onPress={() => setReceiptType('DELIVERY')}
          >
            <Ionicons 
              name="bicycle" 
              size={20} 
              color={receiptType === 'DELIVERY' ? '#F97316' : '#6B7280'} 
            />
            <Text style={styles.typeBtnText}>Delivery</Text>
          </TouchableOpacity>
          
          <TouchableOpacity
            style={[styles.typeBtn, receiptType === 'HOLD' && styles.typeBtnActive]}
            onPress={() => setReceiptType('HOLD')}
          >
            <Ionicons 
              name="pause-circle" 
              size={20} 
              color={receiptType === 'HOLD' ? '#F97316' : '#6B7280'} 
            />
            <Text style={styles.typeBtnText}>Hold</Text>
          </TouchableOpacity>
        </View>
      </View>
      
      {/* Rest of your form... */}
    </View>
  );
};

const styles = StyleSheet.create({
  receiptTypeSelector: {
    marginBottom: 16,
  },
  label: {
    fontSize: 16,
    fontWeight: '600',
    marginBottom: 8,
    color: '#1F2937',
  },
  typeButtons: {
    flexDirection: 'row',
    gap: 8,
  },
  typeBtn: {
    flex: 1,
    padding: 12,
    borderRadius: 8,
    borderWidth: 1,
    borderColor: '#D1D5DB',
    alignItems: 'center',
    gap: 4,
  },
  typeBtnActive: {
    borderColor: '#F97316',
    backgroundColor: '#FFF7ED',
  },
  typeBtnText: {
    fontSize: 12,
    color: '#374151',
  },
});
```

#### 2. Include receiptType in API Request

**File:** `context/ReceiptContext.tsx` or your receipt service

```typescript
// types/index.ts
export interface CreateReceiptRequest {
  items: Array<{
    productId: string;
    quantity: number;
  }>;
  receiptType: 'IN_STORE' | 'PICKUP' | 'DELIVERY' | 'HOLD';  // ✅ ADD THIS
  customerName?: string;
  customerEmail?: string;
  customerPhone?: string;
  status: 'PENDING' | 'COMPLETED';
  paymentMethod?: string;
  notes?: string;
  deliveryPersonId?: string;
  deliveryNotes?: string;
}

export interface Receipt {
  id: number;
  receiptNumber: string;
  receiptType: 'IN_STORE' | 'PICKUP' | 'DELIVERY' | 'HOLD';  // ✅ ADD THIS
  status: 'PENDING' | 'COMPLETED';
  customerName?: string;
  customerEmail?: string;
  customerPhone?: string;
  subtotal: number;
  taxAmount: number;
  discountAmount: number;
  totalAmount: number;
  paymentMethod?: string;
  items: ReceiptItem[];
  // ... other fields
}

// context/ReceiptContext.tsx
const handleSubmitReceipt = async () => {
  try {
    const receiptData: CreateReceiptRequest = {
      receiptType: receiptType,  // ✅ INCLUDE THIS from state
      items: receiptItems.map(item => ({
        productId: item.id,
        quantity: item.quantity,
      })),
      customerName: customerName || 'Walk-in Customer',
      customerEmail: customerEmail,
      customerPhone: customerPhone,
      status: 'PENDING',
      paymentMethod: null,
      notes: notes,
    };
    
    const response = await fetch('https://api.example.com/receipts', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${token}`,
      },
      body: JSON.stringify(receiptData),
    });
    
    const receipt: Receipt = await response.json();
    
    console.log('Created receipt:', receipt);
    console.log('Receipt type:', receipt.receiptType);  // ✅ Should match what you sent
    
    // Handle success...
  } catch (error) {
    console.error('Error creating receipt:', error);
  }
};
```

#### 3. Update Receipt Filtering

**File:** Your receipts list component

```typescript
const ReceiptsScreen = () => {
  const [selectedTab, setSelectedTab] = useState<'all' | 'pickup' | 'delivery' | 'hold'>('all');
  
  const fetchReceipts = async (receiptType?: string) => {
    const params = new URLSearchParams({
      page: '0',
      size: '20',
      status: 'PENDING',
    });
    
    if (receiptType && receiptType !== 'all') {
      params.append('receiptType', receiptType.toUpperCase());
    }
    
    const response = await fetch(`https://api.example.com/receipts?${params}`, {
      headers: {
        'Authorization': `Bearer ${token}`,
      },
    });
    
    const data = await response.json();
    return data.content;
  };
  
  useEffect(() => {
    if (selectedTab === 'all') {
      fetchReceipts();
    } else if (selectedTab === 'pickup') {
      fetchReceipts('PICKUP');
    } else if (selectedTab === 'delivery') {
      fetchReceipts('DELIVERY');
    } else if (selectedTab === 'hold') {
      fetchReceipts('HOLD');
    }
  }, [selectedTab]);
  
  return (
    <View>
      {/* Tab buttons */}
      <View style={styles.tabs}>
        <TouchableOpacity onPress={() => setSelectedTab('all')}>
          <Text>All Pending</Text>
        </TouchableOpacity>
        <TouchableOpacity onPress={() => setSelectedTab('pickup')}>
          <Text>📦 Pickup</Text>
        </TouchableOpacity>
        <TouchableOpacity onPress={() => setSelectedTab('delivery')}>
          <Text>🚚 Delivery</Text>
        </TouchableOpacity>
        <TouchableOpacity onPress={() => setSelectedTab('hold')}>
          <Text>⏸️ Hold</Text>
        </TouchableOpacity>
      </View>
      
      {/* Receipt list */}
    </View>
  );
};
```

## Testing

After implementing frontend changes:

### Test Checklist

1. **Create Receipt with PICKUP type**
   - [ ] Select "Pickup" in receipt type selector
   - [ ] Submit receipt
   - [ ] Verify API request includes `"receiptType": "PICKUP"`
   - [ ] Verify API response includes `"receiptType": "PICKUP"`
   - [ ] Receipt appears in "All Pending" tab with "📦 Pickup" badge
   - [ ] Receipt appears in "Pickup" tab

2. **Create Receipt with DELIVERY type**
   - [ ] Select "Delivery" in receipt type selector
   - [ ] Submit receipt
   - [ ] Verify API request includes `"receiptType": "DELIVERY"`
   - [ ] Verify API response includes `"receiptType": "DELIVERY"`
   - [ ] Receipt appears in "All Pending" tab with "🚚 Delivery" badge
   - [ ] Receipt appears in "Delivery" tab

3. **Create Receipt with HOLD type**
   - [ ] Select "Hold" in receipt type selector
   - [ ] Submit receipt
   - [ ] Verify API request includes `"receiptType": "HOLD"`
   - [ ] Verify API response includes `"receiptType": "HOLD"`
   - [ ] Receipt appears in "All Pending" tab with "⏸️ Hold" badge
   - [ ] Receipt appears in "Hold" tab

4. **Default IN_STORE behavior**
   - [ ] Create receipt without selecting type (should default to IN_STORE)
   - [ ] Verify receipt has `"receiptType": "IN_STORE"`
   - [ ] Receipt appears in "All Pending" tab

## API Response Validation

Use network inspection tools to verify:

```javascript
// ✅ CORRECT REQUEST
POST /receipts
{
  "items": [...],
  "receiptType": "PICKUP",  // ← Must be included
  "status": "PENDING",
  ...
}

// ✅ CORRECT RESPONSE
{
  "id": 123,
  "receiptNumber": "INV-...",
  "receiptType": "PICKUP",  // ← Backend returns this
  ...
}

// ❌ WRONG REQUEST (missing receiptType)
POST /receipts
{
  "items": [...],
  // receiptType: missing!  ← This causes the bug
  "status": "PENDING",
  ...
}

// ❌ WRONG RESULT (defaults to IN_STORE)
{
  "id": 123,
  "receiptNumber": "INV-...",
  "receiptType": "IN_STORE",  // ← Defaults because not provided
  ...
}
```

## Summary

### Backend Status: ✅ FULLY IMPLEMENTED

The backend:
- Accepts `receiptType` in requests
- Stores it in the database
- Returns it in responses
- Supports filtering

### Frontend Status: ❌ NEEDS IMPLEMENTATION

The frontend needs to:
1. Add receipt type selector UI
2. Include `receiptType` in create receipt requests
3. Update TypeScript types to include `receiptType`
4. Use `receiptType` from API responses for filtering

### Priority: 🔥 CRITICAL

Users cannot access pickup/delivery receipts in their designated tabs because the frontend doesn't send `receiptType` during creation.

## Support

For backend API questions or issues, please refer to:
- API Documentation: `API_ENHANCEMENTS_GUIDE.md`
- Receipt Schema: `RECEIPT_SCHEMA_REFACTORING.md`
- Backend source: `src/main/java/com/pos/inventsight/service/SaleService.java`

For frontend implementation, this is a separate repository (InventSightAPP).
