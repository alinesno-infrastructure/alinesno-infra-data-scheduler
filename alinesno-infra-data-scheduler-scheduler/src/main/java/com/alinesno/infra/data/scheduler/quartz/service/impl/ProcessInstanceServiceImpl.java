package com.alinesno.infra.data.scheduler.quartz.service.impl;

import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.data.scheduler.entity.ProcessInstanceEntity;
import com.alinesno.infra.data.scheduler.quartz.mapper.ProcessInstanceMapper;
import com.alinesno.infra.data.scheduler.service.IProcessInstanceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ProcessInstanceServiceImpl extends IBaseServiceImpl<ProcessInstanceEntity , ProcessInstanceMapper> implements IProcessInstanceService {

}
