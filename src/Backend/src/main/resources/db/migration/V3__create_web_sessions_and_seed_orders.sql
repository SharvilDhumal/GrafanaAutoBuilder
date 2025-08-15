-- Ensure pgcrypto for UUIDs
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Create web_sessions table if missing
CREATE TABLE IF NOT EXISTS web_sessions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  started_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  user_id UUID,
  device TEXT
);
CREATE INDEX IF NOT EXISTS idx_web_sessions_started_at ON web_sessions(started_at);

-- Seed orders with sample data for last 30 days
DO $$
DECLARE
  d INTEGER;
  per_day_orders INTEGER;
  i INTEGER;
  base_time TIMESTAMPTZ;
  st TEXT;
BEGIN
  FOR d IN 0..29 LOOP
    base_time := now() - (d || ' days')::interval;
    -- between 20 and 60 orders per day
    per_day_orders := 20 + floor(random()*41);
    FOR i IN 1..per_day_orders LOOP
      -- status distribution: paid(70%), shipped(20%), refunded(5%), pending(5%)
      st := CASE
              WHEN random() < 0.70 THEN 'paid'
              WHEN random() < 0.90 THEN 'shipped'
              WHEN random() < 0.95 THEN 'refunded'
              ELSE 'pending'
            END;
      INSERT INTO orders (order_time, status, amount_cents, customer_id)
      VALUES (
        base_time + (random() * interval '24 hours'),
        st,
        -- amounts between $10 and $300
        (1000 + floor(random() * 29000))::bigint,
        gen_random_uuid()
      );
    END LOOP;
  END LOOP;
END $$;

-- Seed web_sessions roughly proportional to orders
DO $$
DECLARE
  d INTEGER;
  sessions_cnt INTEGER;
  i INTEGER;
  base_time TIMESTAMPTZ;
BEGIN
  FOR d IN 0..29 LOOP
    base_time := now() - (d || ' days')::interval;
    sessions_cnt := 50 + floor(random()*151); -- 50-200 sessions per day
    FOR i IN 1..sessions_cnt LOOP
      INSERT INTO web_sessions (started_at, user_id, device)
      VALUES (
        base_time + (random() * interval '24 hours'),
        gen_random_uuid(),
        (ARRAY['mobile','desktop','tablet'])[1 + floor(random()*3)]
      );
    END LOOP;
  END LOOP;
END $$;
