-- ─────────────────────────────────────────────────────────────────────────────
-- V2__seed_data.sql
-- WeekendBasket — initial seed data
-- Uses ON CONFLICT DO NOTHING — safe to re-run, never fails on duplicates
-- ─────────────────────────────────────────────────────────────────────────────

-- ── Roles ─────────────────────────────────────────────────────────────────────
INSERT INTO role (role_name, role_id) VALUES
    ('Super Admin', 'ROLE_SUPER_ADMIN'),
    ('Admin',       'ROLE_ADMIN'),
    ('Customer',    'ROLE_CUSTOMER'),
    ('Delivery',    'ROLE_DELIVERY')
ON CONFLICT (role_id) DO NOTHING;

-- ── Master Table ──────────────────────────────────────────────────────────────

-- ORDER_STATUS
INSERT INTO master_table (type, lookup_code, lookup_item, lookup_value) VALUES
    ('ORDER_STATUS', 'PLACED',     'Placed',     '1'),
    ('ORDER_STATUS', 'CONFIRMED',  'Confirmed',  '2'),
    ('ORDER_STATUS', 'PACKED',     'Packed',     '3'),
    ('ORDER_STATUS', 'DELIVERED',  'Delivered',  '4'),
    ('ORDER_STATUS', 'CANCELLED',  'Cancelled',  '5')
ON CONFLICT DO NOTHING;

-- PAYMENT_METHOD
INSERT INTO master_table (type, lookup_code, lookup_item, lookup_value) VALUES
    ('PAYMENT_METHOD', 'COD', 'Cash on Delivery', '1')
ON CONFLICT DO NOTHING;

-- PAYMENT_STATUS
INSERT INTO master_table (type, lookup_code, lookup_item, lookup_value) VALUES
    ('PAYMENT_STATUS', 'PENDING', 'Pending', '1'),
    ('PAYMENT_STATUS', 'PAID',    'Paid',    '2')
ON CONFLICT DO NOTHING;

-- DELIVERY_SLOT
INSERT INTO master_table (type, lookup_code, lookup_item, lookup_value) VALUES
    ('DELIVERY_SLOT', 'SAT', 'Saturday', '1'),
    ('DELIVERY_SLOT', 'SUN', 'Sunday',   '2')
ON CONFLICT DO NOTHING;

-- CYCLE_STATUS
INSERT INTO master_table (type, lookup_code, lookup_item, lookup_value) VALUES
    ('CYCLE_STATUS', 'OPEN',        'Open',        '1'),
    ('CYCLE_STATUS', 'CLOSED',      'Closed',      '2'),
    ('CYCLE_STATUS', 'PROCUREMENT', 'Procurement', '3'),
    ('CYCLE_STATUS', 'DELIVERING',  'Delivering',  '4'),
    ('CYCLE_STATUS', 'COMPLETED',   'Completed',   '5')
ON CONFLICT DO NOTHING;

-- TRANSPORT_STAGE
INSERT INTO master_table (type, lookup_code, lookup_item, lookup_value) VALUES
    ('TRANSPORT_STAGE', 'PROCUREMENT_STARTED', 'Procurement Started', '1'),
    ('TRANSPORT_STAGE', 'GOODS_LOADED',        'Goods Loaded',        '2'),
    ('TRANSPORT_STAGE', 'IN_TRANSIT',          'In Transit',          '3'),
    ('TRANSPORT_STAGE', 'ARRIVED',             'Arrived',             '4'),
    ('TRANSPORT_STAGE', 'PACKING',             'Packing',             '5'),
    ('TRANSPORT_STAGE', 'DISPATCHED',          'Dispatched',          '6')
ON CONFLICT DO NOTHING;

-- NOTIFICATION_TYPE
INSERT INTO master_table (type, lookup_code, lookup_item, lookup_value) VALUES
    ('NOTIFICATION_TYPE', 'ORDER_UPDATE',  'Order Update',  '1'),
    ('NOTIFICATION_TYPE', 'ANNOUNCEMENT',  'Announcement',  '2'),
    ('NOTIFICATION_TYPE', 'OFFER',         'Offer',         '3'),
    ('NOTIFICATION_TYPE', 'REMINDER',      'Reminder',      '4')
ON CONFLICT DO NOTHING;
