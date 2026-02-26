package com.xdr.auth.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xdr.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class SysUser extends BaseEntity {

    private String username;
    private String password;
    private String realName;
    private String email;
    private String phone;
    private String role; // ADMIN / AUDITOR / OPERATOR
    private Integer status; // 0-禁用 1-正常
}
