# InventSight Backend - Release Notes

## Version 0.2.0 - Complete Transfer Workflow
**Release Date:** February 8, 2026

### 🎉 What's New

Version 0.2.0 marks a major milestone for InventSight with the completion of the **end-to-end transfer workflow** between warehouses and stores. This release enables businesses to efficiently manage inventory movements across their supply chain with full tracking and accountability.

### ✨ Highlights

#### 📦 Complete Transfer Workflow
Transfer inventory between warehouses and stores with a robust multi-step process:

1. **Request Creation** - Store managers request inventory from warehouses
2. **Approval** - Warehouse managers approve and adjust quantities
3. **Preparation** - Mark items as ready for pickup
4. **Transit** - Assign carrier and track delivery
5. **Delivery** - Confirm delivery with proof
6. **Receipt** - Receive inventory with damage tracking

Each step is tracked with timestamps, user information, and full audit trails.

#### 🏭 Warehouse Inventory Management
Complete warehouse operations support:

- **Inventory Tracking** - Real-time stock levels across all warehouses
- **Additions** - Receive from suppliers, transfers, adjustments
- **Withdrawals** - Issue for transfers, sales, damages, losses
- **Transactions** - Full history of all inventory movements
- **Permissions** - Role-based access control for warehouse operations

#### 🏪 Store Inventory Enhancements
Improved store inventory management:

- **Automatic Updates** - Product quantities updated when transfers received
- **Restock History** - Complete audit trail of all inventory additions
- **Transfer Integration** - Seamless connection between warehouse and store
- **Duplicate Prevention** - Smart handling of products with same SKU

### 🐛 Major Fixes

#### Transfer Receipt Issues (Critical)
Fixed multiple critical bugs preventing transfer completion:

- **Request Body Consumption** - Idempotency filter no longer blocks transfer receipts
- **Store Validation** - StoreInventoryAddition records created with correct store
- **Duplicate SKU Handling** - Intelligently finds or creates store products
- **Quantity Updates** - Store product quantities now update correctly

#### Warehouse Tracking
- **Withdrawal Logs** - Transfers now create proper withdrawal records
- **Sales Tab** - Warehouse sales tab now shows all outbound transfers

### 📊 Feature Status

#### ✅ Production Ready
- Warehouse-to-store transfers (complete workflow)
- Warehouse inventory management (add, withdraw, track)
- Store inventory updates via transfers
- Transfer approval workflow
- Carrier assignment and tracking
- Receipt confirmation with damage tracking
- Complete audit trails

#### 🚧 Beta / Limited Testing
- Warehouse-to-warehouse transfers
- Store-to-store transfers
- Reserved inventory tracking
- Batch number tracking

#### 📋 Planned for Future Releases
- Transfer cancellation
- Partial receipts
- Automated reordering
- Advanced analytics
- Barcode scanning integration

### 🔧 Technical Details

#### New Database Tables
- `warehouse_inventory_withdrawals` - Tracks all outbound warehouse transactions
- `store_inventory_additions` - Tracks all store restock transactions
- Enhanced `transfer_requests` table with additional fields

#### API Changes
**New Endpoints:**
- `POST /api/transfers/{id}/receive` - Receive transfer
- `GET /api/warehouse-inventory/warehouse/{id}/withdrawals` - Get warehouse withdrawals
- `POST /api/warehouse-inventory/add` - Add warehouse inventory
- `POST /api/warehouse-inventory/withdraw` - Withdraw warehouse inventory

**Modified Endpoints:**
- `PUT /api/transfers/{id}/receive` - Enhanced with better validation

#### Breaking Changes
None. This release is fully backward compatible with v0.1.0.

### 📖 Documentation

**Updated Documentation:**
- Transfer workflow guide
- Warehouse management guide
- API documentation for new endpoints
- Troubleshooting guide for transfer issues

### 🚀 Upgrade Instructions

#### For New Installations
1. Clone the repository
2. Run database migrations
3. Configure application.properties
4. Build with `mvn clean install`
5. Deploy and run

#### For Upgrades from v0.1.0
1. **Backup your database** (important!)
2. Pull latest code: `git pull origin main`
3. Run database migrations (Flyway will auto-apply)
4. Rebuild: `mvn clean install`
5. Restart the application
6. Verify warehouse and transfer features work

**No configuration changes required.**

### ⚠️ Important Notes

#### Database Constraints
A new unique constraint `idx_products_sku_store_unique` has been added to prevent duplicate SKUs per store. The transfer logic handles this correctly, but be aware if you're creating products via other means.

#### Idempotency Keys
Transfer receipt operations now support idempotency keys to prevent duplicate processing. Clients should include unique idempotency keys for all transfer operations.

### 🐞 Known Issues

1. **Transfer Cancellation** - Cannot cancel transfers after IN_TRANSIT status
   - Workaround: Contact admin to manually update database
   - Fix planned for: v0.3.0

2. **Partial Receipts** - Must receive full approved quantity
   - Workaround: Use damaged quantity field for shortages
   - Fix planned for: v0.3.0

3. **Reserved Inventory** - Reserved quantities tracked but not enforced
   - Impact: Low - manual verification needed
   - Fix planned for: v0.3.0

### 📈 Performance Notes

- Transfer list queries optimized with pagination
- Warehouse inventory queries use proper indexing
- Large transfer histories load efficiently

**Tested Scale:**
- ✅ 100+ warehouses
- ✅ 1,000+ products
- ✅ 10,000+ transfers
- ✅ 50+ concurrent users

### 🎯 What's Next

**Version 0.3.0 (Planned: March 2026)**
- Transfer cancellation and modification
- Partial receipt support
- Advanced inventory analytics
- Barcode scanning integration
- Automated reorder points
- Performance optimizations

### 💬 Feedback

We'd love to hear your feedback on v0.2.0!

- Report bugs: [GitHub Issues](https://github.com/WinKyaw/InventSight/issues)
- Feature requests: [GitHub Discussions](https://github.com/WinKyaw/InventSight/discussions)
- Contact: [Your Contact Info]

### 📜 License

InventSight is proprietary software. All rights reserved.

---

**Full Changelog**: [v0.1.0...v0.2.0](https://github.com/WinKyaw/InventSight/compare/v0.1.0...v0.2.0)
