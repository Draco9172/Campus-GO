-- migrate:up

CREATE TABLE users (
    id            BIGSERIAL PRIMARY KEY,
    name          TEXT        NOT NULL,
    email         TEXT        NOT NULL UNIQUE,
    password_hash TEXT        NOT NULL,
    role          TEXT        NOT NULL CHECK (role IN ('STUDENT', 'VENDOR', 'TEACHER')),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE students (
    usn     TEXT   PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id)
);

CREATE TABLE vendors (
    id      BIGSERIAL PRIMARY KEY,
    user_id BIGINT  NOT NULL UNIQUE REFERENCES users(id),
    name    TEXT    NOT NULL UNIQUE,
    is_open BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE menu_items (
    id           BIGSERIAL PRIMARY KEY,
    vendor_id    BIGINT  NOT NULL REFERENCES vendors(id),
    name         TEXT    NOT NULL,
    description  TEXT,
    category     TEXT,
    price_paise  INTEGER NOT NULL CHECK (price_paise >= 0),
    is_available BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE orders (
    id               BIGSERIAL PRIMARY KEY,
    student_id       BIGINT      NOT NULL REFERENCES users(id),
    vendor_id        BIGINT      NOT NULL REFERENCES vendors(id),
    status           TEXT        NOT NULL DEFAULT 'PLACED'
                     CHECK (status IN ('PLACED', 'ACCEPTED', 'PAID', 'READY',
                                       'COLLECTED', 'REJECTED', 'CANCELLED')),
    total_paise      INTEGER     NOT NULL CHECK (total_paise >= 0),
    customer_notes   TEXT,
    rejection_reason TEXT,
    placed_at        TIMESTAMPTZ NOT NULL DEFAULT now(),
    accepted_at      TIMESTAMPTZ,
    paid_at          TIMESTAMPTZ,
    ready_at         TIMESTAMPTZ,
    collected_at     TIMESTAMPTZ,
    cancelled_at     TIMESTAMPTZ
);

CREATE TABLE order_items (
    id               BIGSERIAL PRIMARY KEY,
    order_id         BIGINT  NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    menu_item_id     BIGINT  REFERENCES menu_items(id) ON DELETE SET NULL,
    item_name        TEXT    NOT NULL,
    unit_price_paise INTEGER NOT NULL,
    quantity         INTEGER NOT NULL CHECK (quantity > 0)
);

CREATE INDEX idx_orders_vendor_status  ON orders (vendor_id, status);
CREATE INDEX idx_orders_student_status ON orders (student_id, status);
CREATE INDEX idx_menu_items_vendor     ON menu_items (vendor_id);

-- migrate:down

DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS menu_items;
DROP TABLE IF EXISTS vendors;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS users;
