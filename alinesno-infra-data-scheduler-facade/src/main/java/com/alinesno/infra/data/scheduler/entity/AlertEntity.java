package com.alinesno.infra.data.scheduler.entity;

import com.alinesno.infra.common.facade.mapper.entity.InfraBaseEntity;
import com.alinesno.infra.data.scheduler.enums.AlertStatusEnums;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gitee.sunchenbin.mybatis.actable.annotation.ColumnComment;
import com.gitee.sunchenbin.mybatis.actable.annotation.ColumnType;
import com.gitee.sunchenbin.mybatis.actable.constants.MySqlTypeConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.HashMap;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("alert")
public class AlertEntity extends InfraBaseEntity {

    @TableField(value = "title")
    @ColumnComment("标题")
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 128)
    private String title;

    @TableField(value = "content")
    @ColumnComment("内容")
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 128)
    private String content;

    @TableField(value = "alert_status")
    @ColumnComment("状态")
    @ColumnType(value = MySqlTypeConstant.INT)
    private AlertStatusEnums alertStatus;

    @TableField(value = "log")
    @ColumnComment("日志")
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 128)
    private String log;

    @TableField(exist = false)
    private Map<String, Object> info = new HashMap<>();

}