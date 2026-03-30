-- =============================================================================
-- Sentinel – MySQL initialization script
-- Runs once on first container boot via /docker-entrypoint-initdb.d/
-- =============================================================================

-- Create both application schemas
CREATE DATABASE IF NOT EXISTS mydb
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

CREATE DATABASE IF NOT EXISTS scan_results
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

-- Ensure the application user exists before granting privileges.
-- The Docker MYSQL_USER env var creates the user, but that happens
-- after the init script is processed on some versions. Being explicit
-- here prevents 'GRANT' from failing on a non-existent user.
CREATE USER IF NOT EXISTS 'sentinel'@'%' IDENTIFIED BY 'sentinel';

GRANT ALL PRIVILEGES ON mydb.*         TO 'sentinel'@'%';
GRANT ALL PRIVILEGES ON scan_results.* TO 'sentinel'@'%';
FLUSH PRIVILEGES;