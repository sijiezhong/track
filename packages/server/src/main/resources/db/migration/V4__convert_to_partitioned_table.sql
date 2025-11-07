-- 将 events 表转换为分区表（HASH 分区，按 app_id 分成 8 个分区）
-- 注意：此脚本会删除所有现有数据，历史数据为测试数据，无需备份

-- 1. 删除现有的 events 表（包括所有数据和索引）
DROP TABLE IF EXISTS events CASCADE;

-- 2. 创建分区表主表（使用 HASH 分区，按 app_id 分成 8 个分区）
CREATE TABLE events (
    id BIGSERIAL,
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
    created_at TIMESTAMPTZ DEFAULT NOW(),
    PRIMARY KEY (id, app_id)  -- 分区键必须包含在主键中
) PARTITION BY HASH (app_id);

-- 3. 创建 8 个哈希分区
CREATE TABLE events_p0 PARTITION OF events
    FOR VALUES WITH (modulus 8, remainder 0);

CREATE TABLE events_p1 PARTITION OF events
    FOR VALUES WITH (modulus 8, remainder 1);

CREATE TABLE events_p2 PARTITION OF events
    FOR VALUES WITH (modulus 8, remainder 2);

CREATE TABLE events_p3 PARTITION OF events
    FOR VALUES WITH (modulus 8, remainder 3);

CREATE TABLE events_p4 PARTITION OF events
    FOR VALUES WITH (modulus 8, remainder 4);

CREATE TABLE events_p5 PARTITION OF events
    FOR VALUES WITH (modulus 8, remainder 5);

CREATE TABLE events_p6 PARTITION OF events
    FOR VALUES WITH (modulus 8, remainder 6);

CREATE TABLE events_p7 PARTITION OF events
    FOR VALUES WITH (modulus 8, remainder 7);

-- 4. 创建索引（在分区表上创建，会自动应用到所有分区）
CREATE INDEX idx_events_app_user ON events(app_id, user_id);
CREATE INDEX idx_events_timestamp ON events(server_timestamp);
CREATE INDEX idx_events_type ON events(event_type_id);
CREATE INDEX idx_events_custom_id ON events(custom_event_id) WHERE custom_event_id IS NOT NULL;
CREATE INDEX idx_events_app_timestamp ON events(app_id, server_timestamp);
CREATE INDEX idx_events_app_type_timestamp ON events(app_id, event_type_id, server_timestamp);
