package com.xdr.asset.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xdr.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_user")
public class AssetUser extends BaseEntity {
    private String agentId;
    private String username;
    private String unitLevel1;
    private String unitLevel2;
    private String unitLevel3;
    private String unitLevel4;
    private String department;
    private String post;
    private String phone;
    private String email;
}
