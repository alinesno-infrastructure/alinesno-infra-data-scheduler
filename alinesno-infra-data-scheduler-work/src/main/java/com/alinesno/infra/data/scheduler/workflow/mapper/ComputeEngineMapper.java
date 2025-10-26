package com.alinesno.infra.data.scheduler.workflow.mapper;

import com.alinesno.infra.common.facade.mapper.repository.IBaseMapper;
import com.alinesno.infra.data.scheduler.entity.ComputeEngineEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * AI 分布式计算引擎配置 Mapper 接口
 *
 * @version 1.0.0
 * @author luoxiaodong
 */
@Mapper
public interface ComputeEngineMapper extends IBaseMapper<ComputeEngineEntity> {
    // 如需自定义 SQL 方法，可在此处添加
}