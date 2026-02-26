package com.xdr.baseline.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xdr.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("baseline")
public class Baseline extends BaseEntity {
    private String agentId;
    private String type;            // PROCESS/PORT/USB/LOGIN/SOFTWARE
    private String status;          // LEARNING/PENDING_REVIEW/ACTIVE/INACTIVE
    private Integer version;
    private LocalDateTime learningStart;
    private LocalDateTime learningEnd;
    private Integer learningDurationHours;
}
