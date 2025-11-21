-- V10__create_preexisting_items_table.sql
-- Create preexisting_items table for store-scoped catalog items

CREATE TABLE IF NOT EXISTS preexisting_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL,
    item_name VARCHAR(200) NOT NULL,
    category VARCHAR(100),
    default_price DECIMAL(19, 4),
    description VARCHAR(1000),
    sku VARCHAR(100) NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_by VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_preexisting_item_store FOREIGN KEY (store_id) REFERENCES stores(id) ON DELETE CASCADE,
    CONSTRAINT uk_preexisting_item_store_sku UNIQUE (store_id, sku)
);

-- Create indexes for better query performance
CREATE INDEX IF NOT EXISTS idx_preexisting_items_store_id ON preexisting_items(store_id);
CREATE INDEX IF NOT EXISTS idx_preexisting_items_category ON preexisting_items(category);
CREATE INDEX IF NOT EXISTS idx_preexisting_items_is_deleted ON preexisting_items(is_deleted);
CREATE INDEX IF NOT EXISTS idx_preexisting_items_sku ON preexisting_items(sku);
CREATE INDEX IF NOT EXISTS idx_preexisting_items_item_name ON preexisting_items(item_name);

-- Add comments for documentation
COMMENT ON TABLE preexisting_items IS 'Store-scoped catalog items for inventory management';
COMMENT ON COLUMN preexisting_items.id IS 'Unique identifier for the preexisting item';
COMMENT ON COLUMN preexisting_items.store_id IS 'Reference to the store this item belongs to';
COMMENT ON COLUMN preexisting_items.item_name IS 'Name of the catalog item';
COMMENT ON COLUMN preexisting_items.category IS 'Category classification for the item';
COMMENT ON COLUMN preexisting_items.default_price IS 'Default price for the item';
COMMENT ON COLUMN preexisting_items.sku IS 'Stock Keeping Unit - unique within store';
COMMENT ON COLUMN preexisting_items.is_deleted IS 'Soft delete flag';
