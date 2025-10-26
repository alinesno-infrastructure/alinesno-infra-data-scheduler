package com.alinesno.infra.data.scheduler.api.worker;

import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionQuery;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionSave;
import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.common.facade.response.R;
import com.alinesno.infra.data.scheduler.adapter.worker.ComputeEngineConsumer;
import com.alinesno.infra.data.scheduler.api.ComputeEngineConfigDto;
import com.alinesno.infra.data.scheduler.api.ProbeResultDto;
import com.alinesno.infra.data.scheduler.entity.ComputeEngineEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/infra/data/scheduler/computeEngine")
@Validated
public class WorkflowComputeEngineController  {

    @Autowired
    private ComputeEngineConsumer service;

    /**
     * 获取当前配置
     * @param query
     * @return
     */
    @DataPermissionQuery
    @GetMapping("/getConfig")
    public AjaxResult getConfig(PermissionQuery  query) {

        R<ComputeEngineEntity> computeEngineEntity = service.getCurrentConfig(query);
        ComputeEngineConfigDto computeEngineConfigDto = ComputeEngineConfigDto.fromEntity(computeEngineEntity.getData());

        return AjaxResult.success(computeEngineConfigDto);
    }

    /**
     * 保存配置
     * @param config
     * @return
     */
    @DataPermissionSave
    @PostMapping("/saveConfig")
    public AjaxResult saveConfig(@Valid @RequestBody ComputeEngineConfigDto config) {
        R<Boolean> result = service.saveOrUpdateConfig(ComputeEngineConfigDto.toEntity(config));
        return AjaxResult.success("保存成功");
    }

    // 添加到原有的 ComputeEngineController
    @GetMapping("/probeHealth")
    public AjaxResult probeHealth(
            @RequestParam("engineAddress") String engineAddress,
            @RequestParam(value = "adminUser", required = false) String adminUser) {

        R<ProbeResultDto> result = service.probeEngineHealth(engineAddress, adminUser);
        return AjaxResult.success(result);
    }

}