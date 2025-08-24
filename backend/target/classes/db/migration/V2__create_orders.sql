-- Create extension for UUID generation (safe to run multiple times)
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Orders table used by CSV/Grafana queries
CREATE TABLE IF NOT EXISTS orders (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_time TIMESTAMPTZ NOT NULL DEFAULT now(),
  status TEXT NOT NULL,
  amount_cents BIGINT NOT NULL,
  customer_id UUID
);

-- Helpful index for time-based queries
CREATE INDEX IF NOT EXISTS idx_orders_order_time ON orders(order_time);
