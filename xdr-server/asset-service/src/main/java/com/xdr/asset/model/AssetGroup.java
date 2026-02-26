package com.xdr.asset.model;

import com.baomidou.mybatisplus.annotation.TableName;
import com.xdr.common.model.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("asset_group")
public class AssetGroup extends BaseEntity {
    private String name;
    private String parentId;
    private String description;
}
