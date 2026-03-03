package com.xdr.compliance.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xdr.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("compliance_standard")
public class ComplianceStandard extends BaseEntity {
    private String name;
    private String description;
    private String version;
    private String content; // JSON definition of check items
}
