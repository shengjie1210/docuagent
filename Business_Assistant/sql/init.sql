CREATE DATABASE IF NOT EXISTS docuagent DEFAULT CHARSET utf8mb4;
USE docuagent;

CREATE TABLE IF NOT EXISTS user (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    username    VARCHAR(50) NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    is_admin    TINYINT DEFAULT 0 NOT NULL COMMENT '是否管理员：1=管理员',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 把 id=1 的用户设为管理员（首次初始化时）
UPDATE user SET is_admin = 1 WHERE id = 1;

CREATE TABLE IF NOT EXISTS document (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL COMMENT '所属用户',
    filename    VARCHAR(255) NOT NULL COMMENT '原始文件名',
    file_type   VARCHAR(20) NOT NULL COMMENT 'pdf/docx/md/txt',
    file_path   VARCHAR(500) NOT NULL COMMENT '存储相对路径',
    size        BIGINT DEFAULT 0 COMMENT '文件大小(字节)',
    summary     TEXT COMMENT 'AI生成的摘要',
    tags        VARCHAR(255) COMMENT '标签,逗号分隔',
    chunk_count INT DEFAULT 0 COMMENT '切分块数',
    status      VARCHAR(20) DEFAULT 'ready' COMMENT 'processing/ready/failed',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_filename (filename)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 对话表
CREATE TABLE IF NOT EXISTS conversation (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL,
    title       VARCHAR(255),
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 消息表
CREATE TABLE IF NOT EXISTS message (
    id              BIGINT PRIMARY KEY AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    role            VARCHAR(20) NOT NULL,
    content         TEXT NOT NULL,
    sources         TEXT,
    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conv (conversation_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- AI 服务商配置表
CREATE TABLE IF NOT EXISTS ai_provider_config (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     BIGINT NOT NULL COMMENT '哪个用户的配置',
    provider    VARCHAR(50) NOT NULL COMMENT 'dashscope/openai/ollama',
    name        VARCHAR(100) NOT NULL COMMENT '配置名称，如"通义千问"',
    base_url    VARCHAR(500) NOT NULL,
    api_key     VARCHAR(500) COMMENT 'API Key（Ollama可空）',
    chat_model  VARCHAR(100) NOT NULL,
    embed_model VARCHAR(100),
    is_default  TINYINT DEFAULT 0 COMMENT '是否当前启用',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
