package com.alinesno.infra.data.scheduler.entity;

import cn.afterturn.easypoi.excel.annotation.Excel;
import com.alinesno.infra.common.facade.mapper.entity.InfraBaseEntity;
import com.alinesno.infra.common.security.mapper.AESEncryptHandler;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gitee.sunchenbin.mybatis.actable.annotation.ColumnComment;
import com.gitee.sunchenbin.mybatis.actable.annotation.ColumnType;
import com.gitee.sunchenbin.mybatis.actable.annotation.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * AI 分布式计算引擎配置 实体
 *
 * @author luoxiaodong
 * @version 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName(value = "infra_compute_engine" , autoResultMap = true)
@Table(comment = "AI分布式计算引擎配置")
public class ComputeEngineEntity extends InfraBaseEntity {

    @Excel(name = "引擎类型")
    @TableField("compute_engine")
    @ColumnType(length = 64)
    @ColumnComment("计算引擎类型：spark/flink/trino")
    private String computeEngine;

    @Excel(name = "引擎地址")
    @TableField(value = "engine_address" , typeHandler = AESEncryptHandler.class)
    @ColumnType(length = 512)
    @ColumnComment("引擎地址（REST/Thrift 等）")
    private String engineAddress;

    @Excel(name = "引擎请求Token")
    @TableField(value = "api_token" , typeHandler = AESEncryptHandler.class)
    @ColumnType(length = 512)
    @ColumnComment("用于请求引擎的管理员用户）")
    private String apiToken;

    @Excel(name = "请求超时时间(秒)")
    @TableField("request_timeout")
    @ColumnType(length = 11)
    @ColumnComment("请求超时时间，单位秒")
    private Integer requestTimeout;

}