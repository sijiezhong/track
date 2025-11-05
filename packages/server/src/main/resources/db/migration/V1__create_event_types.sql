-- 事件类型枚举表
-- 注意：ID 值必须与客户端和服务端枚举值保持一致
CREATE TABLE event_types (
    id SMALLINT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description TEXT
);

-- 预置事件类型
-- 注意：ID 值必须与客户端 EventType 枚举和服务端 EventType 枚举完全一致
INSERT INTO event_types (id, name, description) VALUES
(1, 'page_view', '页面浏览'),      -- 对应 EventType.PAGE_VIEW = 1
(2, 'click', '点击事件'),          -- 对应 EventType.CLICK = 2
(3, 'performance', '性能指标'),    -- 对应 EventType.PERFORMANCE = 3
(4, 'error', '错误监控'),          -- 对应 EventType.ERROR = 4
(5, 'custom', '自定义事件'),       -- 对应 EventType.CUSTOM = 5
(6, 'page_stay', '页面停留');      -- 对应 EventType.PAGE_STAY = 6

