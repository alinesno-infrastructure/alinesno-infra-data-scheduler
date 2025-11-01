package com.alinesno.infra.data.scheduler.workflow.controller;

import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.facade.response.R;
import com.alinesno.infra.common.web.adapter.rest.BaseController;
import com.alinesno.infra.data.scheduler.api.ComputeEngineConfigDto;
import com.alinesno.infra.data.scheduler.api.ProbeResultDto;
import com.alinesno.infra.data.scheduler.entity.ComputeEngineEntity;
import com.alinesno.infra.data.scheduler.workflow.service.IComputeEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/infra/data/scheduler/computeEngine")
@Validated
public class ComputeEngineController extends BaseController<ComputeEngineEntity, IComputeEngineService> {

    @Autowired
    private IComputeEngineService service;

    /**
     * 获取当前配置
     *
     * @param query
     * @return
     */
    @PostMapping("/getConfig")
    public R<ComputeEngineEntity> getConfig(@RequestBody PermissionQuery  query) {
        ComputeEngineEntity computeEngineEntity = service.getCurrentConfig(query);
        return R.ok(computeEngineEntity);
    }

    /**
     * 保存配置
     * @param config
     * @return
     */
    @PostMapping("/saveConfig")
    public R<Boolean> saveConfig(@Valid @RequestBody ComputeEngineConfigDto config) {
        service.saveOrUpdateConfig(ComputeEngineConfigDto.toEntity(config));
        return R.ok(true);
    }

    // 添加到原有的 ComputeEngineController
    @GetMapping("/probeHealth")
    public R<ProbeResultDto> probeHealth(
            @RequestParam("engineAddress") String engineAddress,
            @RequestParam(value = "adminUser", required = false) String adminUser) {

        ProbeResultDto result = service.probeEngineHealth(engineAddress, adminUser);
        return R.ok(result);
    }

    @Override
    public IComputeEngineService getFeign() {
        return this.service;
    }
}