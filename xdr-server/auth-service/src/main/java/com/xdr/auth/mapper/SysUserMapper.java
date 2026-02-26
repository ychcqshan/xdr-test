package com.xdr.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xdr.auth.model.SysUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
}
