package com.alinesno.infra.data.scheduler.api.controller;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.common.core.constants.SpringInstanceScope;
import com.alinesno.infra.common.core.utils.StringUtils;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionQuery;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionSave;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionScope;
import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.facade.pageable.DatatablesPageBean;
import com.alinesno.infra.common.facade.pageable.TableDataInfo;
import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.common.web.adapter.rest.BaseController;
import com.alinesno.infra.data.scheduler.api.*;
import com.alinesno.infra.data.scheduler.constants.PipeConstants;
import com.alinesno.infra.data.scheduler.entity.ProcessDefinitionEntity;
import com.alinesno.infra.data.scheduler.service.ICategoryService;
import com.alinesno.infra.data.scheduler.service.IProcessDefinitionService;
import io.swagger.annotations.Api;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.ui.Model;
import org.springframework.util.Assert;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 处理与TransEntity相关的请求的Controller。
 * 继承自BaseController类并实现ITransService接口。
 *
 * @version 1.0.0
 * @author  luoxiaodong
 */
@Slf4j
@Api(tags = "Trans")
@RestController
@Scope(SpringInstanceScope.PROTOTYPE)
@RequestMapping("/api/infra/data/scheduler/processDefinition")
public class ProcessDefinitionController extends BaseController<ProcessDefinitionEntity, IProcessDefinitionService> {

    @Autowired
    private IProcessDefinitionService service;

    @Autowired
    private ICategoryService catalogService ;

    @Autowired
    private Scheduler scheduler ;

    /**
     * 获取TransEntity的DataTables数据。
     *
     * @param request HttpServletRequest对象。
     * @param model Model对象。
     * @param page DatatablesPageBean对象。
     * @return 包含DataTables数据的TableDataInfo对象。
     */
    @DataPermissionScope
    @ResponseBody
    @PostMapping("/datatables")
    public TableDataInfo datatables(HttpServletRequest request, Model model, DatatablesPageBean page) {
        log.debug("page = {}", ToStringBuilder.reflectionToString(page));
        return this.toPage(model, this.getFeign(), page);
    }

    /**
     * 查询详情getProcessDefinitionByDto
     */
    @GetMapping("/getProcessDefinitionByDto")
    public AjaxResult getProcessDefinitionByDto(long id){

        ProcessDefinitionEntity entity = service.getById(id) ;

        ProcessContextDto dto = ProcessContextDto.formEntityToDto(entity) ;
        return AjaxResult.success("success" , dto) ;
    }

    /**
     * 获取目录树
     * @return
     */
    @DataPermissionQuery
    @GetMapping("/catalogTreeSelect")
    public AjaxResult catalogTreeSelect(PermissionQuery query){
        return AjaxResult.success("success" , catalogService.selectCatalogTreeList(query)) ;
    }

    /**
     * 验证脚本任务
     * @param dto
     * @return
     */
    @PostMapping("/validateTask")
    public AjaxResult validateTask(@RequestBody @Validated ProcessTaskValidateDto dto){

        log.debug("dto = {}", JSONUtil.toJsonPrettyStr(JSONObject.toJSONString(dto)));

        // 运行任务验证
        service.runProcessTask(dto) ;

        return AjaxResult.success() ;
    }

    /**
     * 保存流程定义信息 saveProcessDefinition
     */
    @DataPermissionSave
    @PostMapping("/saveProcessDefinition")
    public AjaxResult saveProcessDefinition(@RequestBody @Validated ProcessDefinitionSaveDto dto){
        service.saveProcessDefinition(dto) ;
        return ok() ;
    }

    /**
     * 更新流程定义信息
     */
    @DataPermissionSave
    @PostMapping("/updateProcessDefinition")
    public AjaxResult updateProcessDefinition(@RequestBody ProcessDefinitionDto dto){
        if("node".equals(dto.getType())){
            dto.setContext(null); // 清空上下文信息，只更新节点
        }

        List<ProcessTaskDto> taskFlow = dto.getTaskFlow() ;
        Assert.isTrue(taskFlow.size() > 1 , "流程定义为空,请定义流程.");

        service.updateProcessDefinition(dto) ;
        return AjaxResult.success() ;
    }

    /**
     * 保存流程定义信息
     * @return
     */
    @DataPermissionSave
    @PostMapping("/commitProcessDefinition")
    public AjaxResult commitProcessDefinition(@RequestBody ProcessDefinitionDto dto){

        List<ProcessTaskDto> taskFlow = dto.getTaskFlow() ;
        Assert.isTrue(taskFlow.size() > 1 , "流程定义为空,请定义流程.");

        long processId = service.commitProcessDefinition(dto) ;
        return AjaxResult.success("success" , processId) ;
    }

    /**
     * 暂停触发器
     * @param jobId
     * @return
     */
    @PostMapping("pauseTrigger")
    public AjaxResult pauseTrigger(String jobId) throws SchedulerException {

        // 更新online状态
        ProcessDefinitionEntity entity = service.getById(jobId) ;

        // 判断是否定义cron表达式
        Assert.isTrue(StringUtils.isNotBlank(entity.getScheduleCron()) , "请定义cron表达式.");

        entity.setOnline(false);
        service.updateById(entity) ;

        scheduler.pauseTrigger(TriggerKey.triggerKey(jobId , PipeConstants.TRIGGER_GROUP_NAME));
        return AjaxResult.success();
    }

    /**
     * 运行一次
     * @param jobId
     * @return
     */
    @PostMapping("runOneTime")
    public AjaxResult runOneTime(String jobId) throws SchedulerException {

        JobKey jobKey = JobKey.jobKey(jobId,PipeConstants.JOB_GROUP_NAME);
        scheduler.triggerJob(jobKey);

        return AjaxResult.success() ;
    }

    /**
     * 启动触发器
     * @param jobId
     * @return
     */
    @PostMapping("startJob")
    public AjaxResult startJob(String jobId) throws SchedulerException {

        // 更新online状态
        ProcessDefinitionEntity entity = service.getById(jobId) ;
        entity.setOnline(true);
        service.updateById(entity) ;

        scheduler.resumeTrigger(TriggerKey.triggerKey(jobId , PipeConstants.TRIGGER_GROUP_NAME));//恢复Trigger
        return AjaxResult.success();
    }

    /**
     * 移除触发器
     * @param jobId
     * @return
     */
    @PostMapping("unscheduleJob")
    public AjaxResult unscheduleJob(String jobId) throws SchedulerException {

        // 更新online状态
        ProcessDefinitionEntity entity = service.getById(jobId) ;
        entity.setOnline(false);
        service.updateById(entity) ;

        scheduler.unscheduleJob(TriggerKey.triggerKey(jobId , PipeConstants.TRIGGER_GROUP_NAME));//移除触发器
        return AjaxResult.success();
    }

    /**
     * 任务的恢复
     * @param jobId
     * @return
     */
    @PostMapping("resumeTrigger")
    public AjaxResult resumeTrigger(String jobId) throws SchedulerException {

        // 更新online状态
        ProcessDefinitionEntity entity = service.getById(jobId) ;

        // 判断是否定义cron表达式
        Assert.isTrue(StringUtils.isNotBlank(entity.getScheduleCron()) , "请定义cron表达式.");

        entity.setOnline(true);
        service.updateById(entity) ;

        scheduler.resumeTrigger(TriggerKey.triggerKey(jobId , PipeConstants.TRIGGER_GROUP_NAME)) ;
        return AjaxResult.success();
    }

    /**
     * 更新updateProcessDefineCron
     * @return
     */
    @PostMapping("updateProcessDefineCron")
    public AjaxResult updateProcessDefineCron(@RequestBody @Validated ProcessDefineCronDto dto) throws SchedulerException {
        service.updateProcessDefineCron(dto) ;
        return AjaxResult.success() ;
    }

    /**
     * 删除任务
     * @return
     */
    @DeleteMapping("deleteJob")
    public AjaxResult deleteJob(@RequestParam String jobId) throws SchedulerException {

        service.deleteJob(jobId);

        return AjaxResult.success();
    }

    @Override
    public IProcessDefinitionService getFeign() {
        return this.service;
    }
}
