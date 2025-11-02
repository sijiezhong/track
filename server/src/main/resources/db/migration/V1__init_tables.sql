-- 用户表（避免保留字，使用 users）
CREATE TABLE IF NOT EXISTS users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password VARCHAR(256) NOT NULL,
    real_name VARCHAR(64),
    email VARCHAR(128),
    phone VARCHAR(32),
    is_anonymous BOOLEAN NOT NULL DEFAULT FALSE,
    tenant_id INTEGER,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 会话表
CREATE TABLE IF NOT EXISTS session (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL UNIQUE,
    user_id INTEGER,
    user_agent VARCHAR(255),
    ip VARCHAR(64),
    tenant_id INTEGER,
    start_time TIMESTAMP NOT NULL DEFAULT NOW(),
    end_time TIMESTAMP NOT NULL DEFAULT NOW(),
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 应用表
CREATE TABLE IF NOT EXISTS application (
    id SERIAL PRIMARY KEY,
    app_key VARCHAR(64) NOT NULL UNIQUE,
    app_name VARCHAR(128) NOT NULL,
    owner_id INTEGER,
    tenant_id INTEGER,
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 事件表
CREATE TABLE IF NOT EXISTS event (
    id BIGSERIAL PRIMARY KEY,
    event_name VARCHAR(64) NOT NULL,
    user_id INTEGER,
    session_id BIGINT,
    properties TEXT NOT NULL,
    tenant_id INTEGER,
    event_time TIMESTAMP NOT NULL DEFAULT NOW(),
    create_time TIMESTAMP NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP NOT NULL DEFAULT NOW()
);
