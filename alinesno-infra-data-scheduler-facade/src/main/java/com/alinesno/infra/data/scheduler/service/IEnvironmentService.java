package com.alinesno.infra.data.scheduler.service;

import com.alinesno.infra.common.facade.services.IBaseService;
import com.alinesno.infra.data.scheduler.entity.EnvironmentEntity;

public interface IEnvironmentService extends IBaseService<EnvironmentEntity> {

    /**
     * 设置默认环境
     * @param id
     */
    void defaultEnv(Long id);
}
