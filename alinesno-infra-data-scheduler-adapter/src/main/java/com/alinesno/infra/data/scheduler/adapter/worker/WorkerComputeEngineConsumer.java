package com.alinesno.infra.data.scheduler.adapter.worker;

import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.facade.response.R;
import com.alinesno.infra.data.scheduler.api.ComputeEngineConfigDto;
import com.alinesno.infra.data.scheduler.api.ProbeResultDto;
import com.alinesno.infra.data.scheduler.entity.ComputeEngineEntity;
import com.dtflys.forest.annotation.*;

/**
 * ComputeEngineConsumer
 */
@BaseRequest(baseURL = "#{alinesno.data.scheduler.worker-node}/api/infra/data/scheduler/computeEngine" , readTimeout= 3600_000)
public interface WorkerComputeEngineConsumer {

    /**
     * 获取当前配置
     * @param query
     * @return
     */
    @Post("/getConfig")
    R<ComputeEngineEntity> getCurrentConfig(@JSONBody PermissionQuery query);


    /**
     * 保存配置
     * @param entity
     */
    @Post("/saveConfig")
    R<Boolean> saveOrUpdateConfig(@JSONBody ComputeEngineConfigDto entity);

    /**
     * 测试引擎健康
     * @param engineAddress
     * @param apiToken
     * @return
     */
    @Get("/probeHealth")
    R<ProbeResultDto> probeEngineHealth(@Query("engineAddress") String engineAddress, @Query("apiToken") String apiToken);
}
