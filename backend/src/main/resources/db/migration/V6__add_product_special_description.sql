-- V6: Add special marketing description to product
ALTER TABLE product ADD COLUMN IF NOT EXISTS special_description TEXT;
