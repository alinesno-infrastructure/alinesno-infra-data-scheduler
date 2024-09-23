package com.alinesno.infra.data.scheduler.quartz.service.impl;

import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.data.scheduler.entity.TaskInstanceEntity;
import com.alinesno.infra.data.scheduler.quartz.mapper.TaskInstanceMapper;
import com.alinesno.infra.data.scheduler.service.ITaskInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class TaskInstanceServiceImpl extends IBaseServiceImpl<TaskInstanceEntity , TaskInstanceMapper> implements ITaskInstanceService {

}
