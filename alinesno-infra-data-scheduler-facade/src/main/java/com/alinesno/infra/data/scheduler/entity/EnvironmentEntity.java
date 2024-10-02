package com.alinesno.infra.data.scheduler.entity;

import com.alinesno.infra.common.facade.mapper.entity.InfraBaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gitee.sunchenbin.mybatis.actable.annotation.Column;
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
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 32)
    @ColumnComment("环境类型,windows|mac|linux")
    private String systemEnv ;

    @TableField
    @ColumnType(value = MySqlTypeConstant.BIGINT, length = 32)
    @ColumnComment("环境编码")
    private long code;

    @TableField
    @ColumnType(value = MySqlTypeConstant.BIGINT, length = 32)
    @ColumnComment("流程编码")
    private long processId ;

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

    @TableField
    @Column(name = "credential_scope", comment = "范围")
    @ColumnType(length = 16)
    private String credentialScope ;

    // 判断是否是windows/mac/linux类型
    public boolean isWindows() {
        return "windows".equalsIgnoreCase(systemEnv);
    }

    public boolean isMac() {
        return "mac".equalsIgnoreCase(systemEnv);
    }

    public boolean isLinux() {
        return "linux".equalsIgnoreCase(systemEnv);
    }

}
