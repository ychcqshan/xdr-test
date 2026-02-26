package com.xdr.baseline.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xdr.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("baseline_item")
public class BaselineItem extends BaseEntity {
    private String baselineId;
    private String itemKey;     // 比对键值
    private String itemData;    // JSON格式的基线项数据
}
