package com.xdr.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xdr.auth.model.UserInfo;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserInfoMapper extends BaseMapper<UserInfo> {
}
