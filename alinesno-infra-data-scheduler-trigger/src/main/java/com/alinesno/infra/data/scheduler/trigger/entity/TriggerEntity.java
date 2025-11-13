package com.alinesno.infra.data.scheduler.trigger.entity;

import com.alinesno.infra.common.facade.mapper.entity.InfraBaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gitee.sunchenbin.mybatis.actable.annotation.Column;
import com.gitee.sunchenbin.mybatis.actable.annotation.Table;
import com.gitee.sunchenbin.mybatis.actable.annotation.Unique;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Trigger 实体：保存 cron 表达式（支持 H(...)）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("trigger_config")
@Table(name = "trigger_config")
public class TriggerEntity extends InfraBaseEntity {

    @TableField
    @Column(name = "job_id" , comment = "任务ID")
    private Long jobId;

    @TableField
    @Unique
    @Column(name = "process_id" , comment = "流程ID")
    private Long processId;

    @TableField
    @Column(name = "cron" , comment = "cron表达式")
    private String cron;

}