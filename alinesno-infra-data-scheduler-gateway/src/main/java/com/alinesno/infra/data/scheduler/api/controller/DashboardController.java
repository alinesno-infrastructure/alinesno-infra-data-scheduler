package com.alinesno.infra.data.scheduler.api.controller;

import com.alinesno.infra.common.core.constants.SpringInstanceScope;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionQuery;
import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.common.web.adapter.login.account.CurrentAccountJwt;
import com.alinesno.infra.common.web.adapter.rest.SuperController;
//import com.alinesno.infra.data.scheduler.api.session.CurrentProjectSession;
import com.alinesno.infra.data.scheduler.entity.ProcessDefinitionEntity;
import com.alinesno.infra.data.scheduler.entity.ProcessInstanceEntity;
//import com.alinesno.infra.data.scheduler.entity.ProjectEntity;
import com.alinesno.infra.data.scheduler.enums.ProcessStatusEnums;
import com.alinesno.infra.data.scheduler.service.IProcessDefinitionService;
import com.alinesno.infra.data.scheduler.service.IProcessInstanceService;
//import com.alinesno.infra.data.scheduler.service.IProjectService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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

//    @Autowired
//    private IProjectService projectService; ;

    @Autowired
    private IProcessInstanceService processInstanceService;

    /**
     * 查询出最近的8条自定义流程
     */
    @DataPermissionQuery
    @GetMapping(value = "/recentlyProcess")
    public AjaxResult recentlyProcess(PermissionQuery query) {
//        long projectId = CurrentProjectSession.get().getId() ;
        List<ProcessDefinitionEntity> list = processDefinitionService.queryRecentlyProcess(11 , query , 1L) ;
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

//        if(CurrentProjectSession.get() != null){

        long orgId = CurrentAccountJwt.get().getOrgId() ; // CurrentProjectSession.get().getOrgId() ;

//            LambdaQueryWrapper<ProjectEntity> wrapper = new LambdaQueryWrapper<>() ;
//            wrapper.eq(ProjectEntity::getOrgId, orgId);
//            projectCount = projectService.count(wrapper) ;

        LambdaQueryWrapper<ProcessDefinitionEntity> wrapper2 = new LambdaQueryWrapper<>() ;
        wrapper2.eq(ProcessDefinitionEntity::getOrgId, orgId);
        taskCount = processDefinitionService.count(wrapper2) ;

        LambdaQueryWrapper<ProcessInstanceEntity> wrapper3 = new LambdaQueryWrapper<>() ;
        wrapper3.eq(ProcessInstanceEntity::getOrgId, orgId);
        completeTaskCount = processInstanceService.count(wrapper3) ;

        LambdaQueryWrapper<ProcessInstanceEntity> wrapper4 = new LambdaQueryWrapper<>() ;
        wrapper4.eq(ProcessInstanceEntity::getState, ProcessStatusEnums.FAIL.getCode());
        wrapper4.eq(ProcessInstanceEntity::getOrgId, orgId);
        errorTaskCount = processInstanceService.count(wrapper4) ;

        LambdaQueryWrapper<ProcessInstanceEntity> wrapper5 = new LambdaQueryWrapper<>() ;
        wrapper5.eq(ProcessInstanceEntity::getState, ProcessStatusEnums.RUNNING.getCode());
        wrapper5.eq(ProcessInstanceEntity::getOrgId, orgId);
        runningTaskCount = processInstanceService.count(wrapper5) ;

//        }

        Map<String, Long> statistics = new HashMap<>() ;
        statistics.put("projectCount", projectCount);
        statistics.put("taskCount", taskCount);
        statistics.put("runningTaskCount", runningTaskCount);
        statistics.put("errorTaskCount", errorTaskCount);
        statistics.put("completeTaskCount", completeTaskCount);

        return AjaxResult.success(statistics) ;
    }

}
