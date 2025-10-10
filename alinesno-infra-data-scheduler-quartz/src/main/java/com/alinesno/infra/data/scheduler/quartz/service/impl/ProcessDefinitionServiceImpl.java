package com.alinesno.infra.data.scheduler.quartz.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.common.core.utils.StringUtils;
import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.web.log.utils.SpringUtils;
import com.alinesno.infra.data.scheduler.adapter.CloudStorageConsumer;
import com.alinesno.infra.data.scheduler.api.*;
import com.alinesno.infra.data.scheduler.constants.PipeConstants;
import com.alinesno.infra.data.scheduler.entity.ProcessDefinitionEntity;
import com.alinesno.infra.data.scheduler.entity.ProcessInstanceEntity;
import com.alinesno.infra.data.scheduler.entity.TaskDefinitionEntity;
import com.alinesno.infra.data.scheduler.entity.TaskInstanceEntity;
import com.alinesno.infra.data.scheduler.enums.ExecutorTypeEnums;
import com.alinesno.infra.data.scheduler.enums.JobStatusEnums;
import com.alinesno.infra.data.scheduler.enums.ProcessStatusEnums;
import com.alinesno.infra.data.scheduler.executor.IExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import com.alinesno.infra.data.scheduler.quartz.mapper.ProcessDefinitionMapper;
import com.alinesno.infra.data.scheduler.quartz.utils.CronConverter;
import com.alinesno.infra.data.scheduler.quartz.utils.ProcessUtils;
import com.alinesno.infra.data.scheduler.scheduler.IQuartzSchedulerService;
import com.alinesno.infra.data.scheduler.service.*;
import com.alinesno.infra.data.scheduler.workflow.service.IFlowService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.TriggerKey;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.File;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
public class ProcessDefinitionServiceImpl extends IBaseServiceImpl<ProcessDefinitionEntity, ProcessDefinitionMapper> implements IProcessDefinitionService {

    @Autowired
    private IQuartzSchedulerService distSchedulerService;

    @Autowired
    private IEnvironmentService environmentService;

    @Autowired
    private ITaskDefinitionService taskDefinitionService;

    @Autowired
    private IProcessInstanceService processInstanceService;

    @Autowired
    private ITaskInstanceService taskInstanceService;

    @Autowired
    private ISecretsService secretsService;

    @Autowired
    protected CloudStorageConsumer storageConsumer;

    @Value("${alinesno.file.local.path:${java.io.tmpdir}}")
    protected String localPath;

    @Autowired
    private Scheduler scheduler ;

    @Autowired
    private IResourceService resourceService;


    @Value("${alinesno.data.scheduler.workspacePath:#{systemProperties['java.io.tmpdir']}}")
    private String workspacePath;

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public CompletableFuture<Void> runProcess(TaskInfoBean task, List<TaskDefinitionEntity> taskDefinition) {

        IFlowService flowService = SpringUtils.getBean(IFlowService.class);

        ProcessDefinitionEntity process = task.getProcess();
        log.debug("ProcessDefinitionServiceImpl.runProcess Start:{}", process.getId());

        // 获取代表整个流程执行的 CompletableFuture（注意：flowService.runRoleFlow 必须返回代表真正执行结束的 future）
        CompletableFuture<String> flowFuture = flowService.runRoleFlow(process.getId());

        // 将 flowFuture 链接为一个 CompletableFuture<Void>，便于上层统一等待/取消

        return flowFuture.handle((res, ex) -> {
            ProcessDefinitionEntity finishProcess = getById(process.getId());
            if (ex != null) {
                // 记录日志
                log.error("ProcessDefinitionServiceImpl.runProcess Failed:{} , error:{}", process.getId(), ((Throwable) ex).getMessage());
                // 你可以在此根据需要更新数据库的失败状态（如果想在流程失败时进行额外持久化）
                // 抛出异常以让上层 knowing it's failed
                throw new CompletionException((Throwable) ex);
            } else {
                log.debug("ProcessDefinitionServiceImpl.runProcess Finish:{}", process.getId());
                finishProcess.setUpdateTime(new Date());
                update(finishProcess);
                return null;
            }
        });
    }

    /**
     * 运行流程实例
     *
     * @param task
     * @param taskDefinition
     * @param process
     */
    @SneakyThrows
    private void runProcessInstance(TaskInfoBean task, List<TaskDefinitionEntity> taskDefinition, ProcessDefinitionEntity process) {
        IExecutorService executorService;

        // 记录任务流程实例开始
        long count = processInstanceService.count(new LambdaQueryWrapper<ProcessInstanceEntity>().eq(ProcessInstanceEntity::getProcessId, process.getId())) + 1;
        ProcessInstanceEntity processInstance = ProcessUtils.fromTaskToProcessInstance(process, count);

        processInstance.setProcessId(process.getId());
        processInstance.setState(ProcessStatusEnums.RUNNING.getCode());

        // 生成实例工作空间
        String instanceWorkspace = process.getId() + File.separator + count;
        FileUtils.forceMkdir(new File(workspacePath + File.separator + instanceWorkspace)); // 创建工作空间
        processInstance.setWorkspace(instanceWorkspace);

        processInstanceService.save(processInstance);

        task.setWorkspace(instanceWorkspace);
        task.setWorkspacePath(workspacePath);

        boolean isFailTask = false;

        // 任务开始更新状态
        for (TaskDefinitionEntity t : taskDefinition) {
            task.setTask(t);

            // 记录任务实例开始
            TaskInstanceEntity taskInstance = ProcessUtils.fromTaskToTaskInstance(process, t, processInstance.getId());
            taskInstance.setState(ProcessStatusEnums.RUNNING.getCode());
            taskInstance.setStartTime(new Date());

            // 设置权限角色
            taskInstance.setOrgId(processInstance.getOrgId());
            taskInstance.setDepartmentId(process.getDepartmentId());
            taskInstance.setOperatorId(processInstance.getOperatorId());

            taskInstanceService.save(taskInstance);

            String beanName = t.getTaskType() + "Executor";
            log.debug("TaskExecutor BeanName:{}", beanName);

            executorService = SpringUtils.getBean(beanName);
            try {
                // 配置任务参数
                // configTaskParams(task, executorService);
                executorService.execute(task);

                // 更新任务实例结束
                taskInstance.setState(ProcessStatusEnums.END.getCode());
                taskInstance.setEndTime(new Date());
                taskInstanceService.update(taskInstance);

            } catch (Exception e) {

                log.error("任务执行异常：", e);

                taskInstance.setState(ProcessStatusEnums.FAIL.getCode());
                taskInstance.setEndTime(new Date());
                taskInstance.setErrorMsg(e.getMessage());
                taskInstanceService.update(taskInstance);

                isFailTask = true;

                // 如果异常中断则直接跳出，否则继续执行
                if (!t.isContinueIgnore()) {
                    break;
                }
            }finally {
                // 关闭数据源连接
                if(executorService != null){
                    executorService.closeDataSource();
                }
            }
        }

        if (isFailTask) {
            processInstance.setState(ProcessStatusEnums.FAIL.getCode());
        } else {
            // 任务流程结束，更新任务实例
            processInstance.setState(ProcessStatusEnums.END.getCode());
        }

        processInstance.setEndTime(new Date());
        processInstanceService.update(processInstance);
    }



    @Override
    public long commitProcessDefinition(ProcessDefinitionDto dto) {
        long projectId = dto.getProjectId();

        ProcessDefinitionEntity processDefinition = ProcessUtils.fromDtoToEntity(dto);
        processDefinition.setOnline(false);
        this.save(processDefinition);

        long processId = processDefinition.getId();
        List<TaskDefinitionEntity> taskDefinitionList = ProcessUtils.fromDtoToTaskInstance(dto, processId, projectId);

        taskDefinitionList.forEach(t -> {
                t.setProcessId(processId);
                t.setId(IdUtil.getSnowflakeNextId());
            }
        );

        taskDefinitionService.saveBatch(taskDefinitionList);

        log.debug("saveProcessDefinition:{}", processId);

        // 生成job任务
        distSchedulerService.addJob(processId + "", dto.getContext().getCronExpression());

        return processId;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)  // 此方法不需要开启事务
    @SneakyThrows
    @Override
    public void runProcessTask(ProcessTaskValidateDto dto) {

        ParamsDto params = dto.getTaskParams() ;
        String type = ExecutorTypeEnums.fromType(Integer.parseInt(dto.getTaskType())).getCode();

        String beanName = type + "Executor";
        log.debug("TaskExecutor BeanName:{}", beanName);
        IExecutorService executorService = SpringUtils.getBean(beanName);;

        TaskInfoBean task = new TaskInfoBean() ;
        TaskDefinitionEntity taskDefinition = new TaskDefinitionEntity() ;

        taskDefinition.setTaskParams(JSONObject.toJSONString(params));
        task.setTask(taskDefinition);

        String fileName = IdUtil.getSnowflakeNextIdStr() ;
        FileUtils.forceMkdir(new File(workspacePath, fileName)); // 创建工作空间
        task.setWorkspace(fileName);

        log.debug("workspace = {}" , fileName);

        ProcessDefinitionEntity process = new ProcessDefinitionEntity() ;
        process.setEnvId(dto.getContext().getEnvId()) ;
        process.setGlobalParams(JSONObject.toJSONString((dto.getContext().getGlobalParams()))) ;
        task.setProcess(process) ;

        // 配置任务参数
        // configTaskParams(task, executorService);

        // 执行任务
        executorService.execute(task);

        FileUtils.forceDeleteOnExit(new File(workspacePath, task.getWorkspace()));
    }

    @Override
    public List<ProcessDefinitionEntity> queryRecentlyProcess(int count, PermissionQuery query) {

        LambdaQueryWrapper<ProcessDefinitionEntity> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.setEntityClass(ProcessDefinitionEntity.class);
        query.toWrapper(queryWrapper);

        queryWrapper
                .orderByDesc(ProcessDefinitionEntity::getAddTime)
                .last("limit " + count);

        return this.list(queryWrapper);
    }

    /**
     * 更新流程定义信息
     *
     * 此方法用于根据传入的流程定义DTO更新流程定义的信息它允许修改流程定义的各种属性，
     * 比如流程名称、描述等这个方法不对更新操作的成功与否进行返回，因此不包含返回值说明
     *
     * @param dto 包含要更新的流程定义信息的DTO（数据传输对象）不能为空这个DTO包含了所有
     *            需要更新的流程定义的信息，如流程定义ID、新流程名称、新描述等
     */
    @Override
    public void updateProcessDefinition(ProcessDefinitionDto dto) {

        long processId = dto.getProcessId() ; // context.getId();

        if(dto.getContext() != null){
            ProcessContextDto context = dto.getContext() ;
            Assert.notNull(context, "流程定义上下文不能为空");

            ProcessDefinitionEntity oldProcessDefinition = this.getById(processId) ;

            ProcessDefinitionEntity processDefinition = ProcessUtils.fromDtoToEntity(dto);
            BeanUtils.copyProperties(processDefinition, oldProcessDefinition);

            oldProcessDefinition.setId(processId);

            this.updateById(oldProcessDefinition);
        }

        List<ProcessTaskDto> taskFlow = dto.getTaskFlow() ;
        Assert.isTrue(taskFlow.size() > 1 , "流程定义为空,请定义流程.");

        long projectId = dto.getProjectId();

        // 删除流程关联的所有任务
        LambdaUpdateWrapper<TaskDefinitionEntity>  updateWrapper = new LambdaUpdateWrapper<>() ;
        updateWrapper.eq(TaskDefinitionEntity::getProcessId , processId) ;
        taskDefinitionService.remove(updateWrapper);

        // 重新添加关联任务
        List<TaskDefinitionEntity> taskDefinitionList = ProcessUtils.fromDtoToTaskInstance(dto, processId, projectId);

        AtomicInteger orderNum = new AtomicInteger(1);
        taskDefinitionList.forEach(t -> {
            t.setOrderNum(orderNum.getAndIncrement());
            t.setProcessId(processId);
        });

        taskDefinitionService.saveOrUpdateBatch(taskDefinitionList);

    }

    @Override
    public void saveProcessDefinition(ProcessDefinitionSaveDto dto) {

        ProcessDefinitionEntity processDefinition = new ProcessDefinitionEntity() ;

        if(dto.getId() != null && dto.getId() > 0){
            processDefinition = this.getById(dto.getId()) ;
        }

        ProcessDefinitionEntity newProcessDefinition = ProcessUtils.fromSaveDtoToEntity(dto , processDefinition);
        newProcessDefinition.setOnline(false);

        this.saveOrUpdate(newProcessDefinition);

        // 生成job任务
        long processId = newProcessDefinition.getId();

        JobStatusEnums jobStatus = distSchedulerService.getJobStatus(String.valueOf(processId));

        if(jobStatus == JobStatusEnums.NOT_FOUND){
            distSchedulerService.addJob(String.valueOf(processId), null);
        }
    }

    @Override
    public void updateProcessDefineCron(ProcessDefineCronDto dto) {

        ProcessDefinitionEntity processDefinition = this.getById(dto.getProcessDefineId()) ;

        String cron = CronConverter.toQuartzCron(dto.getCron()) ;

        processDefinition.setScheduleCron(cron) ;
        this.saveOrUpdate(processDefinition);

        distSchedulerService.updateJobCron(String.valueOf(processDefinition.getId()), cron);
    }

    @SneakyThrows
    @Override
    public void deleteJob(String jobId) {
        removeById(jobId);

        scheduler.pauseTrigger(TriggerKey.triggerKey(jobId, PipeConstants.TRIGGER_GROUP_NAME));//暂停触发器
        scheduler.unscheduleJob(TriggerKey.triggerKey(jobId, PipeConstants.TRIGGER_GROUP_NAME));//移除触发器
        scheduler.deleteJob(JobKey.jobKey(jobId, PipeConstants.JOB_GROUP_NAME));//删除Job
    }

    @SneakyThrows
    @Override
    public void pauseJob(Long longJobId) {
        String jobId = String.valueOf(longJobId);

        // 更新online状态
        ProcessDefinitionEntity entity = getById(jobId) ;

        // 判断是否定义cron表达式
        if(StringUtils.isNotBlank(entity.getScheduleCron())){
            entity.setOnline(false);
            updateById(entity) ;
        }

        scheduler.pauseTrigger(TriggerKey.triggerKey(jobId , PipeConstants.TRIGGER_GROUP_NAME));
    }

}

































