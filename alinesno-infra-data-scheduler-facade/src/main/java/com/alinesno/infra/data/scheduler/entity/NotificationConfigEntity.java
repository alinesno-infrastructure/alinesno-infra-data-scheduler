// NotificationConfigEntity.java
package com.alinesno.infra.data.scheduler.entity;

import com.alinesno.infra.common.facade.mapper.entity.InfraBaseEntity;
import com.alinesno.infra.common.security.mapper.AESEncryptHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gitee.sunchenbin.mybatis.actable.annotation.ColumnComment;
import com.gitee.sunchenbin.mybatis.actable.annotation.ColumnType;
import com.gitee.sunchenbin.mybatis.actable.annotation.Table;
import com.gitee.sunchenbin.mybatis.actable.constants.MySqlTypeConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通用通知配置（机器人/webhook/邮件）
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "notification_config" , autoResultMap = true)
@Table(comment = "通知配置表：机器人 webhook / 邮件 SMTP 等")
public class NotificationConfigEntity extends InfraBaseEntity {

    @TableField("name")
    @ColumnComment("配置名称")
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 128)
    private String name;

    @TableField("provider")
    @ColumnComment("提供者类型：WECHAT_ROBOT / DINGTALK / EMAIL")
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 32)
    private String provider; // WECHAT_ROBOT / DINGTALK / EMAIL

    @TableField(value = "webhook" , typeHandler = AESEncryptHandler.class)
    @ColumnComment("机器人 webhook 地址（企业微信/钉钉等）")
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 1024)
    private String webhook; // 机器人 webhook 地址（webhook 模式）

    @TableField(value = "secret"  , typeHandler = AESEncryptHandler.class)
    @ColumnComment("机器人 secret（签名用，可选）")
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 512)
    private String secret; // 机器人 secret（可选）

    @TableField(value = "access_token" , typeHandler = AESEncryptHandler.class)
    @ColumnComment("访问令牌（可选，备用字段）")
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 512)
    private String accessToken; // 可选备用字段

    // 邮件专用字段：
    @TableField(value = "smtp_host"  , typeHandler = AESEncryptHandler.class)
    @ColumnComment("SMTP 主机地址（邮件发送）")
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 256)
    private String smtpHost;

    @TableField(value = "smtp_port"  , typeHandler = AESEncryptHandler.class)
    @ColumnComment("SMTP 端口（如 25/465/587）")
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 256)
    private String smtpPort;

    @TableField(value = "smtp_username"  , typeHandler = AESEncryptHandler.class)
    @ColumnComment("SMTP 登录用户名")
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 256)
    private String smtpUsername;

    @TableField(value = "smtp_password" , typeHandler = AESEncryptHandler.class)
    @ColumnComment("SMTP 密码（建议加密存储）")
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 512)
    private String smtpPassword;

    @TableField("from_address")
    @ColumnComment("邮件发送人地址（From）")
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 256)
    private String fromAddress;

    @TableField("enabled")
    @ColumnComment("是否启用（1=启用，0=禁用）")
    @ColumnType(value = MySqlTypeConstant.TINYINT, length = 1)
    private Boolean enabled;
}