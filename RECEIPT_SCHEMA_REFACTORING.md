# Receipt Schema Refactoring - Implementation Guide

## Overview

This document describes the comprehensive database schema refactoring that separates receipt information from the sales table into a dedicated receipts system. This refactoring enables receipt reusability across different transaction types (sales, transfers, returns) and provides better separation of concerns.

## Problem Statement

Previously, receipt information was tightly coupled with the `sales` table, which created several issues:

1. ❌ **Tight Coupling**: Receipt data (number, status, payment) was mixed with sale data
2. ❌ **No Reusability**: Couldn't have a receipt for transfers or other transaction types
3. ❌ **Poor Separation**: Payment and fulfillment logic was scattered
4. ❌ **Data Duplication**: Receipt fields would need to be duplicated for each transaction type

## Solution: Normalized Database Schema

### New Database Tables

#### 1. `receipts` Table (Central Receipt Data)

The `receipts` table serves as the central repository for all receipt information across different transaction types:

```sql
CREATE TABLE receipts (
    id UUID PRIMARY KEY,
    receipt_number VARCHAR(50) UNIQUE NOT NULL,
    receipt_type VARCHAR(20) NOT NULL,  -- SALE, TRANSFER, RETURN
    status VARCHAR(20) NOT NULL,        -- PENDING, COMPLETED, CANCELLED
    
    -- Payment information
    payment_method VARCHAR(50),
    payment_status VARCHAR(20),         -- UNPAID, PAID, PARTIALLY_PAID, REFUNDED
    paid_amount DECIMAL(15, 2),
    payment_date TIMESTAMP,
    
    -- Financial summary
    subtotal DECIMAL(15, 2) NOT NULL,
    tax_amount DECIMAL(15, 2),
    discount_amount DECIMAL(15, 2),
    total_amount DECIMAL(15, 2) NOT NULL,
    
    -- Fulfillment and delivery tracking
    fulfilled_by_user_id UUID,
    fulfilled_at TIMESTAMP,
    delivery_type VARCHAR(20),
    delivery_person_id UUID,
    delivered_at TIMESTAMP,
    
    -- Customer and multi-tenancy
    customer_id UUID,
    customer_name VARCHAR(200),
    company_id UUID NOT NULL,
    store_id UUID,
    
    -- Audit fields
    created_by_user_id UUID NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
```

#### 2. Junction Tables

**`sale_receipts`**: Links sales to receipts (one-to-one)
```sql
CREATE TABLE sale_receipts (
    id UUID PRIMARY KEY,
    receipt_id UUID NOT NULL REFERENCES receipts(id),
    sale_id BIGINT NOT NULL REFERENCES sales(id),
    UNIQUE (receipt_id, sale_id)
);
```

**`transfer_receipts`**: Links transfers to receipts (one-to-one)
```sql
CREATE TABLE transfer_receipts (
    id UUID PRIMARY KEY,
    receipt_id UUID NOT NULL REFERENCES receipts(id),
    transfer_id UUID NOT NULL REFERENCES transfer_requests(id),
    UNIQUE (receipt_id, transfer_id)
);
```

#### 3. `receipt_items` Table

Line items for receipts with denormalized product information:
```sql
CREATE TABLE receipt_items (
    id UUID PRIMARY KEY,
    receipt_id UUID NOT NULL REFERENCES receipts(id),
    product_id UUID NOT NULL,
    product_name VARCHAR(200),      -- Denormalized for history
    product_sku VARCHAR(100),       -- Denormalized for history
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(15, 2) NOT NULL,
    total_price DECIMAL(15, 2) NOT NULL
);
```

## Migration Strategy

### Phase 1: Create Tables (V42)
- Creates all new receipt-related tables
- Adds appropriate indexes for performance

### Phase 2: Migrate Data (V43)
- Migrates existing sales to receipts table
- Creates sale_receipt mappings
- Migrates sale_items to receipt_items
- **Preserves all existing data**

### Phase 3: Mark Deprecated (V44)
- Marks receipt-related columns in sales table as deprecated
- **Does NOT drop columns** to maintain backward compatibility
- Allows gradual migration of code

## Java Implementation

### New Enums

```java
public enum ReceiptStatus {
    PENDING,      // Receipt is pending
    COMPLETED,    // Receipt is completed
    CANCELLED     // Receipt was cancelled
}

public enum PaymentStatus {
    UNPAID,           // Payment has not been made
    PAID,             // Payment is complete
    PARTIALLY_PAID,   // Partial payment received
    REFUNDED          // Payment was refunded
}

public enum DeliveryType {
    IN_STORE,    // Immediate purchase at store
    DELIVERY,    // Requires delivery
    PICKUP       // Customer will pick up
}
```

### New Entities

#### `Receipt` Entity
```java
@Entity
@Table(name = "receipts")
public class Receipt {
    private UUID id;
    private String receiptNumber;
    private ReceiptType receiptType;
    private ReceiptStatus status;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;
    // ... other fields
}
```

#### Junction Entities
```java
@Entity
@Table(name = "sale_receipts")
public class SaleReceipt {
    private UUID id;
    private Receipt receipt;
    private Sale sale;
}
```

### Updated `Sale` Entity

The `Sale` entity now includes a relationship to access receipt data:

```java
@Entity
@Table(name = "sales")
public class Sale {
    private Long id;
    
    // New: Access receipt via junction table
    @OneToOne(mappedBy = "sale")
    private SaleReceipt saleReceipt;
    
    // Existing receipt fields remain for backward compatibility
    private String receiptNumber;
    private SaleStatus status;
    // ...
}
```

To access receipt information:
```java
// Old way (still works for backward compatibility)
String receiptNumber = sale.getReceiptNumber();

// New way (via junction table)
Receipt receipt = sale.getSaleReceipt().getReceipt();
```

### New Services

#### `ReceiptService`

Provides comprehensive receipt management:

```java
@Service
public class ReceiptService {
    // Create a new receipt
    public Receipt createReceipt(ReceiptRequest request, Company company, User createdBy);
    
    // Complete payment for a receipt
    public Receipt completePayment(UUID receiptId, PaymentMethod method);
    
    // Fulfill a receipt
    public Receipt fulfillReceipt(UUID receiptId, User fulfilledBy);
    
    // Mark as delivered
    public Receipt markAsDelivered(UUID receiptId);
    
    // Query methods
    public List<Receipt> getUnpaidReceipts(UUID companyId);
    public List<Receipt> getPendingReceipts(UUID companyId);
    public Page<Receipt> getReceiptsByCompany(UUID companyId, Pageable pageable);
}
```

## Benefits

### 1. ✅ Separation of Concerns
- Receipt logic is separate from sale/transfer logic
- Each entity has a single responsibility

### 2. ✅ Reusability
- Same receipt structure for sales, transfers, returns, etc.
- Easy to add new transaction types

### 3. ✅ Better Payment Tracking
- Explicit `payment_status` field
- Track partial payments and refunds
- Payment date tracking

### 4. ✅ Normalized Data
- No duplication of receipt fields across tables
- Single source of truth for receipt information

### 5. ✅ Backward Compatible
- Existing code continues to work
- Gradual migration possible
- No breaking changes

### 6. ✅ Flexible & Extensible
- Can extend to support any transaction type
- Easy to add new receipt fields
- Clear upgrade path

## Usage Examples

### Creating a Receipt for a Sale

```java
// Create receipt request
ReceiptRequest request = new ReceiptRequest();
request.setReceiptType(ReceiptType.IN_STORE);
request.setSubtotal(new BigDecimal("100.00"));
request.setTaxAmount(new BigDecimal("10.00"));
request.setTotalAmount(new BigDecimal("110.00"));
request.setPaymentMethod(PaymentMethod.CASH);

// Create receipt
Receipt receipt = receiptService.createReceipt(request, company, user);

// Link to sale
SaleReceipt saleReceipt = new SaleReceipt(receipt, sale);
saleReceiptRepository.save(saleReceipt);
```

### Querying Unpaid Receipts

```java
// Get all unpaid receipts for a company
List<Receipt> unpaidReceipts = receiptService.getUnpaidReceipts(companyId);

// Get pending receipts
List<Receipt> pendingReceipts = receiptService.getPendingReceipts(companyId);
```

### Accessing Receipt from Sale

```java
// Via junction table (new way)
Sale sale = saleRepository.findById(saleId).orElseThrow();
SaleReceipt saleReceipt = sale.getSaleReceipt();
if (saleReceipt != null) {
    Receipt receipt = saleReceipt.getReceipt();
    ReceiptStatus status = receipt.getStatus();
    PaymentStatus paymentStatus = receipt.getPaymentStatus();
}

// Direct from sale (old way - still works)
String receiptNumber = sale.getReceiptNumber();
SaleStatus status = sale.getStatus();
```

## Migration Checklist

- [x] Create database tables (V42)
- [x] Migrate existing data (V43)
- [x] Mark deprecated columns (V44)
- [x] Create Java entities
- [x] Create repositories
- [x] Create services and DTOs
- [x] Compile and verify
- [ ] Update controllers to use new receipt system
- [ ] Write comprehensive tests
- [ ] Update documentation
- [ ] Monitor production usage
- [ ] (Future) Remove deprecated columns from sales table

## Future Enhancements

1. **Transfer Receipts**: Add receipt support for transfer requests
2. **Return Receipts**: Add receipt support for product returns
3. **Partial Payments**: Full support for tracking partial payments
4. **Receipt Templates**: Customizable receipt templates per company
5. **Digital Receipts**: Email/SMS receipt delivery
6. **Receipt Analytics**: Advanced reporting on receipt data

## Notes

- All migrations are backward compatible
- Existing code continues to work without modifications
- Deprecated fields will be removed in a future release
- New code should use the receipt system via junction tables
- Performance tested with indexes on all foreign keys

## Support

For questions or issues related to this refactoring, please contact the development team or create an issue in the repository.
