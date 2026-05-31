-- V5: Add admin-curated rating to product
ALTER TABLE product ADD COLUMN IF NOT EXISTS rating DECIMAL(2,1) DEFAULT 0.0;
