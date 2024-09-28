package com.alinesno.infra.data.scheduler.service;

import com.alinesno.infra.common.facade.services.IBaseService;
import com.alinesno.infra.data.scheduler.api.LogReadResultDto;
import com.alinesno.infra.data.scheduler.entity.ProcessInstanceEntity;

/**
 */
public interface IProcessInstanceService extends IBaseService<ProcessInstanceEntity> {

    /**
     * 读取日志
     * @param processInstanceId
     * @return
     */
    LogReadResultDto readLog(long processInstanceId , String start);

}
