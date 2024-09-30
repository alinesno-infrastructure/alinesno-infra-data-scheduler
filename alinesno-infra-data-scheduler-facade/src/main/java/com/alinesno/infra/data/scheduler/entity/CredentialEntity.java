package com.alinesno.infra.data.scheduler.entity;

import com.alinesno.infra.common.facade.mapper.entity.InfraBaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gitee.sunchenbin.mybatis.actable.annotation.Column;
import com.gitee.sunchenbin.mybatis.actable.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * CredentialEntity凭据实体信息
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("credential")
@Table(comment = "操作日志记录")
public class CredentialEntity extends InfraBaseEntity {

    @TableField
    @Column(name = "credential_id", comment = "凭据标识")
    private String credentialId;

    @TableField
    @Column(name = "project_id", comment = "项目标识")
    private long projectId ; // 所属项目

    @TableField
    @Column(name = "username", comment = "用户名")
    private String username;

    @TableField
    @Column(name = "password", comment = "密码")
    private String password; // 注意：在实际应用中应考虑对密码进行加密存储

    @TableField
    @Column(name = "type", comment = "类型")
    private String type;

    @TableField
    @Column(name = "description", comment = "描述")
    private String description;


}