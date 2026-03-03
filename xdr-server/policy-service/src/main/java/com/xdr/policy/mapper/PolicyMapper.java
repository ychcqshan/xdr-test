package com.xdr.policy.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xdr.policy.model.Policy;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PolicyMapper extends BaseMapper<Policy> {
}
