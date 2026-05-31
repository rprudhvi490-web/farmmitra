-- V4: Cycle-level product stock limits
-- Admin sets max_stock per product before each cycle opens.
-- ordered_qty is maintained incrementally on each order placement / cancellation.
CREATE TABLE cycle_product (
    id           BIGSERIAL PRIMARY KEY,
    cycle_id     BIGINT        NOT NULL REFERENCES weekly_cycle(id),
    product_id   BIGINT        NOT NULL REFERENCES product(id),
    max_stock    DECIMAL(10,2) NOT NULL,
    ordered_qty  DECIMAL(10,2) NOT NULL DEFAULT 0,
    sold_out     BOOLEAN       NOT NULL DEFAULT false,
    created_by   VARCHAR(100),
    created_on   TIMESTAMP,
    updated_on   TIMESTAMP,
    updated_by   VARCHAR(100),
    CONSTRAINT uq_cycle_product UNIQUE (cycle_id, product_id)
);
