-- =============================================
-- XDR平台数据库初始化脚本
-- =============================================

-- 认证服务数据库
CREATE DATABASE IF NOT EXISTS xdr_auth DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE xdr_auth;

CREATE TABLE IF NOT EXISTS sys_user (
    id VARCHAR(36) PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    real_name VARCHAR(50),
    email VARCHAR(100),
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'OPERATOR' COMMENT 'ADMIN/AUDITOR/OPERATOR',
    status INT NOT NULL DEFAULT 1 COMMENT '0-禁用 1-正常',
    deleted INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 默认管理员 (密码: admin123)
INSERT INTO sys_user (id, username, password, real_name, role, status, deleted)
VALUES (UUID(), 'admin', '$2a$10$N.ZOn9MHSbEU0Oq6lZXaF.kXBqb0h.VDd7jqDeYRcP1XlVLYN9Cku', '系统管理员', 'ADMIN', 1, 0);

-- =============================================
-- 资产服务数据库
CREATE DATABASE IF NOT EXISTS xdr_asset DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE xdr_asset;

CREATE TABLE IF NOT EXISTS asset (
    id VARCHAR(36) PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL UNIQUE COMMENT 'Agent唯一标识',
    hostname VARCHAR(100),
    os_type VARCHAR(20) COMMENT 'WINDOWS/KYLIN/UOS/EULER',
    os_version VARCHAR(50),
    cpu_arch VARCHAR(20) COMMENT 'x86_64/ARM64/LoongArch',
    cpu_model VARCHAR(100),
    memory_total BIGINT COMMENT '内存总量(字节)',
    disk_total BIGINT COMMENT '磁盘总量(字节)',
    mac_address VARCHAR(50),
    ip_address VARCHAR(50),
    agent_version VARCHAR(20),
    status VARCHAR(20) NOT NULL DEFAULT 'OFFLINE' COMMENT 'ONLINE/OFFLINE/UPGRADING',
    group_id VARCHAR(36),
    department VARCHAR(100),
    risk_score INT DEFAULT 0 COMMENT '0-100风险评分',
    last_heartbeat DATETIME,
    deleted INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_agent_id (agent_id),
    INDEX idx_status (status),
    INDEX idx_group_id (group_id)
);

CREATE TABLE IF NOT EXISTS asset_group (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    parent_id VARCHAR(36) COMMENT '父分组ID，NULL表示顶级',
    description VARCHAR(255),
    deleted INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS user_info (
    id VARCHAR(36) PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL UNIQUE,
    real_name VARCHAR(50),
    department VARCHAR(100),
    organization VARCHAR(100),
    phone VARCHAR(20),
    email VARCHAR(100),
    deleted INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_agent_id (agent_id)
);

-- =============================================
-- 基线服务数据库
CREATE DATABASE IF NOT EXISTS xdr_baseline DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE xdr_baseline;

CREATE TABLE IF NOT EXISTS baseline (
    id VARCHAR(36) PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL COMMENT 'PROCESS/PORT/USB/LOGIN/SOFTWARE',
    status VARCHAR(20) NOT NULL DEFAULT 'LEARNING' COMMENT 'LEARNING/PENDING_REVIEW/ACTIVE/INACTIVE',
    version INT NOT NULL DEFAULT 1,
    learning_start DATETIME,
    learning_end DATETIME,
    learning_duration_hours INT COMMENT '学习时长(小时)',
    deleted INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE INDEX idx_agent_type (agent_id, type),
    INDEX idx_status (status)
);

CREATE TABLE IF NOT EXISTS baseline_item (
    id VARCHAR(36) PRIMARY KEY,
    baseline_id VARCHAR(36) NOT NULL,
    item_key VARCHAR(255) NOT NULL COMMENT '比对键值',
    item_data JSON NOT NULL COMMENT '基线项完整数据',
    deleted INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_baseline_id (baseline_id)
);

-- =============================================
-- 威胁服务数据库
CREATE DATABASE IF NOT EXISTS xdr_threat DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE xdr_threat;

CREATE TABLE IF NOT EXISTS alert (
    id VARCHAR(36) PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    level VARCHAR(20) NOT NULL COMMENT 'CRITICAL/HIGH/MEDIUM/LOW',
    status VARCHAR(20) NOT NULL DEFAULT 'NEW' COMMENT 'NEW/ACKNOWLEDGED/RESOLVED/IGNORED',
    threat_type VARCHAR(50) COMMENT 'RANSOMWARE/LATERAL_MOVEMENT/FILELESS/BASELINE_VIOLATION',
    title VARCHAR(200),
    description TEXT,
    raw_event JSON COMMENT '原始事件数据',
    attack_chain_id VARCHAR(36),
    priority INT DEFAULT 0 COMMENT '优先级',
    resolved_at DATETIME,
    resolved_by VARCHAR(50),
    resolve_comment TEXT,
    deleted INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_agent_id (agent_id),
    INDEX idx_level (level),
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
);

CREATE TABLE IF NOT EXISTS event (
    id VARCHAR(36) PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    event_type VARCHAR(50) NOT NULL COMMENT 'PROCESS/NETWORK/USB/LOGIN/ASSET/BASELINE_DIFF',
    event_data JSON NOT NULL,
    priority VARCHAR(20) DEFAULT 'LOW' COMMENT 'CRITICAL/HIGH/LOW',
    processed INT DEFAULT 0 COMMENT '0-未处理 1-已处理',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_agent_id (agent_id),
    INDEX idx_event_type (event_type),
    INDEX idx_created_at (created_at)
);
