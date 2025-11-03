-- 数据库重构：将 tenant_id 改为 app_id
-- 清理所有现有数据，并重命名字段

-- 1. 清空所有表数据
TRUNCATE TABLE audit_log CASCADE;
TRUNCATE TABLE webhook_subscription CASCADE;
TRUNCATE TABLE event CASCADE;
TRUNCATE TABLE session CASCADE;
TRUNCATE TABLE application CASCADE;
TRUNCATE TABLE users CASCADE;

-- 2. 重命名字段：users 表
ALTER TABLE users RENAME COLUMN tenant_id TO app_id;

-- 3. 重命名字段：session 表
ALTER TABLE session RENAME COLUMN tenant_id TO app_id;

-- 4. 重命名字段：application 表
ALTER TABLE application RENAME COLUMN tenant_id TO app_id;

-- 5. 重命名字段：event 表
ALTER TABLE event RENAME COLUMN tenant_id TO app_id;

-- 6. 重命名字段：audit_log 表
ALTER TABLE audit_log RENAME COLUMN tenant_id TO app_id;

-- 7. 重命名字段：webhook_subscription 表
ALTER TABLE webhook_subscription RENAME COLUMN tenant_id TO app_id;

-- 8. 更新注释
COMMENT ON COLUMN users.app_id IS '应用ID，用于多应用数据隔离';
COMMENT ON COLUMN session.app_id IS '应用ID';
COMMENT ON COLUMN application.app_id IS '应用ID';
COMMENT ON COLUMN event.app_id IS '应用ID';
COMMENT ON COLUMN audit_log.app_id IS '应用ID';
COMMENT ON COLUMN webhook_subscription.app_id IS '应用ID';

