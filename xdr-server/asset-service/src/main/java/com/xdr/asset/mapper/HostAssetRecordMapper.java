package com.xdr.asset.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xdr.asset.model.HostAssetRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface HostAssetRecordMapper extends BaseMapper<HostAssetRecord> {

        @Update("UPDATE host_asset_record SET status = 'INACTIVE' WHERE agent_id = #{agentId} AND asset_type = #{assetType} AND status = 'ACTIVE' AND last_updated < #{threshold}")
        int markInactiveBefore(@Param("agentId") String agentId, @Param("assetType") String assetType,
                        @Param("threshold") LocalDateTime threshold);

        @Update("DELETE FROM host_asset_record WHERE status = 'INACTIVE' AND last_updated < #{threshold}")
        int deleteInactiveBefore(@Param("threshold") LocalDateTime threshold);

}
