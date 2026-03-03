package com.xdr.asset.dto;

import com.xdr.asset.model.Asset;
import com.xdr.asset.model.UserInfo;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class AssetDetailDTO {
    private Asset baseInfo;
    private UserInfo userInfo;

    // 细粒度资产快照
    private List<Map<String, Object>> processes;
    private List<Map<String, Object>> ports;
    private List<Map<String, Object>> softwares;
    private List<Map<String, Object>> usbDevices;
    private List<Map<String, Object>> logins;
}
