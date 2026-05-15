-- ─────────────────────────────────────────────────────────────────────────────
-- V1__init_schema.sql
-- WeekendBasket — full schema
-- All constraints mirror Spring/JPA annotations exactly
-- ─────────────────────────────────────────────────────────────────────────────

-- ── app_user ─────────────────────────────────────────────────────────────────
CREATE TABLE app_user (
    id             BIGSERIAL PRIMARY KEY,
    phone_number   VARCHAR(15)  NOT NULL UNIQUE,
    password       VARCHAR(255) NOT NULL,
    username       VARCHAR(100) NOT NULL DEFAULT 'guest',
    referral_code  VARCHAR(20)  UNIQUE,
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_by     VARCHAR(100),
    created_on     TIMESTAMP,
    updated_on     TIMESTAMP,
    updated_by     VARCHAR(100)
);

-- ── user_profile ──────────────────────────────────────────────────────────────
CREATE TABLE user_profile (
    id           BIGSERIAL PRIMARY KEY,
    first_name   VARCHAR(100),
    last_name    VARCHAR(100),
    email        VARCHAR(150),
    flat_number  VARCHAR(50),
    block        VARCHAR(50),
    community_id BIGINT,
    user_id      BIGINT NOT NULL REFERENCES app_user(id),
    created_by   VARCHAR(100),
    created_on   TIMESTAMP,
    updated_on   TIMESTAMP,
    updated_by   VARCHAR(100)
);

-- ── community ─────────────────────────────────────────────────────────────────
CREATE TABLE community (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(200) NOT NULL,
    city       VARCHAR(100),
    address    TEXT,
    active     BOOLEAN NOT NULL DEFAULT TRUE,
    created_by VARCHAR(100),
    created_on TIMESTAMP,
    updated_on TIMESTAMP,
    updated_by VARCHAR(100)
);

-- ── role ──────────────────────────────────────────────────────────────────────
CREATE TABLE role (
    id         BIGSERIAL PRIMARY KEY,
    role_name  VARCHAR(100) NOT NULL,
    role_id    VARCHAR(50)  NOT NULL UNIQUE,
    created_by VARCHAR(100),
    created_on TIMESTAMP,
    updated_on TIMESTAMP,
    updated_by VARCHAR(100)
);

-- ── role_access ───────────────────────────────────────────────────────────────
CREATE TABLE role_access (
    id         BIGSERIAL PRIMARY KEY,
    role_id    BIGINT NOT NULL REFERENCES role(id),
    user_id    BIGINT NOT NULL REFERENCES app_user(id),
    created_by VARCHAR(100),
    created_on TIMESTAMP,
    updated_on TIMESTAMP,
    updated_by VARCHAR(100),
    CONSTRAINT uq_role_access_user_role UNIQUE (user_id, role_id)
);

-- ── otp_verification ──────────────────────────────────────────────────────────
CREATE TABLE otp_verification (
    id           BIGSERIAL PRIMARY KEY,
    phone_number VARCHAR(15)  NOT NULL,
    otp_code     VARCHAR(10)  NOT NULL,
    expires_at   TIMESTAMP    NOT NULL,
    attempts     INT          NOT NULL DEFAULT 0,
    verified     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_on   TIMESTAMP    NOT NULL
);

-- ── invalidated_token ─────────────────────────────────────────────────────────
CREATE TABLE invalidated_token (
    id             BIGSERIAL PRIMARY KEY,
    token          TEXT      NOT NULL,
    invalidated_at TIMESTAMP NOT NULL
);

-- ── category ──────────────────────────────────────────────────────────────────
CREATE TABLE category (
    id            BIGSERIAL PRIMARY KEY,
    name          VARCHAR(100) NOT NULL,
    image_url     VARCHAR(500),
    display_order INT,
    active        BOOLEAN NOT NULL DEFAULT TRUE,
    created_by    VARCHAR(100),
    created_on    TIMESTAMP,
    updated_on    TIMESTAMP,
    updated_by    VARCHAR(100)
);

-- ── product ───────────────────────────────────────────────────────────────────
CREATE TABLE product (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(200)   NOT NULL,
    description     TEXT,
    category_id     BIGINT         NOT NULL REFERENCES category(id),
    unit            VARCHAR(50),
    price_per_unit  DECIMAL(10,2),
    image_url       VARCHAR(500),
    available       BOOLEAN        NOT NULL DEFAULT TRUE,
    min_order_qty   DECIMAL(6,2),
    created_by      VARCHAR(100),
    created_on      TIMESTAMP,
    updated_on      TIMESTAMP,
    updated_by      VARCHAR(100)
);

-- ── weekly_cycle ──────────────────────────────────────────────────────────────
CREATE TABLE weekly_cycle (
    id                BIGSERIAL PRIMARY KEY,
    cycle_label       VARCHAR(100) NOT NULL,
    order_open_at     TIMESTAMP    NOT NULL,
    order_close_at    TIMESTAMP    NOT NULL,
    delivery_date_sat DATE,
    delivery_date_sun DATE,
    status            VARCHAR(30)  NOT NULL DEFAULT 'OPEN',
    created_by        VARCHAR(100),
    created_on        TIMESTAMP,
    updated_on        TIMESTAMP,
    updated_by        VARCHAR(100)
);

-- ── customer_order ────────────────────────────────────────────────────────────
CREATE TABLE customer_order (
    id                BIGSERIAL PRIMARY KEY,
    order_number      VARCHAR(50)    NOT NULL UNIQUE,
    user_id           BIGINT         NOT NULL REFERENCES app_user(id),
    cycle_id          BIGINT         NOT NULL REFERENCES weekly_cycle(id),
    delivery_slot     VARCHAR(10),
    status            VARCHAR(30)    NOT NULL DEFAULT 'PLACED',
    total_amount      DECIMAL(10,2),
    referral_discount DECIMAL(10,2)  NOT NULL DEFAULT 0,
    amount_to_collect DECIMAL(10,2),
    payment_method    VARCHAR(30)    NOT NULL DEFAULT 'COD',
    payment_status    VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    notes             TEXT,
    created_by        VARCHAR(100),
    created_on        TIMESTAMP,
    updated_on        TIMESTAMP,
    updated_by        VARCHAR(100)
);

-- ── order_item ────────────────────────────────────────────────────────────────
CREATE TABLE order_item (
    id          BIGSERIAL PRIMARY KEY,
    order_id    BIGINT         NOT NULL REFERENCES customer_order(id),
    product_id  BIGINT         NOT NULL REFERENCES product(id),
    quantity    DECIMAL(6,2)   NOT NULL,
    unit_price  DECIMAL(10,2)  NOT NULL,
    total_price DECIMAL(10,2)  NOT NULL,
    created_by  VARCHAR(100),
    created_on  TIMESTAMP,
    updated_on  TIMESTAMP,
    updated_by  VARCHAR(100)
);

-- ── procurement_sheet ─────────────────────────────────────────────────────────
CREATE TABLE procurement_sheet (
    id             BIGSERIAL PRIMARY KEY,
    cycle_id       BIGINT        NOT NULL REFERENCES weekly_cycle(id),
    product_id     BIGINT        NOT NULL REFERENCES product(id),
    total_quantity DECIMAL(8,2)  NOT NULL,
    unit           VARCHAR(50),
    vendor_name    VARCHAR(200),
    vendor_notes   TEXT,
    procured_qty   DECIMAL(8,2),
    status         VARCHAR(30)   NOT NULL DEFAULT 'PENDING',
    created_by     VARCHAR(100),
    created_on     TIMESTAMP,
    updated_on     TIMESTAMP,
    updated_by     VARCHAR(100),
    CONSTRAINT uq_procurement_cycle_product UNIQUE (cycle_id, product_id)
);

-- ── transport_tracking ────────────────────────────────────────────────────────
CREATE TABLE transport_tracking (
    id         BIGSERIAL PRIMARY KEY,
    cycle_id   BIGINT       NOT NULL REFERENCES weekly_cycle(id),
    stage      VARCHAR(50)  NOT NULL,
    notes      TEXT,
    updated_by VARCHAR(100),
    created_on TIMESTAMP    NOT NULL
);

-- ── delivery_batch ────────────────────────────────────────────────────────────
CREATE TABLE delivery_batch (
    id            BIGSERIAL PRIMARY KEY,
    cycle_id      BIGINT       NOT NULL REFERENCES weekly_cycle(id),
    batch_label   VARCHAR(100) NOT NULL,
    delivery_date DATE,
    assigned_to   BIGINT       REFERENCES app_user(id),
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    created_by    VARCHAR(100),
    created_on    TIMESTAMP,
    updated_on    TIMESTAMP,
    updated_by    VARCHAR(100)
);

-- ── delivery_batch_order ──────────────────────────────────────────────────────
CREATE TABLE delivery_batch_order (
    id       BIGSERIAL PRIMARY KEY,
    batch_id BIGINT NOT NULL REFERENCES delivery_batch(id),
    order_id BIGINT NOT NULL REFERENCES customer_order(id),
    CONSTRAINT uq_batch_order UNIQUE (batch_id, order_id)
);

-- ── notification ──────────────────────────────────────────────────────────────
CREATE TABLE notification (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT       REFERENCES app_user(id),
    title       VARCHAR(200) NOT NULL,
    body        TEXT         NOT NULL,
    type        VARCHAR(50)  NOT NULL,
    read_status BOOLEAN      NOT NULL DEFAULT FALSE,
    sent_at     TIMESTAMP    NOT NULL,
    created_by  VARCHAR(100),
    created_on  TIMESTAMP,
    updated_on  TIMESTAMP,
    updated_by  VARCHAR(100)
);

-- ── fcm_token ─────────────────────────────────────────────────────────────────
CREATE TABLE fcm_token (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT      NOT NULL REFERENCES app_user(id),
    token       TEXT        NOT NULL,
    device_type VARCHAR(20),
    active      BOOLEAN     NOT NULL DEFAULT TRUE,
    created_by  VARCHAR(100),
    created_on  TIMESTAMP,
    updated_on  TIMESTAMP,
    updated_by  VARCHAR(100)
);

-- ── referral ──────────────────────────────────────────────────────────────────
CREATE TABLE referral (
    id            BIGSERIAL PRIMARY KEY,
    referrer_id   BIGINT      NOT NULL REFERENCES app_user(id),
    referred_id   BIGINT      NOT NULL REFERENCES app_user(id),
    referral_code VARCHAR(20) NOT NULL,
    status        VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_by    VARCHAR(100),
    created_on    TIMESTAMP,
    updated_on    TIMESTAMP,
    updated_by    VARCHAR(100),
    CONSTRAINT uq_referral UNIQUE (referrer_id, referred_id)
);

-- ── master_table ──────────────────────────────────────────────────────────────
CREATE TABLE master_table (
    id           BIGSERIAL PRIMARY KEY,
    lookup_value VARCHAR(10),
    lookup_item  VARCHAR(100) NOT NULL,
    lookup_code  VARCHAR(50)  NOT NULL,
    type         VARCHAR(50)  NOT NULL,
    created_by   VARCHAR(100),
    created_on   TIMESTAMP,
    updated_on   TIMESTAMP,
    updated_by   VARCHAR(100),
    CONSTRAINT uq_master_type_code UNIQUE (type, lookup_code)
);
