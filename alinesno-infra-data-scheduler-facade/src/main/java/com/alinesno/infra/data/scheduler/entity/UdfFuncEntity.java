package com.alinesno.infra.data.scheduler.entity;

import com.alinesno.infra.common.facade.mapper.entity.InfraBaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gitee.sunchenbin.mybatis.actable.annotation.ColumnComment;
import com.gitee.sunchenbin.mybatis.actable.annotation.ColumnType;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * udf function
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("udfs")
public class UdfFuncEntity extends InfraBaseEntity {

    @TableField(value = "func_name")
    @ColumnType(length=32)
    @ColumnComment("功能名称")
    private String funcName;

    @TableField(value = "class_name")
    @ColumnType(length=128)
    @ColumnComment("类名")
    private String className;

    @TableField(value = "arg_types")
    @ColumnType(length=128)
    @ColumnComment("参数类型")
    private String argTypes;

    @TableField(value = "database")
    @ColumnType(length=128)
    @ColumnComment("数据库")
    private String database;

    @TableField(value = "description")
    @ColumnType(length=128)
    @ColumnComment("描述")
    private String description;

    @TableField(value = "resource_id")
    @ColumnType(length=32)
    @ColumnComment("资源ID")
    private long resourceId;

    @TableField(value = "resource_name")
    @ColumnType(length=128)
    @ColumnComment("资源名称")
    private String resourceName;


}