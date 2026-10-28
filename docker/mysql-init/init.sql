-- Runs ONCE when the MySQL container is created for the first time.
-- One MySQL server, THREE separate databases: each service owns its own data (database-per-service).
-- (In real production each service would normally get its own MySQL server.)
CREATE DATABASE IF NOT EXISTS order_db;
CREATE DATABASE IF NOT EXISTS payment_db;
CREATE DATABASE IF NOT EXISTS inventory_db;
