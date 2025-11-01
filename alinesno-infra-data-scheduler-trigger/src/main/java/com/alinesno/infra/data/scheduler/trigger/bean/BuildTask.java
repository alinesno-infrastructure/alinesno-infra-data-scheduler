package com.alinesno.infra.data.scheduler.trigger.bean;

import cn.hutool.core.util.IdUtil;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 构建任务实例
 */
@NoArgsConstructor
@Data
public class BuildTask {

    private String id = IdUtil.getSnowflakeNextIdStr() ;
    private Long jobId;
    private Long processId ; // 进程ID
    private String jobName;
    private TaskState state = TaskState.QUEUED;
    private LocalDateTime enqueuedAt = LocalDateTime.now();
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String message;

    private Integer buildNumber;
    private Long recordId;

    public BuildTask(Long jobId, String jobName , Long processId) {
        this.jobId = jobId;
        this.processId = processId ;
        this.jobName = jobName;
    }

    /**
     * 获取任务显示名称
     * @return
     */
    public String getDisplayName() {
        return jobName + " #" + (buildNumber == null ? "?" : buildNumber);
    }
}