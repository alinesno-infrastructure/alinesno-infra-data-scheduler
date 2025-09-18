package com.alinesno.infra.data.scheduler.llm.mapper;

import com.alinesno.infra.common.facade.mapper.repository.IBaseMapper;
import com.alinesno.infra.data.scheduler.llm.entity.LlmModelEntity;
import org.apache.ibatis.annotations.Mapper;

/**
 * 应用构建Mapper接口
 * 
 * @version 1.0.0
 * @author luoxiaodong
 */
@Mapper
public interface LlmModelMapper extends IBaseMapper<LlmModelEntity> {

}