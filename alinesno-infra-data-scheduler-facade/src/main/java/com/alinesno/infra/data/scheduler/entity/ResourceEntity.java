package com.alinesno.infra.data.scheduler.entity;

import com.alinesno.infra.common.facade.mapper.entity.InfraBaseEntity;
import com.alinesno.infra.data.scheduler.enums.ResourceTypeEnums;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gitee.sunchenbin.mybatis.actable.annotation.ColumnComment;
import com.gitee.sunchenbin.mybatis.actable.annotation.ColumnType;
import com.gitee.sunchenbin.mybatis.actable.annotation.TableComment;
import com.gitee.sunchenbin.mybatis.actable.constants.MySqlTypeConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("resources")
@TableComment(value = "任务资源定义表")
public class ResourceEntity extends InfraBaseEntity {

    @TableField
    @ColumnComment("父级ID")
    @ColumnType(value = MySqlTypeConstant.INT)
    private long pid;

    @TableField
    @ColumnComment("资源别名")
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 128)
    private String alias;

    @TableField
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 128)
    @ColumnComment("资源描述")
    private String description;

    @TableField
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 128)
    @ColumnComment("文件名")
    private String fileName;

    @TableField
    @ColumnComment("是否为目录")
    private boolean isDirectory = false;

    @TableField
    @ColumnComment("云存储ID")
    @ColumnType(value = MySqlTypeConstant.BIGINT, length = 32)
    private long storageId ;

    @TableField
    @ColumnComment("资源类型")
    @ColumnType(value = MySqlTypeConstant.INT)
    private ResourceTypeEnums type;

    @TableField
    @ColumnComment("资源大小")
    @ColumnType(value = MySqlTypeConstant.INT)
    private long size;

}