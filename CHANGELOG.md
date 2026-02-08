# Changelog

All notable changes to InventSight Backend will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [0.2.0] - 2026-02-08

### 🎉 Major Features

#### Complete Transfer Request Workflow
- ✅ **Warehouse to Store Transfers** - Full end-to-end transfer management
  - Create transfer requests with product, quantity, and destination
  - Multi-step approval workflow (Pending → Approved → In Transit → Delivered → Completed)
  - Carrier information tracking (name, phone, vehicle, estimated delivery)
  - QR code generation for delivery verification
  - Digital signature support for proof of delivery
  - Damaged goods tracking and reporting

#### Warehouse Inventory Management
- ✅ **Warehouse Inventory Tracking** - Complete warehouse stock management
  - Real-time inventory levels per warehouse
  - Available, reserved, held, damaged, and expired quantity tracking
  - Low stock alerts and reorder point notifications
  - Location tracking within warehouse (aisle, bin, shelf)
  - Warehouse-to-warehouse transfers support

- ✅ **Warehouse Inventory Additions** - Restock/receipt tracking
  - Receive inventory from suppliers
  - Transfer-in from other warehouses
  - Batch number and expiry date tracking
  - Supplier information and unit cost tracking
  - Manual adjustments with reason codes

- ✅ **Warehouse Inventory Withdrawals** - Outbound inventory tracking
  - Transfer-out transactions (to stores/warehouses)
  - Issue transactions (sales, consumption)
  - Damage/loss/theft tracking
  - Expired inventory disposal
  - Complete audit trail with reference numbers

#### Store Inventory Management
- ✅ **Store Inventory Additions** - Store restock tracking
  - Manual restocking
  - Transfer-in from warehouses
  - Batch restock operations
  - Restock history with date, user, and notes

### 🐛 Critical Bug Fixes

#### Transfer Receipt Fixes
- **Fixed: Idempotency filter consuming request body**
  - Issue: Filter read request body before controller, causing null body errors
  - Fix: Modified filter to not read body when replaying cached responses
  - Impact: Transfer receipt confirmations now work correctly

- **Fixed: Store validation failure on StoreInventoryAddition**
  - Issue: `store` field was null causing constraint violation
  - Fix: Get destination store from transfer.toLocationId instead of product.store
  - Impact: Restock records created successfully

- **Fixed: Duplicate SKU constraint violation**
  - Issue: Trying to update warehouse product with store_id when store product exists
  - Fix: Find existing store product by SKU + store_id, update if exists, create if not
  - Impact: Transfer receipts work for both new and existing store products

- **Fixed: Store product quantity not updating**
  - Issue: Transfer updated wrong product entity
  - Fix: Properly find/create store product and update its quantity
  - Impact: Store inventory counts now reflect received transfers

#### Warehouse Withdrawal Logging
- **Fixed: Missing warehouse withdrawal records for transfers**
  - Issue: No withdrawal log created when inventory left warehouse
  - Fix: Create WarehouseInventoryWithdrawal record with TRANSFER_OUT type
  - Impact: Complete audit trail, warehouse "Sales" tab now populated

### 🎯 Enhancements

#### Exception Logging
- Added comprehensive exception logging to transfer receipt endpoint
- Logs full stack trace and all caused-by exceptions
- Easier debugging of production issues

#### Transfer Workflow Improvements
- Added idempotency key support for duplicate request prevention
- Enhanced validation for transfer state transitions
- Improved error messages for better user experience

### 📊 Current Features (v0.2.0)

#### ✅ Fully Working Features

**Warehouse Management:**
- Create, update, and manage warehouses
- Assign warehouses to employees with permissions (READ/READ_WRITE)
- Warehouse inventory tracking (additions, withdrawals, current levels)
- Warehouse sales/withdrawals tab with transaction history
- Warehouse restocks tab with receipt history
- Warehouse permissions (GM+ can manage, employees have read/write access)

**Transfer Requests:**
- Create transfer requests (warehouse → store, store → store, warehouse → warehouse)
- Approval workflow with quantity adjustments
- Mark as ready for pickup
- Assign carrier and start delivery
- Mark as delivered with proof of delivery
- Receive and complete with damaged goods tracking
- Transfer timeline and status tracking
- Idempotency support for reliable operations

**Store Inventory:**
- Product quantity management
- Store inventory additions (restocks)
- Store restock history with full audit trail
- Transfer-in records with reference to source transfer

**User Management:**
- Role-based access control (GM, MANAGER, EMPLOYEE)
- Company-store-user associations
- Warehouse permissions management
- Activity logging

**Product Management:**
- Predefined items system
- Product creation in stores and warehouses
- SKU and barcode support
- Category management
- Pricing (retail, cost, owner-set)
- Low stock thresholds

#### 🚧 Partial/In Progress Features

**Warehouse Features:**
- Warehouse-to-warehouse transfers (backend ready, limited testing)
- Reserved quantity tracking (data model ready, workflow incomplete)
- Held/damaged/expired inventory (tracking exists, no UI workflow)

**Store Features:**
- Store-to-warehouse returns (not implemented)
- Store sales integration (separate module, not connected to transfers)

**Reporting:**
- Basic inventory counts (working)
- Transfer history (working)
- Advanced analytics (not implemented)

#### ❌ Known Limitations

**Transfer Workflow:**
- Cannot cancel transfers in IN_TRANSIT or DELIVERED status
- No partial receipt support (must receive full quantity)
- Damaged quantity tracked but not separated in inventory
- No automatic reorder based on transfers

**Warehouse:**
- No batch/lot tracking for warehouse inventory
- No FIFO/LIFO inventory valuation
- No automatic stock level optimization
- No warehouse capacity management

**Permissions:**
- Warehouse permissions are basic (READ vs READ_WRITE)
- No granular permissions (e.g., approve transfers but not create)
- No temporary permission grants

**Integration:**
- No third-party logistics (3PL) integration
- No barcode scanning during receipt
- No automatic carrier tracking updates

### 🔧 Technical Improvements

**Database:**
- Added `warehouse_inventory_withdrawals` table
- Added `store_inventory_additions` table
- Added unique constraint `idx_products_sku_store_unique`
- Added foreign keys for referential integrity

**Backend Architecture:**
- Improved transaction handling in transfer service
- Better separation of warehouse vs store inventory logic
- Enhanced error handling and logging
- Idempotency key filter for duplicate prevention

**API Endpoints:**
- `POST /api/transfers/{id}/receive` - Receive transfer at destination
- `GET /api/warehouse-inventory/warehouse/{id}/withdrawals` - Warehouse sales
- `GET /api/warehouse-inventory/warehouse/{id}/additions` - Warehouse restocks
- `POST /api/warehouse-inventory/add` - Add warehouse inventory
- `POST /api/warehouse-inventory/withdraw` - Withdraw warehouse inventory

### 📝 Migration Notes

**Database Migrations Required:**
- Ensure `warehouse_inventory_withdrawals` table exists
- Ensure `store_inventory_additions` table exists
- Ensure `idx_products_sku_store_unique` constraint is in place
- Run any pending Flyway migrations

**Configuration:**
No configuration changes required for this release.

### 🎯 What's Next (v0.3.0 Roadmap)

**Planned Features:**
- [ ] Transfer cancellation workflow
- [ ] Partial receipt support
- [ ] Batch/lot tracking
- [ ] Advanced reporting and analytics
- [ ] Mobile barcode scanning
- [ ] Automated low stock reordering
- [ ] Store-to-warehouse returns
- [ ] Multi-warehouse inventory optimization

**Improvements:**
- [ ] Performance optimization for large inventories
- [ ] Real-time inventory updates via WebSocket
- [ ] Enhanced permission system
- [ ] Audit log viewer UI
- [ ] Excel/CSV import/export

### 🙏 Contributors

- Leon Win (WinKyaw) - Core development
- GitHub Copilot - Code assistance and debugging

---

## [0.1.0] - 2026-01-11

### Initial Release
- Basic store management
- Product management
- User authentication and authorization
- Company and store associations
- Initial warehouse infrastructure

---

For more details, see the [Release Notes](RELEASE_NOTES.md).
