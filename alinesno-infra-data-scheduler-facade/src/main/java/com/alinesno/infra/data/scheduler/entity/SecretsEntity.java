package com.alinesno.infra.data.scheduler.entity;

import com.alinesno.infra.common.facade.mapper.entity.InfraBaseEntity;
import com.alinesno.infra.common.security.mapper.AESEncryptHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gitee.sunchenbin.mybatis.actable.annotation.Column;
import com.gitee.sunchenbin.mybatis.actable.annotation.ColumnComment;
import com.gitee.sunchenbin.mybatis.actable.annotation.ColumnType;
import com.gitee.sunchenbin.mybatis.actable.annotation.TableComment;
import com.gitee.sunchenbin.mybatis.actable.constants.MySqlTypeConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 密钥管理
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value ="task_secrets" , autoResultMap = true)
@TableComment(value = "密钥定义表")
public class SecretsEntity extends InfraBaseEntity {

    @TableField
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 128)
    @ColumnComment("密钥名称")
    private String secName;

    @TableField(typeHandler = AESEncryptHandler.class)
    @ColumnType(value = MySqlTypeConstant.VARCHAR , length = 512)
    @ColumnComment("密钥值")
    private String secValue;

    @TableField
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 128)
    @ColumnComment("描述")
    private String secDesc;

    @TableField
    @Column(name = "sec_scope", comment = "范围")
    @ColumnType(length = 16)
    private String secScope;
}
