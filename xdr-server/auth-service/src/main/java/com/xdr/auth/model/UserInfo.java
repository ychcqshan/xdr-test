package com.xdr.auth.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xdr.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_info")
public class UserInfo extends BaseEntity {
    private String loginName;
    private String password;
    private String realName;
    private String role; // ADMIN / AUDITOR / OPERATOR
    private String unitLevel1;
    private String unitLevel2;
    private String unitLevel3;
    private String unitLevel4;
    private String department;
    private String post;
    private String phone;
    private String email;
    private Integer status; // 0-禁用 1-正常
}
