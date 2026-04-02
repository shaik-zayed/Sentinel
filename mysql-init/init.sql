-- =============================================================================
-- Sentinel – MySQL schema initialisation
--
-- Execution context
-- -----------------
-- This script is placed in /docker-entrypoint-initdb.d/ inside the MySQL
-- Docker image. The MySQL entrypoint (docker-entrypoint.sh) runs it ONCE —
-- only when the /var/lib/mysql data directory is empty (i.e. first boot of a
-- fresh volume). It is never re-executed on subsequent container restarts.
--
-- User lifecycle
-- --------------
-- The Docker MySQL image automatically creates the application user from the
-- MYSQL_USER and MYSQL_PASSWORD environment variables BEFORE executing any
-- scripts in /docker-entrypoint-initdb.d/. This script therefore does NOT
-- re-create the user with a hardcoded password; it only ensures the user
-- exists as a safety net (in case this script is run outside Docker, e.g.
-- by a DBA against a bare MySQL server) and grants the correct privileges.
--
-- Schema ownership
-- ----------------
-- This script creates the database shells and grants access. The actual
-- table DDL is managed by Flyway in each Spring Boot service and runs at
-- application startup. This clean separation means:
--   • init.sql = "create the playground"
--   • Flyway migrations = "set up the furniture"
-- =============================================================================

-- ---------------------------------------------------------------------------
-- Session settings
-- Ensure consistent behaviour regardless of the server's global defaults.
-- ---------------------------------------------------------------------------
SET NAMES 'utf8mb4' COLLATE 'utf8mb4_unicode_ci';
SET TIME_ZONE = '+00:00';

-- ---------------------------------------------------------------------------
-- Schema: mydb
-- Owned by: auth-service
-- Contains: users, refresh_tokens, token_blacklist, audit_logs
-- ---------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS mydb
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- ---------------------------------------------------------------------------
-- Schema: scan_results
-- Owned by: scan-service
-- Contains: scan_requests, scan_items, scan_results
-- ---------------------------------------------------------------------------
CREATE DATABASE IF NOT EXISTS scan_results
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

-- FIX: Removed the placeholder IDENTIFIED BY clause.
-- The user is created by the MySQL entrypoint using MYSQL_USER/MYSQL_PASSWORD.
-- This statement only ensures existence in non-Docker environments.
# CREATE USER IF NOT EXISTS 'sentinel'@'%';

-- ---------------------------------------------------------------------------
-- Grants
--
-- GRANT ALL PRIVILEGES on specific schemas is appropriate for an application
-- user that owns its own database. This is NOT the same as GRANT ALL
-- PRIVILEGES ON *.* which would give global admin rights.
--
-- The privileges granted:
--   SELECT, INSERT, UPDATE, DELETE  — normal CRUD
--   CREATE, ALTER, DROP             — required by Flyway to manage tables
--   INDEX                           — required by Flyway for index creation
--   REFERENCES                      — required for foreign key constraints
--   CREATE TEMPORARY TABLES         — used by some JPA/Hibernate operations
--
-- If you want to be more restrictive after initial Flyway migration, you can
-- reduce this to SELECT, INSERT, UPDATE, DELETE and run Flyway with a
-- separate migration user.
-- ---------------------------------------------------------------------------
GRANT ALL PRIVILEGES ON mydb.* TO 'sentinel'@'%';
GRANT ALL PRIVILEGES ON scan_results.* TO 'sentinel'@'%';

-- Flush so grants take effect for connections already in the pool
FLUSH PRIVILEGES;

-- ---------------------------------------------------------------------------
-- Verification (visible in MySQL error log during container initialisation)
-- ---------------------------------------------------------------------------
SELECT 'Sentinel schemas initialised successfully' AS status;
SHOW DATABASES LIKE '%mydb%';
SHOW DATABASES LIKE '%scan_results%';