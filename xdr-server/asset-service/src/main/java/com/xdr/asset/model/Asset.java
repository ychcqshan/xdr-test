package com.xdr.asset.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xdr.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset")
public class Asset extends BaseEntity {

    private String agentId;
    private String hostname;
    private String osType;
    private String osVersion;
    private String cpuArch;
    private String cpuModel;
    private Long memoryTotal;
    private Long diskTotal;
    private String macAddress;
    private String ipAddress;
    private String agentVersion;
    private String status; // ONLINE / OFFLINE / UPGRADING
    private String groupId;
    private String department;
    private String unit;
    private String responsiblePerson;
    private Integer riskScore;
    private LocalDateTime lastHeartbeat;
}
