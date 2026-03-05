package com.xdr.policy.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xdr.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("response_command")
public class ResponseCommand extends BaseEntity {
    private String agentId;
    private String commandType; // KILL_PROCESS, DELETE_FILE, ISOLATE
    private String commandData; // JSON string
    private String status; // PENDING, SENT, EXECUTED, FAILED
    private String errorMessage;
}
