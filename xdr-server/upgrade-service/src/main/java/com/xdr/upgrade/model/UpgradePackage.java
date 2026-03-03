package com.xdr.upgrade.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xdr.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("upgrade_package")
public class UpgradePackage extends BaseEntity {
    private String version;
    private String platform; // windows, linux, darwin
    private String arch; // x86_64, arm64
    private String fileName;
    private String checksum; // SHA-256
    private Long fileSize;
    private String releaseNotes;
}
