CREATE INDEX IF NOT EXISTS idx_restaurant_chain_id ON restaurant (chain_id);
CREATE INDEX IF NOT EXISTS idx_menu_item_restaurant_id ON menu_item (restaurant_id);
CREATE INDEX IF NOT EXISTS idx_category_restaurant_id ON category (restaurant_id);
CREATE INDEX IF NOT EXISTS idx_orders_restaurant_last_updated ON orders (restaurant_id, last_updated_time);

-- Optional supporting indexes for tenant-heavy list filters
CREATE INDEX IF NOT EXISTS idx_tax_rate_restaurant_id ON tax_rate (restaurant_id);
CREATE INDEX IF NOT EXISTS idx_sale_item_restaurant_id ON sale_item (restaurant_id);
CREATE INDEX IF NOT EXISTS idx_restaurant_table_restaurant_id ON restaurant_table (restaurant_id);
CREATE INDEX IF NOT EXISTS idx_item_stock_restaurant_id ON item_stock (restaurant_id);
