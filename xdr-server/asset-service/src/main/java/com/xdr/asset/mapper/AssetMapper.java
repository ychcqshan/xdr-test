package com.xdr.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xdr.asset.model.Asset;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AssetMapper extends BaseMapper<Asset> {
}
