package com.xdr.upgrade.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xdr.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("upgrade_task")
public class UpgradeTask extends BaseEntity {
    private String agentId;
    private String packageId;
    private String status; // PENDING, DOWNLOADING, INSTALLING, SUCCESS, FAILED
    private String errorMessage;
    private Integer progress; // 0-100
}
