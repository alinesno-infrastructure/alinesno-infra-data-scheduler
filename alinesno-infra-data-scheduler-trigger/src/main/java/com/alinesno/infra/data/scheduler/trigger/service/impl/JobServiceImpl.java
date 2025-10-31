package com.alinesno.infra.data.scheduler.trigger.service.impl;

import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.data.scheduler.trigger.entity.JobEntity;
import com.alinesno.infra.data.scheduler.trigger.mapper.JobMapper;
import com.alinesno.infra.data.scheduler.trigger.service.IJobService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * JobServiceImpl
 */
@Slf4j
@Service
public class JobServiceImpl extends IBaseServiceImpl<JobEntity , JobMapper> implements IJobService {

}