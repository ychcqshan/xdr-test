package com.xdr.threat.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xdr.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("`event`")
public class Event extends BaseEntity {
    private String agentId;
    private String eventType; // PROCESS/NETWORK/USB/LOGIN/ASSET/BASELINE_DIFF
    private String eventData; // JSON
    private String priority; // CRITICAL/HIGH/LOW
    private Integer processed; // 0-未处理 1-已处理
}
