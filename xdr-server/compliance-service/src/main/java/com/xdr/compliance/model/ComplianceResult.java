package com.xdr.compliance.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xdr.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("compliance_result")
public class ComplianceResult extends BaseEntity {
    private String agentId;
    private String standardId;
    private String status; // PASS, FAIL, WARNING
    private String details; // JSON results of individual check items
    private Integer score;
}
