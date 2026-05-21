USE xdr_policy;

CREATE TABLE IF NOT EXISTS response_command (
    id VARCHAR(36) PRIMARY KEY,
    agent_id VARCHAR(50) NOT NULL,
    command_type VARCHAR(20) NOT NULL COMMENT 'KILL_PROCESS/DELETE_FILE/ISOLATE',
    command_data JSON NOT NULL COMMENT '指令参数，如 pid 或 filePath',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT 'PENDING/SENT/EXECUTED/FAILED',
    error_message TEXT,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_agent_status (agent_id, status)
);
