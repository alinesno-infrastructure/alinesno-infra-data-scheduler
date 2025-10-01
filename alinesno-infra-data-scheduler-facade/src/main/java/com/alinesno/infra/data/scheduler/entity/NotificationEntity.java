// NotificationEntity.java
package com.alinesno.infra.data.scheduler.entity;

import com.alinesno.infra.common.facade.mapper.entity.InfraBaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gitee.sunchenbin.mybatis.actable.annotation.ColumnComment;
import com.gitee.sunchenbin.mybatis.actable.annotation.ColumnType;
import com.gitee.sunchenbin.mybatis.actable.annotation.Table;
import com.gitee.sunchenbin.mybatis.actable.constants.MySqlTypeConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 通知记录实体类（notification_record）
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("notification_record")
@Table(comment = "通知发送记录表")
public class NotificationEntity extends InfraBaseEntity {

    @TableField("config_id")
    @ColumnComment("通知配置 id")
    @ColumnType(value = MySqlTypeConstant.BIGINT)
    private Long configId;

    @TableField("provider")
    @ColumnComment("提供者类型：WECHAT_ROBOT / DINGTALK / EMAIL")
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 32)
    private String provider; // WECHAT_ROBOT / DINGTALK / EMAIL

    @TableField("title")
    @ColumnComment("通知标题/简要描述")
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 256)
    private String title;

    @TableField("content")
    @ColumnComment("通知内容（正文）")
    @ColumnType(value = MySqlTypeConstant.TEXT)
    private String content;

    @TableField("tos")
    @ColumnComment("接收者，逗号分隔（邮件为邮箱列表，机器人可为 @ 用户列表）")
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 1024)
    private String tos; // 逗号分隔

    @TableField("status")
    @ColumnComment("发送状态：PENDING / SUCCESS / FAILED")
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 32)
    private String status; // PENDING/SUCCESS/FAILED

    @TableField("response")
    @ColumnComment("第三方响应原文（建议截断/脱敏存储）")
    @ColumnType(value = MySqlTypeConstant.LONGTEXT)
    private String response; // 原始响应

    @TableField("error_msg")
    @ColumnComment("错误信息/失败原因")
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 1024)
    private String errorMsg;
}