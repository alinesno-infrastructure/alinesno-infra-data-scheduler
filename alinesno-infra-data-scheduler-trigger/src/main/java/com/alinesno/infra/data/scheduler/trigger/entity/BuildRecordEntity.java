package com.alinesno.infra.data.scheduler.trigger.entity;

import com.alinesno.infra.common.facade.mapper.entity.InfraBaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gitee.sunchenbin.mybatis.actable.annotation.Column;
import com.gitee.sunchenbin.mybatis.actable.annotation.Table;
import com.gitee.sunchenbin.mybatis.actable.constants.MySqlTypeConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 构建执行记录（类似 Jenkins 的 Build 记录）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("build_record")
@Table(name = "build_record")
public class BuildRecordEntity extends InfraBaseEntity {

    @TableField
    @Column(name = "job_id", type = MySqlTypeConstant.BIGINT, comment = "所属任务ID")
    private Long jobId;

    @TableField
    @Column(name = "process_id", type = MySqlTypeConstant.BIGINT, comment = "所属进程ID")
    private Long processId ;

    @TableField
    @Column(name = "trigger_id", type = MySqlTypeConstant.BIGINT, isNull = true, comment = "触发器ID（可选）")
    private Long triggerId;

    @TableField
    @Column(name = "build_number", type = MySqlTypeConstant.INT, comment = "job 级别的自增 build number")
    private Integer buildNumber;

    @TableField
    @Column(name = "display_name", type = MySqlTypeConstant.VARCHAR, length = 255, comment = "展示名字，例如 JobName #123")
    private String displayName;

    @TableField
    @Column(name = "status", type = MySqlTypeConstant.VARCHAR, length = 50, comment = "状态：QUEUED RUNNING COMPLETED FAILED CANCELLED")
    private String status;

    @TableField
    @Column(name = "message", type = MySqlTypeConstant.VARCHAR, length = 1000, isNull = true, comment = "日志/消息")
    private String message;

    @TableField
    @Column(name = "enqueued_at", type = MySqlTypeConstant.DATETIME, isNull = true, comment = "入队时间")
    private LocalDateTime enqueuedAt;

    @TableField
    @Column(name = "started_at", type = MySqlTypeConstant.DATETIME, isNull = true, comment = "开始时间")
    private LocalDateTime startedAt;

    @TableField
    @Column(name = "finished_at", type = MySqlTypeConstant.DATETIME, isNull = true, comment = "结束时间")
    private LocalDateTime finishedAt;

    @TableField
    @Column(name = "duration_ms", type = MySqlTypeConstant.BIGINT, isNull = true, comment = "耗时（毫秒）")
    private Long durationMs;

    // 可按需扩展字段，如 artifacts / console_log 等
}