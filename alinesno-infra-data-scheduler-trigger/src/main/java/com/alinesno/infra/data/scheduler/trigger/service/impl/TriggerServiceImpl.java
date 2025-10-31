package com.alinesno.infra.data.scheduler.trigger.service.impl;

import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.data.scheduler.trigger.entity.TriggerEntity;
import com.alinesno.infra.data.scheduler.trigger.mapper.TriggerMapper;
import com.alinesno.infra.data.scheduler.trigger.service.ITriggerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * TriggerService
 */
@Slf4j
@Service
public class TriggerServiceImpl extends IBaseServiceImpl<TriggerEntity , TriggerMapper> implements ITriggerService {

}