-- =============================================
-- XDR平台审计修复脚本 (Remediation)
-- =============================================

-- 1. 修复 xdr_threat.alert 表
USE xdr_threat;
ALTER TABLE alert ADD COLUMN response_status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/EXECUTED/FAILED/CANCELED' AFTER resolve_comment;
ALTER TABLE alert ADD COLUMN response_action VARCHAR(50) COMMENT 'ISOLATE/TERMINATE_PROCESS/DELETE_FILE' AFTER response_status;

-- 2. 补全策略服务数据库
CREATE DATABASE IF NOT EXISTS xdr_policy DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE xdr_policy;

CREATE TABLE IF NOT EXISTS policy (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    scope VARCHAR(20) NOT NULL COMMENT 'GLOBAL/GROUP/AGENT',
    target_id VARCHAR(36) COMMENT 'groupId or agentId',
    content JSON NOT NULL COMMENT '策略详情',
    version INT DEFAULT 1,
    status INT DEFAULT 1 COMMENT '0-失效 1-生效',
    deleted INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 3. 补全升级服务数据库
CREATE DATABASE IF NOT EXISTS xdr_upgrade DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE xdr_upgrade;

CREATE TABLE IF NOT EXISTS upgrade_package (
    id VARCHAR(36) PRIMARY KEY,
    version VARCHAR(20) NOT NULL,
    platform VARCHAR(20) NOT NULL,
    arch VARCHAR(20) NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    checksum VARCHAR(64),
    file_size BIGINT,
    release_notes TEXT,
    deleted INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS upgrade_task (
    id VARCHAR(36) PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    package_id VARCHAR(36) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/DOWNLOADING/INSTALLING/SUCCESS/FAILED',
    error_message TEXT,
    progress INT DEFAULT 0,
    deleted INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_agent_id (agent_id)
);

-- 4. 补全合规服务数据库
CREATE DATABASE IF NOT EXISTS xdr_compliance DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE xdr_compliance;

CREATE TABLE IF NOT EXISTS compliance_standard (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    version VARCHAR(20),
    content JSON COMMENT '检查项定义',
    deleted INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS compliance_result (
    id VARCHAR(36) PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    standard_id VARCHAR(36) NOT NULL,
    pass_rate INT,
    details JSON COMMENT '检查项结论',
    deleted INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_agent_standard (agent_id, standard_id)
);

-- 5. 补全资产类目表 (用于 EXTRA 类型上报)
USE xdr_asset;

CREATE TABLE IF NOT EXISTS asset_software (
    id VARCHAR(36) PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    name VARCHAR(200) NOT NULL,
    version VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_agent_id (agent_id)
);

CREATE TABLE IF NOT EXISTS asset_usb_device (
    id VARCHAR(36) PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    raw_info TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_agent_id (agent_id)
);

CREATE TABLE IF NOT EXISTS asset_login_log (
    id VARCHAR(36) PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    user_name VARCHAR(100),
    terminal VARCHAR(50),
    host VARCHAR(50),
    login_time DATETIME,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_agent_id (agent_id)
);
