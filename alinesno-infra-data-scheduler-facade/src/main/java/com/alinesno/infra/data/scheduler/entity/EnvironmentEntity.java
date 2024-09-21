package com.alinesno.infra.data.scheduler.entity;

import com.alinesno.infra.common.facade.mapper.entity.InfraBaseEntity;
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
@TableName("environment")
@TableComment(value = "环境定义表")
public class EnvironmentEntity extends InfraBaseEntity {

    @TableField
    @ColumnType(value = MySqlTypeConstant.BIGINT, length = 32)
    @ColumnComment("环境编码")
    private long code;

    @TableField
    @ColumnType(value = MySqlTypeConstant.BIGINT, length = 32)
    @ColumnComment("流程编码")
    private long processCode ;

    @TableField
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 128)
    @ColumnComment("环境名称")
    private String name;

    @TableField
    @ColumnType(value = MySqlTypeConstant.LONGTEXT)
    @ColumnComment("环境配置")
    private String config;

    @TableField
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 128)
    @ColumnComment("环境描述")
    private String description;

}
