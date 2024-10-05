package com.alinesno.infra.data.scheduler.service;

import com.alinesno.infra.common.facade.services.IBaseService;
import com.alinesno.infra.data.scheduler.entity.EnvironmentEntity;

public interface IEnvironmentService extends IBaseService<EnvironmentEntity> {

    /**
     * 设置默认环境
     * @param id
     */
    void defaultEnv(Long id);

    /**
     * 获取默认环境
     * @return
     */
    EnvironmentEntity getDefaultEnv();

    /**
     * 保存环境变量
     * @param e
     */
    void saveEnv(EnvironmentEntity e);
}
