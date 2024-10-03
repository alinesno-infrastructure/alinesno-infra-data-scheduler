package com.alinesno.infra.data.scheduler.service.impl;

import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.data.scheduler.entity.EnvironmentEntity;
import com.alinesno.infra.data.scheduler.mapper.EnvironmentMapper;
import com.alinesno.infra.data.scheduler.service.IEnvironmentService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 【请填写功能名称】Service业务层处理
 *
 * @version 1.0.0
 * @autor luoxiaodong
 */
@Slf4j
@Service
public class EnvironmentServiceImpl extends IBaseServiceImpl<EnvironmentEntity, EnvironmentMapper> implements IEnvironmentService {
    @Override
    public void defaultEnv(Long id) {

        // 当前环境配置成默认的，非当前id配置成非默认的
        EnvironmentEntity environment = baseMapper.selectById(id);
        environment.setDefaultEnv(true);
        baseMapper.updateById(environment);

        // 其它非当前id的环境配置成非默认的
        LambdaUpdateWrapper<EnvironmentEntity> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper
                .ne(EnvironmentEntity::getId, id)
                .set(EnvironmentEntity::isDefaultEnv, false);

        update(updateWrapper) ;

    }
}
