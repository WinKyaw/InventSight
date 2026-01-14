-- Merchants table
CREATE TABLE IF NOT EXISTS merchants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    contact_person VARCHAR(255),
    phone VARCHAR(50),
    email VARCHAR(255),
    address TEXT,
    location VARCHAR(255),
    notes TEXT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(255),
    CONSTRAINT fk_merchant_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE
);

CREATE INDEX idx_merchants_company ON merchants(company_id);
CREATE INDEX idx_merchants_active ON merchants(is_active);

-- Product Ads table
CREATE TABLE IF NOT EXISTS product_ads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    store_id UUID NOT NULL,
    product_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    product_sku VARCHAR(100),
    description TEXT,
    unit_price DECIMAL(10, 2) NOT NULL,
    available_quantity INTEGER NOT NULL,
    min_order_quantity INTEGER DEFAULT 1,
    image_url VARCHAR(500),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    created_by VARCHAR(255),
    CONSTRAINT fk_product_ad_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_product_ad_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE
);

CREATE INDEX idx_product_ads_company ON product_ads(company_id);
CREATE INDEX idx_product_ads_store ON product_ads(store_id);
CREATE INDEX idx_product_ads_active ON product_ads(is_active);
CREATE INDEX idx_product_ads_expires ON product_ads(expires_at);

-- Transfer Requests table
CREATE TABLE IF NOT EXISTS transfer_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    company_id UUID NOT NULL,
    product_id UUID NOT NULL,
    from_warehouse_id UUID NOT NULL,
    to_store_id UUID NOT NULL,
    requested_quantity INTEGER NOT NULL,
    approved_quantity INTEGER,
    status VARCHAR(20) DEFAULT 'PENDING',
    priority VARCHAR(20) DEFAULT 'MEDIUM',
    reason TEXT,
    notes TEXT,
    requested_by UUID NOT NULL,
    requested_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    approved_by UUID,
    approved_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transfer_company FOREIGN KEY (company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_transfer_warehouse FOREIGN KEY (from_warehouse_id) REFERENCES warehouses(id) ON DELETE CASCADE,
    CONSTRAINT fk_transfer_store FOREIGN KEY (to_store_id) REFERENCES stores(id) ON DELETE CASCADE,
    CONSTRAINT fk_transfer_requester FOREIGN KEY (requested_by) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_transfer_approver FOREIGN KEY (approved_by) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_transfer_requests_company ON transfer_requests(company_id);
CREATE INDEX idx_transfer_requests_warehouse ON transfer_requests(from_warehouse_id);
CREATE INDEX idx_transfer_requests_store ON transfer_requests(to_store_id);
CREATE INDEX idx_transfer_requests_status ON transfer_requests(status);
CREATE INDEX idx_transfer_requests_priority ON transfer_requests(priority);

-- Marketplace Orders table
CREATE TABLE IF NOT EXISTS marketplace_orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    buyer_company_id UUID NOT NULL,
    buyer_store_id UUID NOT NULL,
    seller_company_id UUID NOT NULL,
    seller_store_id UUID NOT NULL,
    product_ad_id UUID NOT NULL,
    product_name VARCHAR(255) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10, 2) NOT NULL,
    total_price DECIMAL(10, 2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    delivery_address TEXT,
    notes TEXT,
    ordered_by UUID NOT NULL,
    ordered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    confirmed_at TIMESTAMP,
    delivered_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_marketplace_buyer_company FOREIGN KEY (buyer_company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_marketplace_buyer_store FOREIGN KEY (buyer_store_id) REFERENCES stores(id) ON DELETE CASCADE,
    CONSTRAINT fk_marketplace_seller_company FOREIGN KEY (seller_company_id) REFERENCES companies(id) ON DELETE CASCADE,
    CONSTRAINT fk_marketplace_seller_store FOREIGN KEY (seller_store_id) REFERENCES stores(id) ON DELETE CASCADE,
    CONSTRAINT fk_marketplace_product_ad FOREIGN KEY (product_ad_id) REFERENCES product_ads(id) ON DELETE CASCADE,
    CONSTRAINT fk_marketplace_orderer FOREIGN KEY (ordered_by) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_marketplace_orders_buyer_company ON marketplace_orders(buyer_company_id);
CREATE INDEX idx_marketplace_orders_seller_company ON marketplace_orders(seller_company_id);
CREATE INDEX idx_marketplace_orders_status ON marketplace_orders(status);
CREATE INDEX idx_marketplace_orders_ordered_at ON marketplace_orders(ordered_at);
