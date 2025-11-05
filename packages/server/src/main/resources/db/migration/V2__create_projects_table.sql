-- 项目表
CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    app_id VARCHAR(64) UNIQUE NOT NULL,
    app_name VARCHAR(128) NOT NULL,
    description TEXT,
    created_by BIGINT,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- 创建索引
CREATE INDEX idx_projects_app_id ON projects(app_id);
CREATE INDEX idx_projects_active ON projects(is_active);

