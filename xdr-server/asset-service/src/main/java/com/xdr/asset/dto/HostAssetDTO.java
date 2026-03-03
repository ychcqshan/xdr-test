package com.xdr.asset.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class HostAssetDTO {
    private String agentId;
    private String assetType; // PROCESS, NETWORK, SOFTWARE, USB, LOGIN
    private String assetId;
    private String assetData; // JSON string

    // 助手：方便前端直接使用解析后的数据
    private Map<String, Object> details;
}
