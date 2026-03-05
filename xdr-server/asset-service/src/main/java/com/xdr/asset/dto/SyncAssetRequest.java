package com.xdr.asset.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class SyncAssetRequest {
    private String agentId;
    private String assetType;
    private String reportType;
    private List<Map<String, Object>> items;
}
