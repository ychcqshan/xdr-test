package com.xdr.threat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.xdr.threat.model.Alert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AlertMapper extends BaseMapper<Alert> {

    @Select("SELECT level, COUNT(*) as count FROM `alert` WHERE deleted=0 GROUP BY level")
    List<Map<String, Object>> countByLevel();

    @Select("SELECT status, COUNT(*) as count FROM `alert` WHERE deleted=0 GROUP BY status")
    List<Map<String, Object>> countByStatus();

    @Select("SELECT DATE(created_at) as date, COUNT(*) as count FROM `alert` WHERE deleted=0 AND created_at >= DATE_SUB(NOW(), INTERVAL #{days} DAY) GROUP BY DATE(created_at) ORDER BY date")
    List<Map<String, Object>> countByDay(int days);
}
