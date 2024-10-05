package com.alinesno.infra.data.scheduler.service.impl;

import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.data.scheduler.entity.EnvironmentEntity;
import com.alinesno.infra.data.scheduler.mapper.EnvironmentMapper;
import com.alinesno.infra.data.scheduler.service.IEnvironmentService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

    /**
     * 获取默认环境
     *
     * @return 默认的EnvironmentEntity对象
     */
    @Override
    public EnvironmentEntity getDefaultEnv() {
        // 创建查询包装器，用于查询默认环境
        LambdaQueryWrapper<EnvironmentEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(EnvironmentEntity::isDefaultEnv, true);
        // 返回查询到的默认环境实体
        return getOne(queryWrapper);
    }

    /**
     * 保存环境实体，并在没有其他环境实体时将其设置为默认环境
     *
     * @param e 要保存的EnvironmentEntity对象
     */
    @Override
    public void saveEnv(EnvironmentEntity e) {
        // 保存环境实体
        save(e) ;
        // 获取当前环境实体总数
        long count = count() ;
        // 如果只有一个环境实体，将其设置为默认环境
        if(count == 1){
            e.setDefaultEnv(true);
            // 更新环境实体信息
            update(e) ;
        }
    }
}
