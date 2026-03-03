package com.xdr.threat.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xdr.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("host_asset")
public class HostAsset extends BaseEntity {
    private String agentId;
    private String assetType; // PROCESS, NETWORK
    private String assetId; // Unique key (e.g., pid|name|createTime)
    private String assetData; // Full JSON details
}
