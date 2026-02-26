package com.xdr.asset.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xdr.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_info")
public class UserInfo extends BaseEntity {
    private String agentId;
    private String realName;
    private String department;
    private String organization;
    private String phone;
    private String email;
}
