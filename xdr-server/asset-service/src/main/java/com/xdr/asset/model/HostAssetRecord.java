package com.xdr.asset.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName(value = "host_asset_record", autoResultMap = true)
public class HostAssetRecord {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String agentId;
    private String assetType;
    private String assetFingerprint;
    // We use String for simplicity, or we can use Jackson mapping
    private String assetData;
    private String status;
    private LocalDateTime firstSeen;
    private LocalDateTime lastUpdated;
}
