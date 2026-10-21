CREATE TABLE customers (
    id          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name        VARCHAR(200) NOT NULL,
    email       VARCHAR(320) NOT NULL,
    phone       VARCHAR(30),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uk_customers_email UNIQUE (email)
);

-- "order" is a reserved word in SQL, hence the plural table name
CREATE TABLE orders (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_id             BIGINT        NOT NULL,
    status                  VARCHAR(20)   NOT NULL,
    order_date              DATE          NOT NULL,
    promised_delivery_date  DATE          NOT NULL,
    delivered_at            TIMESTAMPTZ,
    total_amount            NUMERIC(12,2) NOT NULL,
    created_at              TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT fk_orders_customer FOREIGN KEY (customer_id) REFERENCES customers (id),
    CONSTRAINT ck_orders_status
        CHECK (status IN ('PENDING', 'CONFIRMED', 'SHIPPED', 'DELIVERED', 'CANCELLED')),
    CONSTRAINT ck_orders_total_non_negative CHECK (total_amount >= 0),
    CONSTRAINT ck_orders_promised_after_order CHECK (promised_delivery_date >= order_date),
    CONSTRAINT ck_orders_delivered_consistency
        CHECK ((status = 'DELIVERED') = (delivered_at IS NOT NULL))
);

-- PostgreSQL does not index foreign keys automatically
CREATE INDEX ix_orders_customer_id ON orders (customer_id);

-- Partial index for the "late orders" query: only open orders are indexed
CREATE INDEX ix_orders_open_promised_date ON orders (promised_delivery_date)
    WHERE status NOT IN ('DELIVERED', 'CANCELLED');