# InventSight Item/Product Management API

This document describes the complete Item/Product Management API endpoints available in the InventSight backend system.

## Base URL
```
http://localhost:8080/items
```

## Authentication
All endpoints require authentication via JWT token in the Authorization header:
```
Authorization: Bearer <jwt_token>
```

## Endpoints

### Basic CRUD Operations

#### 1. Get All Items
```http
GET /items?page=0&size=20&sortBy=name&sortDir=asc&category=Electronics&active=true
```
**Response:** Paginated list of items with ProductResponse objects

#### 2. Get Item by ID
```http
GET /items/{id}
```
**Response:** Single ProductResponse object

#### 3. Create New Item
```http
POST /items
Content-Type: application/json

{
    "name": "Product Name",
    "description": "Product description",
    "sku": "PROD-001",
    "category": "Electronics",
    "price": 99.99,
    "costPrice": 50.00,
    "quantity": 100,
    "maxQuantity": 500,
    "unit": "pieces",
    "supplier": "Supplier Name",
    "location": "Warehouse A",
    "expiryDate": "2025-12-31",
    "lowStockThreshold": 10,
    "reorderLevel": 25
}
```
**Response:** Created ProductResponse object

#### 4. Update Item
```http
PUT /items/{id}
Content-Type: application/json

{
    "name": "Updated Product Name",
    "price": 109.99,
    "quantity": 150
}
```
**Response:** Updated ProductResponse object

#### 5. Soft Delete Item
```http
DELETE /items/{id}
```
**Response:** Success message

### Search and Filter Operations

#### 6. Search Items
```http
GET /items/search?query=electronics&page=0&size=20&sortBy=name&sortDir=asc
```
**Response:** Paginated search results

#### 7. Get Items by Category
```http
GET /items/category/Electronics?page=0&size=20
```
**Response:** List of items in the specified category

#### 8. Get Item by SKU
```http
GET /items/sku/PROD-001
```
**Response:** Single ProductResponse object

#### 9. Get Low Stock Items
```http
GET /items/low-stock
```
**Response:** List of items with quantity below their low stock threshold

### Stock Management Operations

#### 10. Add Stock
```http
POST /items/{id}/stock/add
Content-Type: application/json

{
    "quantity": 50,
    "reason": "Stock replenishment"
}
```
**Response:** Updated ProductResponse object

#### 11. Reduce Stock
```http
POST /items/{id}/stock/reduce
Content-Type: application/json

{
    "quantity": 25,
    "reason": "Sale transaction"
}
```
**Response:** Updated ProductResponse object

#### 12. Update Stock Directly
```http
PUT /items/{id}/stock
Content-Type: application/json

{
    "quantity": 100,
    "reason": "Stock adjustment"
}
```
**Response:** Updated ProductResponse object

### Bulk Operations

#### 13. Import Items (Bulk Create)
```http
POST /items/import
Content-Type: application/json

{
    "products": [
        {
            "name": "Bulk Product 1",
            "sku": "BULK-001",
            "category": "Electronics",
            "price": 49.99,
            "quantity": 50
        },
        {
            "name": "Bulk Product 2",
            "sku": "BULK-002",
            "category": "Electronics", 
            "price": 59.99,
            "quantity": 30
        }
    ],
    "skipDuplicates": true,
    "validateStock": true
}
```
**Response:** Import results with success/error counts

#### 14. Export Items
```http
GET /items/export?category=Electronics&supplier=TechCorp&activeOnly=true
```
**Response:** Export data with items and metadata

### Analytics Operations

#### 15. Get Inventory Valuation
```http
GET /items/analytics/valuation
```
**Response:**
```json
{
    "totalValue": 50000.00,
    "totalProducts": 150,
    "averageValue": 333.33,
    "calculatedAt": "2025-08-28T08:00:00",
    "calculatedBy": "WinKyaw"
}
```

#### 16. Get Inventory Turnover
```http
GET /items/analytics/turnover
```
**Response:**
```json
{
    "categoryDistribution": {
        "Electronics": 75,
        "Furniture": 25,
        "Clothing": 50
    },
    "supplierDistribution": {
        "TechCorp": 60,
        "FurniturePlus": 40,
        "FashionWorld": 50
    },
    "lowStockCount": 5,
    "outOfStockCount": 2,
    "calculatedAt": "2025-08-28T08:00:00",
    "calculatedBy": "WinKyaw"
}
```

#### 17. Get Item Statistics
```http
GET /items/statistics
```
**Response:**
```json
{
    "totalItems": 150,
    "activeItems": 145,
    "lowStockItems": 5,
    "outOfStockItems": 2,
    "totalValue": 50000.00,
    "categories": ["Electronics", "Furniture", "Clothing"],
    "suppliers": ["TechCorp", "FurniturePlus", "FashionWorld"],
    "currentDateTime": "2025-08-28T08:00:00",
    "currentUser": "WinKyaw"
}
```

## Data Models

### ProductRequest (Input)
```json
{
    "name": "string (required)",
    "description": "string (optional)",
    "sku": "string (optional, auto-generated if not provided)",
    "category": "string (required)",
    "price": "number (required, > 0)",
    "costPrice": "number (optional, >= 0)",
    "quantity": "number (required, >= 0)",
    "maxQuantity": "number (optional, >= 0)",
    "unit": "string (optional)",
    "supplier": "string (optional)",
    "location": "string (optional)",
    "barcode": "string (optional)",
    "expiryDate": "date (optional, YYYY-MM-DD)",
    "lowStockThreshold": "number (optional)",
    "reorderLevel": "number (optional)"
}
```

### ProductResponse (Output)
```json
{
    "id": "number",
    "name": "string",
    "description": "string",
    "sku": "string",
    "category": "string",
    "price": "number",
    "costPrice": "number",
    "quantity": "number",
    "maxQuantity": "number",
    "unit": "string",
    "supplier": "string",
    "location": "string",
    "barcode": "string",
    "expiryDate": "date",
    "lowStockThreshold": "number",
    "reorderLevel": "number",
    "isActive": "boolean",
    "createdAt": "datetime",
    "updatedAt": "datetime",
    "createdBy": "string",
    "updatedBy": "string",
    "totalValue": "number (calculated)",
    "isLowStock": "boolean (calculated)",
    "isOutOfStock": "boolean (calculated)",
    "needsReorder": "boolean (calculated)",
    "isExpired": "boolean (calculated)",
    "isNearExpiry": "boolean (calculated)",
    "profitMargin": "number (calculated)"
}
```

### StockUpdateRequest
```json
{
    "quantity": "number (required)",
    "reason": "string (optional)"
}
```

## Error Responses

### Common Error Codes
- **400 Bad Request:** Invalid input data
- **401 Unauthorized:** Missing or invalid authentication
- **404 Not Found:** Item not found
- **409 Conflict:** Duplicate SKU
- **500 Internal Server Error:** Server error

### Error Response Format
```json
{
    "success": false,
    "message": "Error description",
    "system": "InventSight System",
    "timestamp": "2025-08-28T08:00:00"
}
```

## Features

### Input Validation
- All required fields are validated
- Price and quantity constraints enforced
- SKU uniqueness enforced
- Date format validation

### Business Logic
- Automatic SKU generation if not provided
- Stock level monitoring (low stock, out of stock, reorder alerts)
- Profit margin calculation
- Expiry date monitoring
- Audit trail (created/updated by/at fields)

### Security
- JWT token authentication required
- Activity logging for all operations
- Rate limiting support
- CORS enabled for frontend integration

### Performance
- Pagination support for large datasets
- Efficient database queries
- Caching support ready
- Connection pooling optimized

This API provides complete inventory/item management functionality suitable for integration with frontend applications and external systems.