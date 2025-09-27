package com.alinesno.infra.data.scheduler.service;

import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.facade.services.IBaseService;
import com.alinesno.infra.data.scheduler.api.ProbeResultDto;
import com.alinesno.infra.data.scheduler.entity.ComputeEngineEntity;

/**
 * AI 分布式计算引擎配置 Service 接口
 *
 * @version 1.0.0
 * @author luoxiaodong
 */
public interface IComputeEngineService extends IBaseService<ComputeEngineEntity> {

    /**
     * 获取当前配置（若按项目需要可扩展为按 id 获取或按键获取）
     * @return 当前配置实体或 null
     */
    ComputeEngineEntity getCurrentConfig(PermissionQuery permissionQuery);

    /**
     * 保存或更新配置（根据 id 或其他规则）
     * @param entity 实体
     * @return 保存结果
     */
    boolean saveOrUpdateConfig(ComputeEngineEntity entity);

    /**
     * 测试引擎健康
     * @param engineAddress
     * @param adminUser
     * @return
     */
    ProbeResultDto probeEngineHealth(String engineAddress, String adminUser);

    /**
     * 获取当前配置
     * @param orgId
     */
    ComputeEngineEntity getCurrentConfig(Long orgId);
}