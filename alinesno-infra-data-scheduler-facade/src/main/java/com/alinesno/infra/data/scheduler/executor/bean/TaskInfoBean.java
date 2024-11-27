package com.alinesno.infra.data.scheduler.executor.bean;

import com.alinesno.infra.data.scheduler.entity.EnvironmentEntity;
import com.alinesno.infra.data.scheduler.entity.ProcessDefinitionEntity;
import com.alinesno.infra.data.scheduler.entity.ResourceEntity;
import com.alinesno.infra.data.scheduler.entity.TaskDefinitionEntity;
import lombok.Data;
import lombok.ToString;

import java.io.Serializable;
import java.util.List;

/**
 * 任务信息
 */
@ToString
@Data
public class TaskInfoBean implements Serializable {

    // 运行的工作空间
    private String workspace ;

    // 本地工程目录
    private String workspacePath ;

    // 环境定义
    private EnvironmentEntity environment ;

    // 任务定义
    private ProcessDefinitionEntity process ;

    // 任务列表
    private TaskDefinitionEntity task;

    // 资源列表
    private List<ResourceEntity> resources;

}
