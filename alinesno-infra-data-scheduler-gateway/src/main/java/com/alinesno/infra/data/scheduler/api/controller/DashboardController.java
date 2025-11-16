package com.alinesno.infra.data.scheduler.api.controller;

import com.alinesno.infra.common.core.constants.SpringInstanceScope;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionQuery;
import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.common.web.adapter.login.account.CurrentAccountJwt;
import com.alinesno.infra.common.web.adapter.rest.SuperController;
import com.alinesno.infra.data.scheduler.entity.ProcessDefinitionEntity;
import com.alinesno.infra.data.scheduler.service.IProcessDefinitionService;
import com.alinesno.infra.data.scheduler.service.IProcessInstanceService;
import io.swagger.annotations.Api;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    @Autowired
    private IProcessInstanceService processInstanceService;

    /**
     * 查询出最近的8条自定义流程
     */
    @DataPermissionQuery
    @GetMapping(value = "/recentlyProcess")
    public AjaxResult recentlyProcess(PermissionQuery query , Integer limit) {
        if(limit == null){
            limit = 11 ;
        }
        List<ProcessDefinitionEntity> list = processDefinitionService.queryRecentlyProcess(limit , query) ;
        return AjaxResult.success(list) ;
    }

    /**
     * 简单数据统计
     */
    @GetMapping(value = "/simpleStatistics")
    public AjaxResult simpleStatistics() {

        // 统计包括项目总数/总任务/运行中/异常任务/完成任务
        long projectCount = 0 ;
        long taskCount = 0 ;
        long runningTaskCount = 0 ;
        long errorTaskCount = 0 ;
        long completeTaskCount = 0 ;

        long orgId = CurrentAccountJwt.get().getOrgId() ;

        Map<String, Long> statistics = new HashMap<>() ;
        statistics.put("projectCount", projectCount);
        statistics.put("taskCount", taskCount);
        statistics.put("runningTaskCount", runningTaskCount);
        statistics.put("errorTaskCount", errorTaskCount);
        statistics.put("completeTaskCount", completeTaskCount);

        return AjaxResult.success(statistics) ;
    }

}
