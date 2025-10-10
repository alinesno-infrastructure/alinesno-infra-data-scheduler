package com.alinesno.infra.data.scheduler.workflow.controller;

import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.data.scheduler.constants.PipeConstants;
import com.alinesno.infra.data.scheduler.workflow.dto.FlowDto;
import com.alinesno.infra.data.scheduler.workflow.dto.LastExecuteFlowDto;
import com.alinesno.infra.data.scheduler.workflow.dto.WorkflowRequestDto;
import com.alinesno.infra.data.scheduler.workflow.entity.FlowEntity;
import com.alinesno.infra.data.scheduler.workflow.service.IFlowService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 流程控制
 * @version 1.0.0
 * @author luoxiaodong
 */
@Slf4j
@RestController
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequestMapping("/api/infra/data/scheduler/flow")
public class FlowController {

    @Autowired
    private Scheduler scheduler ;

    @Autowired
    private IFlowService flowService;

    @PostMapping("/processAndSave")
    public AjaxResult processAndSave(@RequestBody WorkflowRequestDto flowDto, @RequestParam Long processDefinitionId) {
        FlowEntity  flowEntity = flowService.saveRoleFlow(processDefinitionId, flowDto);  // 保存工作流
        return AjaxResult.success("保存成功." , flowEntity.getId());
    }

    @GetMapping("/tryRun")
    public DeferredResult<AjaxResult> tryRun(@RequestParam Long processDefinitionId) {
        // 设置超时时间（毫秒），根据业务调整
        long timeout = 300_000L;
        DeferredResult<AjaxResult> deferred = new DeferredResult<>(timeout);

        // 后端异步任务
        CompletableFuture<String> future = flowService.tryRun(processDefinitionId);

        // 当 future 完成时设置 DeferredResult
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                // 记录日志、包装错误信息
                // logger.error("tryRun failed", ex);
                deferred.setErrorResult(AjaxResult.error("流程试运行失败"));
            } else {
                // 可以把 result 放到 AjaxResult 的 data 中，或按需处理
                deferred.setResult(AjaxResult.success("流程试运行成功", result));
            }
        });

        // 超时处理
        deferred.onTimeout(() -> {
            // 可取消底层任务，避免无意义的资源占用
            future.completeExceptionally(new java.util.concurrent.TimeoutException("执行超时"));
            deferred.setErrorResult(AjaxResult.error("请求超时，请稍后重试"));
        });

        // 可选：完成时的清理逻辑
        deferred.onCompletion(() -> {
            // 比如记录完成、释放自定义资源等
        });

        return deferred;
    }

    /**
     * 停止正在运行的任务实例（尝试中断）
     * @param processDefinitionId Job 的 id（即 JobKey.name）
     */
    @GetMapping("stopRun")
    public AjaxResult stopRun(String processDefinitionId) throws SchedulerException {

        Assert.hasLength(processDefinitionId , "任务标识为空");

        List<JobExecutionContext> executingJobs = scheduler.getCurrentlyExecutingJobs();
        boolean found = false;
        for (JobExecutionContext executingJob : executingJobs) {
            JobKey jobKey = executingJob.getJobDetail().getKey();
            if (jobKey.getName().equals(processDefinitionId) && jobKey.getGroup().equals(PipeConstants.JOB_GROUP_NAME)) {
                // 使用 fireInstanceId 精确中断该次执行实例
                String fireInstanceId = executingJob.getFireInstanceId();
                boolean interrupted = scheduler.interrupt(fireInstanceId);
                log.info("请求中断 jobId={} fireInstanceId={} result={}", processDefinitionId, fireInstanceId, interrupted);
                found = true;
            }
        }

        if (!found) {
            return AjaxResult.error("未发现正在运行的任务实例");
        }

        return AjaxResult.success();
    }

    /**
     * 发布工作流接口
     * @param processDefinitionId 角色ID
     * @return 操作结果
     */
    @PostMapping("/publish")
    public AjaxResult publishFlow(@RequestParam Long processDefinitionId) {
        try {
            flowService.publishFlow(processDefinitionId);
            return AjaxResult.success("工作流发布成功");
        } catch (Exception e) {
            log.error("工作流发布失败", e);
            return AjaxResult.error("工作流发布失败：" + e.getMessage());
        }
    }

    /**
     * 获取指定角色最新版本的已发布流程接口
     * @param processDefinitionId 角色ID
     * @return 包含最新版本已发布流程的结果
     */
    @GetMapping("/latest")
    public AjaxResult getLatestFlowByProcessDefinitionId(@RequestParam Long processDefinitionId) {
        FlowDto flowDto = flowService.getLatestFlowByProcessDefinitionId(processDefinitionId);
        return AjaxResult.success(flowDto);
    }

    /**
     * 发布流程
     */
    @GetMapping("/published")
    public AjaxResult published(@RequestParam Long flowId) {
        flowService.publishFlow(flowId);
        return AjaxResult.success("流程发布成功");
    }

    /**
     * 获取指定角色最新版本的已发布流程接口
     * @param processDefinitionId 角色ID
     * @return 包含最新版本已发布流程的结果
     */
    @GetMapping("/latestPublished")
    public AjaxResult getLatestPublishedFlowByProcessDefinitionId(@RequestParam Long processDefinitionId) {
        FlowEntity flowEntity = flowService.getLatestPublishedFlowByProcessDefinitionId(processDefinitionId);
        if (flowEntity != null) {
            return AjaxResult.success(flowEntity);
        }
        return AjaxResult.error("未找到指定角色的最新已发布流程");
    }

    /**
     * 获取到最后运行的流程执行节点
     */
    @GetMapping("/lastExecutedFlowNodes")
    public AjaxResult lastExecutedFlowNodes(@RequestParam Long processDefinitionId) {

        LastExecuteFlowDto flowNodeExecutionDtos = flowService.getLastExecutedFlow(processDefinitionId , null) ;

        AjaxResult result = AjaxResult.success(flowNodeExecutionDtos.getFlowNode()) ;
        result.put("status" , flowNodeExecutionDtos.getStatus());
        result.put("executeInstanceId" , flowNodeExecutionDtos.getExecuteInstanceId());

        return result ;

    }

    /**
     * 获取到最后运行的流程执行节点
     */
    @GetMapping("/executedFlowNodes")
    public AjaxResult executedFlowNodes(@RequestParam Long processDefinitionId , @RequestParam Long executeId) {

        LastExecuteFlowDto flowNodeExecutionDtos = flowService.getLastExecutedFlow(processDefinitionId , executeId) ;

        AjaxResult result = AjaxResult.success(flowNodeExecutionDtos.getFlowNode()) ;
        result.put("status" , flowNodeExecutionDtos.getStatus());

        return result ;

    }

    /**
     * 获取指定角色的未发布流程接口
     * @param processDefinitionId 角色ID
     * @return 包含未发布流程的结果
     */
    @GetMapping("/unpublished")
    public AjaxResult getUnpublishedFlowByProcessDefinitionId(@RequestParam Long processDefinitionId) {
        FlowEntity flowEntity = flowService.getUnpublishedFlowByProcessDefinitionId(processDefinitionId);
        if (flowEntity != null) {
            return AjaxResult.success(flowEntity);
        }
        return AjaxResult.error("未找到指定角色的未发布流程");
    }
}