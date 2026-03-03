package com.xdr.compliance.controller;

import com.xdr.common.dto.ApiResponse;
import com.xdr.compliance.model.ComplianceResult;
import com.xdr.compliance.model.ComplianceStandard;
import com.xdr.compliance.service.ComplianceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/compliance")
@RequiredArgsConstructor
public class ComplianceController {

    private final ComplianceService complianceService;

    @GetMapping("/standards")
    public ApiResponse<List<ComplianceStandard>> listStandards() {
        return ApiResponse.ok(complianceService.listStandards());
    }

    @PostMapping("/standards")
    public ApiResponse<Void> saveStandard(@RequestBody ComplianceStandard standard) {
        complianceService.saveStandard(standard);
        return ApiResponse.ok();
    }

    /** Agent上报合规检查结果 */
    @PostMapping("/results")
    public ApiResponse<Void> reportResult(@RequestBody ComplianceResult result) {
        complianceService.reportResult(result);
        return ApiResponse.ok();
    }

    /** 查询指定Agent的合规结果 */
    @GetMapping("/results/{agentId}")
    public ApiResponse<List<ComplianceResult>> getResults(@PathVariable String agentId) {
        return ApiResponse.ok(complianceService.getResultsByAgent(agentId));
    }
}
