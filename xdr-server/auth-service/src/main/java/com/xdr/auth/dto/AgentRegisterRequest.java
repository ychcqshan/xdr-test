package com.xdr.auth.dto;

import lombok.Data;

@Data
public class AgentRegisterRequest {
    private String hostname;
    private String osType;       // WINDOWS / KYLIN / UOS / EULER
    private String cpuArch;      // x86_64 / ARM64 / LoongArch
    private String macAddress;
    private String agentVersion;
}
