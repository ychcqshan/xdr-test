package com.xdr.threat.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.xdr.common.dto.ApiResponse;
import com.xdr.threat.mapper.HostAssetMapper;
import com.xdr.threat.model.HostAsset;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/host-assets")
@RequiredArgsConstructor
public class HostAssetController {

    private final HostAssetMapper hostAssetMapper;

    /** 获取指定Agent的所有细粒度资产快照 */
    @GetMapping("/{agentId}")
    public ApiResponse<List<HostAsset>> getHostAssets(@PathVariable String agentId) {
        return ApiResponse.ok(hostAssetMapper.selectList(
                new LambdaQueryWrapper<HostAsset>().eq(HostAsset::getAgentId, agentId)));
    }
}
