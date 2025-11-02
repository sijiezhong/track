-- users 表唯一索引已在 DDL 定义，补充保险
CREATE UNIQUE INDEX IF NOT EXISTS ux_users_username ON users (username);

-- session 表索引
CREATE UNIQUE INDEX IF NOT EXISTS ux_session_session_id ON session (session_id);
CREATE INDEX IF NOT EXISTS ix_session_tenant_id ON session (tenant_id);
CREATE INDEX IF NOT EXISTS ix_session_user_id ON session (user_id);

-- application 表索引
CREATE UNIQUE INDEX IF NOT EXISTS ux_application_app_key ON application (app_key);
CREATE INDEX IF NOT EXISTS ix_application_tenant_id ON application (tenant_id);

-- event 表索引
CREATE INDEX IF NOT EXISTS ix_event_event_time ON event (event_time);
CREATE INDEX IF NOT EXISTS ix_event_tenant_id ON event (tenant_id);
CREATE INDEX IF NOT EXISTS ix_event_event_name ON event (event_name);
CREATE INDEX IF NOT EXISTS ix_event_session_id ON event (session_id);
