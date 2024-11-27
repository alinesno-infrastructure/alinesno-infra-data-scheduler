package com.alinesno.infra.data.scheduler.api.controller;

import com.alinesno.infra.common.core.constants.SpringInstanceScope;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionQuery;
import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.common.web.adapter.rest.SuperController;
import com.alinesno.infra.data.scheduler.api.session.CurrentProjectSession;
import com.alinesno.infra.data.scheduler.entity.ProcessDefinitionEntity;
import com.alinesno.infra.data.scheduler.service.IProcessDefinitionService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 仪表盘控制器
 */
@Slf4j
@Api(tags = "Dashboard")
@RestController
@Scope(SpringInstanceScope.PROTOTYPE)
@RequestMapping("/api/infra/data/scheduler/dashboard")
public class DashboardController extends SuperController {

    @Autowired
    private IProcessDefinitionService processDefinitionService ;

    /**
     * 查询出最近的8条自定义流程
     */
    @DataPermissionQuery
    @GetMapping(value = "/recentlyProcess")
    public AjaxResult recentlyProcess(PermissionQuery query) {
        long projectId = CurrentProjectSession.get().getId() ;
        List<ProcessDefinitionEntity> list = processDefinitionService.queryRecentlyProcess(8 , query , projectId) ;
        return AjaxResult.success(list) ;
    }

}
