package com.alinesno.infra.data.scheduler.api.controller;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.common.core.constants.SpringInstanceScope;
import com.alinesno.infra.common.facade.pageable.DatatablesPageBean;
import com.alinesno.infra.common.facade.pageable.TableDataInfo;
import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.common.web.adapter.rest.BaseController;
import com.alinesno.infra.data.scheduler.api.ProcessDefinitionDto;
import com.alinesno.infra.data.scheduler.api.ProcessTaskDto;
import com.alinesno.infra.data.scheduler.api.ProcessTaskValidateDto;
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
    @ResponseBody
    @PostMapping("/datatables")
    public TableDataInfo datatables(HttpServletRequest request, Model model, DatatablesPageBean page) {
        log.debug("page = {}", ToStringBuilder.reflectionToString(page));
        return this.toPage(model, this.getFeign(), page);
    }

    @GetMapping("/catalogTreeSelect")
    public AjaxResult catalogTreeSelect(){
        return AjaxResult.success("success" , catalogService.selectCatalogTreeList()) ;
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
     * 保存流程定义信息
     * @return
     */
    @PostMapping("/commitProcessDefinition")
    public AjaxResult commitProcessDefinition(@RequestBody ProcessDefinitionDto dto){
        log.debug("dto = {}", dto);

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
        scheduler.resumeTrigger(TriggerKey.triggerKey(jobId , PipeConstants.TRIGGER_GROUP_NAME)) ;
        return AjaxResult.success();
    }


    @Override
    public IProcessDefinitionService getFeign() {
        return this.service;
    }
}
