-- 事件数据表
-- 注意：app_id、user_id、user_properties 从 Session 中获取，不再从请求体获取
CREATE TABLE events (
    id BIGSERIAL PRIMARY KEY,
    app_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(128) NOT NULL,
    user_properties JSONB,
    event_type_id SMALLINT NOT NULL REFERENCES event_types(id),
    custom_event_id VARCHAR(128),  -- 自定义事件唯一标识符
    properties JSONB,              -- 事件属性
    dom_path TEXT,                 -- DOM路径（点击事件）
    page_url TEXT,
    page_title TEXT,
    referrer TEXT,
    user_agent TEXT,
    ip_address INET,
    server_timestamp TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 创建索引
CREATE INDEX idx_events_app_user ON events(app_id, user_id);
CREATE INDEX idx_events_timestamp ON events(server_timestamp);
CREATE INDEX idx_events_type ON events(event_type_id);
CREATE INDEX idx_events_custom_id ON events(custom_event_id) WHERE custom_event_id IS NOT NULL;
CREATE INDEX idx_events_app_timestamp ON events(app_id, server_timestamp);
CREATE INDEX idx_events_app_type_timestamp ON events(app_id, event_type_id, server_timestamp);

