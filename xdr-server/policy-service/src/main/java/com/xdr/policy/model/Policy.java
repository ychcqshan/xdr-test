package com.xdr.policy.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xdr.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("policy")
public class Policy extends BaseEntity {
    private String name;
    private String scope; // GLOBAL, GROUP, AGENT
    private String targetId; // groupId or agentId (null for GLOBAL)
    private String content; // JSON string of policy details
    private Integer version;
    private Integer status; // 0: INACTIVE, 1: ACTIVE
}
