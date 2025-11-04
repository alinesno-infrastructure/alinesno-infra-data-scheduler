package com.alinesno.infra.data.scheduler.workflow.controller;

import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.common.facade.response.R;
import com.alinesno.infra.data.scheduler.api.worker.FlowDto;
import com.alinesno.infra.data.scheduler.api.worker.LastExecuteFlowDto;
import com.alinesno.infra.data.scheduler.api.worker.RunRoleFlowDto;
import com.alinesno.infra.data.scheduler.api.worker.WorkflowRequestDto;
import com.alinesno.infra.data.scheduler.constants.PipeConstants;
import com.alinesno.infra.data.scheduler.entity.ProcessDefinitionEntity;
import com.alinesno.infra.data.scheduler.entity.worker.FlowEntity;
import com.alinesno.infra.data.scheduler.enums.ExecutionStrategyEnums;
import com.alinesno.infra.data.scheduler.workflow.service.IFlowExecutionService;
import com.alinesno.infra.data.scheduler.workflow.service.IFlowService;
import lombok.extern.slf4j.Slf4j;
//import org.quartz.JobExecutionContext;
//import org.quartz.JobKey;
//import org.quartz.Scheduler;
//import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

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
    private IFlowExecutionService flowExecutionService;

//    @Autowired
//    private Scheduler scheduler ;

    @Autowired
    private IFlowService flowService;

    @PostMapping("/processAndSave")
    public AjaxResult processAndSave(@RequestBody WorkflowRequestDto flowDto, @RequestParam Long processDefinitionId) {
        FlowEntity  flowEntity = flowService.saveRoleFlow(processDefinitionId, flowDto);  // 保存工作流
        return AjaxResult.success("保存成功." , flowEntity.getId());
    }

    @PostMapping("/tryRun")
    public DeferredResult<AjaxResult> tryRun(@RequestParam Long processDefinitionId) {
        // 设置超时时间（毫秒），根据业务调整
        long timeout = 300_000L;
        DeferredResult<AjaxResult> deferred = new DeferredResult<>(timeout);

        // 后端异步任务
        CompletableFuture<String> future = flowService.tryRun(processDefinitionId , null , null , null);

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

//    /**
//     * 运行角色工作流
//     * @param runRoleFlowDto
//     * @return
//     * @throws SchedulerException
//     */
//    @PostMapping("/runRoleFlow")
//    public R<String> runRoleFlow(@RequestBody RunRoleFlowDto runRoleFlowDto) {
//
//        Long processDefinitionId = runRoleFlowDto.getProcessDefinitionId();
//        ProcessDefinitionEntity processDefinitionEntity = runRoleFlowDto.getProcessDefinitionEntity();
//        ExecutionStrategyEnums errorStrategy = runRoleFlowDto.getErrorStrategy();
//        Map<String , String> orgSecrets = runRoleFlowDto.getOrgSecrets();
//
//        log.debug("processDefinitionId: {}" , processDefinitionId);
//        log.debug("processDefinitionEntity: {}" , processDefinitionEntity);
//        log.debug("errorStrategy: {}" , errorStrategy);
//        log.debug("orgSecrets: {}" , orgSecrets);
//
//        CompletableFuture<String> future = flowService.runRoleFlow(processDefinitionId , processDefinitionEntity , errorStrategy , orgSecrets);
//
//        String executionId = null;
//        try {
//            // future 是已经完成的 completedFuture，所以 get()/join() 会立即返回
//            executionId = future.get();
//        } catch (Exception e) {
//            log.error("生成执行实例失败", e);
//            return R.fail() ;
//        }
//
//        // 返回执行id 给调用方
//        return R.ok(executionId);
//    }


    @PostMapping("/runRoleFlow")
    public DeferredResult<R<String>> runRoleFlow(@RequestBody RunRoleFlowDto runRoleFlowDto) {

        Long processDefinitionId = runRoleFlowDto.getProcessDefinitionId();
        ProcessDefinitionEntity processDefinitionEntity = runRoleFlowDto.getProcessDefinitionEntity();
        ExecutionStrategyEnums errorStrategy = runRoleFlowDto.getErrorStrategy();
        Map<String, String> orgSecrets = runRoleFlowDto.getOrgSecrets();

        log.debug("processDefinitionId: {}" , processDefinitionId);

        // 超时时间（毫秒），可按需调整或从 DTO/配置读取
        long timeoutMillis = runRoleFlowDto.getOutTime() ;

        // DeferredResult 用来异步返回给客户端，超时会触发 onTimeout
        DeferredResult<R<String>> deferred = new DeferredResult<>(timeoutMillis);

        // 调用 service，service 返回 CompletableFuture<String>
        CompletableFuture<String> future = flowService.runRoleFlow(processDefinitionId,
                processDefinitionEntity, errorStrategy, orgSecrets);

        // 当 CompletableFuture 完成或异常时，设置 DeferredResult 的结果
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("runRoleFlow 执行失败, processDefinitionId={}, ex={}", processDefinitionId, ex);
                // 返回失败信息（根据实际 R 的实现调整）
                deferred.setErrorResult(R.fail("流程执行失败: " + ex.getMessage()));
            } else {
                // result 是 executionId（或其他你约定的返回值）
                deferred.setResult(R.ok(result));
            }
        });

        // timeout 处理：可以尝试取消底层 future 或设置异常
        deferred.onTimeout(() -> {
            log.warn("runRoleFlow 请求超时, processDefinitionId={}, timeoutMs={}", processDefinitionId, timeoutMillis);
            // 尝试取消底层任务（取决于你的任务是否响应中断）
            try {
                future.completeExceptionally(new TimeoutException("请求超时"));
                future.cancel(true); // 可选：是否中断运行线程，视具体实现而定
            } catch (Exception e) {
                log.warn("尝试取消 future 失败: {}", e.getMessage());
            }
            deferred.setErrorResult(R.fail("请求超时，请稍后查询执行状态"));
        });

        // 请求完成时的清理（可选）
        deferred.onCompletion(() -> {
            // 若需要额外清理或指标上报可以写这里
            log.debug("runRoleFlow DeferredResult completed for processDefinitionId={}", processDefinitionId);
        });

        return deferred;
    }

    /**
     * 停止正在运行的任务实例（尝试中断）
     * @param processDefinitionId Job 的 id（即 JobKey.name）
     */
    @GetMapping("stopRun")
    public AjaxResult stopRun(String processDefinitionId) {

        Assert.hasLength(processDefinitionId , "任务标识为空");

//        List<JobExecutionContext> executingJobs = scheduler.getCurrentlyExecutingJobs();
//        boolean found = false;
//        for (JobExecutionContext executingJob : executingJobs) {
//            JobKey jobKey = executingJob.getJobDetail().getKey();
//            if (jobKey.getName().equals(processDefinitionId) && jobKey.getGroup().equals(PipeConstants.JOB_GROUP_NAME)) {
//                // 使用 fireInstanceId 精确中断该次执行实例
//                String fireInstanceId = executingJob.getFireInstanceId();
//                boolean interrupted = scheduler.interrupt(fireInstanceId);
//                log.info("请求中断 jobId={} fireInstanceId={} result={}", processDefinitionId, fireInstanceId, interrupted);
//                found = true;
//            }
//        }

//        if (!found) {
//            return AjaxResult.error("未发现正在运行的任务实例");
//        }

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

//        result.put("status" , flowNodeExecutionDtos.getStatus());
//        result.put("executeInstanceId" , flowNodeExecutionDtos.getExecuteInstanceId());

        return AjaxResult.success(flowNodeExecutionDtos);

    }

    /**
     * 获取到最后运行的流程执行节点
     */
    @GetMapping("/executedFlowNodes")
    public R<LastExecuteFlowDto> executedFlowNodes(@RequestParam Long processDefinitionId , @RequestParam Long executeId) {

        LastExecuteFlowDto flowNodeExecutionDtos = flowService.getLastExecutedFlow(processDefinitionId , executeId) ;

        //        result.put("status" , flowNodeExecutionDtos.getStatus());

        return R.ok(flowNodeExecutionDtos) ; //  AjaxResult.success(flowNodeExecutionDtos.getFlowNode());

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