package com.alinesno.infra.data.scheduler.api.worker;

import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.common.facade.response.R;
import com.alinesno.infra.data.scheduler.adapter.worker.WorkerFlowConsumer;
import com.alinesno.infra.data.scheduler.constants.PipeConstants;
import com.alinesno.infra.data.scheduler.entity.ProcessDefinitionEntity;
import com.alinesno.infra.data.scheduler.entity.worker.FlowEntity;
import com.alinesno.infra.data.scheduler.enums.ExecutionStrategyEnums;
import com.alinesno.infra.data.scheduler.service.IProcessDefinitionService;
import com.alinesno.infra.data.scheduler.service.ISecretsService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import java.util.List;
import java.util.Map;

/**
 * 流程控制
 * @version 1.0.0
 * @author luoxiaodong
 */
@Slf4j
@RestController
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
@RequestMapping("/api/infra/data/scheduler/flow")
public class WorkerFlowController {

//    @Autowired
//    private Scheduler scheduler ;

    @Autowired
    private IProcessDefinitionService processDefinitionService ;

    @Autowired
    private ISecretsService secretsService;

    @Autowired
    private WorkerFlowConsumer flowService;

    @PostMapping("/processAndSave")
    public AjaxResult processAndSave(@RequestBody WorkflowRequestDto flowDto, @RequestParam Long processDefinitionId) {
        R<Long> result = flowService.processAndSave(flowDto, processDefinitionId) ;
        return AjaxResult.success("保存成功." , result.getData());
    }

    @PostMapping("/tryRun")
    public DeferredResult<AjaxResult> tryRun(@RequestParam Long processDefinitionId) {
        // 设置超时时间（毫秒），根据业务调整
        long timeout = 300_000L;
        DeferredResult<AjaxResult> deferred = new DeferredResult<>(timeout);

        ProcessDefinitionEntity process = processDefinitionService.getById(processDefinitionId) ;
        ExecutionStrategyEnums errorStrategy = ExecutionStrategyEnums.fromCode(process.getErrorStrategy()) ;

        Map<String , String> orgSecrets = secretsService.getSecretMapByOrgId(process.getOrgId()) ;

        // 后端异步任务
        RunRoleFlowDto roleFlowDto = RunRoleFlowDto.form(processDefinitionId , process , errorStrategy , orgSecrets);


        try{
            R<String> result = flowService.runRoleFlow(roleFlowDto) ;

            if(R.isSuccess(result)){
                // 更新执行结果
                process.setSuccessCount(process.getSuccessCount() + 1) ;
                processDefinitionService.updateById(process) ;
            }

            log.debug("任务执行结果：{}" , result);

            deferred.setResult(R.isSuccess(result)?AjaxResult.success("流程试运行成功", result.getData()) : AjaxResult.error("流程试运行失败")) ;
            return deferred;
        }catch (Exception e){
           log.error("tryRun failed", e);
           deferred.setErrorResult(AjaxResult.error("流程试运行失败，请确认是否发布流程"));
           return deferred;
        }
    }

//    /**
//     * 停止正在运行的任务实例（尝试中断）
//     * @param processDefinitionId Job 的 id（即 JobKey.name）
//     */
//    @GetMapping("stopRun")
//    public AjaxResult stopRun(String processDefinitionId) {
//
//        Assert.hasLength(processDefinitionId , "任务标识为空");
//
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
//
//        if (!found) {
//            return AjaxResult.error("未发现正在运行的任务实例");
//        }
//
//        return AjaxResult.success();
//    }

    /**
     * 发布工作流接口
     * @param processDefinitionId 角色ID
     * @return 操作结果
     */
    @PostMapping("/publish")
    public AjaxResult publishFlow(@RequestParam Long processDefinitionId) {
        try {
            R<Boolean> result = flowService.publishFlow(processDefinitionId);
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
        R<FlowDto> flowDto = flowService.getLatestFlowByProcessDefinitionId(processDefinitionId);
        return AjaxResult.success(flowDto.getData());
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
        R<FlowEntity> flowEntity = flowService.getLatestPublishedFlowByProcessDefinitionId(processDefinitionId);
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

        R<LastExecuteFlowDto> flowNodeExecutionResult = flowService.lastExecutedFlowNodes(processDefinitionId) ;
        LastExecuteFlowDto flowNodeExecutionDtos = flowNodeExecutionResult.getData() ;

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

        R<LastExecuteFlowDto> flowNodeExecutionDtoResult = flowService.getLastExecutedFlow(processDefinitionId , executeId) ;
        LastExecuteFlowDto flowNodeExecutionDtos = flowNodeExecutionDtoResult.getData() ;

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
        R<FlowEntity> flowEntity = flowService.getUnpublishedFlowByProcessDefinitionId(processDefinitionId);
        if (flowEntity.getData() != null) {
            return AjaxResult.success(flowEntity.getData());
        }
        return AjaxResult.error("未找到指定角色的未发布流程");
    }
}