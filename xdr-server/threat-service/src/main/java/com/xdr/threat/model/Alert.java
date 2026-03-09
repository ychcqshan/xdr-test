package com.xdr.threat.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xdr.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("`alert`")
public class Alert extends BaseEntity {
    private String agentId;
    private String level; // CRITICAL / HIGH / MEDIUM / LOW
    private String status; // NEW / ACKNOWLEDGED / RESOLVED / IGNORED
    private String threatType; // RANSOMWARE / LATERAL_MOVEMENT / FILELESS / BASELINE_VIOLATION
    private String title;
    private String description;
    private String rawEvent; // JSON
    private String attackChainId;
    private Integer priority;
    private LocalDateTime resolvedAt;
    private String resolvedBy;
    private String resolveComment;
    private String responseStatus; // PENDING / EXECUTED / FAILED / CANCELED
    private String responseAction; // ISOLATE / TERMINATE_PROCESS / DELETE_FILE
    private String unit;
    private String responsiblePerson;
}
