package com.alinesno.infra.data.scheduler.quartz.job;

import com.alinesno.infra.common.web.log.utils.SpringUtils;
import com.alinesno.infra.data.scheduler.constants.PipeConstants;
import com.alinesno.infra.data.scheduler.entity.EnvironmentEntity;
import com.alinesno.infra.data.scheduler.entity.ProcessDefinitionEntity;
import com.alinesno.infra.data.scheduler.entity.ResourceEntity;
import com.alinesno.infra.data.scheduler.entity.TaskDefinitionEntity;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import com.alinesno.infra.data.scheduler.service.IEnvironmentService;
import com.alinesno.infra.data.scheduler.service.IProcessDefinitionService;
import com.alinesno.infra.data.scheduler.service.IResourceService;
import com.alinesno.infra.data.scheduler.service.ITaskDefinitionService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.scheduling.quartz.QuartzJobBean;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

@Slf4j
@DisallowConcurrentExecution
public class QuartzJob extends QuartzJobBean implements InterruptableJob {

    // 线程池用于执行实际耗时任务（可根据需要调整）
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    // 存储当前正在执行的 future，key 使用 fireInstanceId
    private static final ConcurrentMap<String, Future<?>> RUNNING = new ConcurrentHashMap<>();

    // 实例字段，方便 interrupt() 时定位
    private volatile String currentFireInstanceId;
    private volatile Thread currentThread;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {

        // 记录当前执行实例 id 和线程
        currentFireInstanceId = context.getFireInstanceId();
        currentThread = Thread.currentThread();

        // 准备任务参数
        Result result = getResult(context);
        TaskInfoBean task = new TaskInfoBean();
        task.setProcess(result.processDefinition());
        task.setEnvironment(result.environment());
        task.setResources(result.resourceList());

        // 提交到线程池执行，并保存 Future，便于中断
        Callable<Void> callable = () -> {
            try {
                result.processDefinitionService().runProcess(task, result.taskDefinitionList());
            } catch (Throwable t) {
                // 捕获所有异常并向上记录（Quartz 框架会处理 JobExecutionException）
                log.error("任务执行发生异常: {}", t.getMessage(), t);
                throw t;
            }
            return null;
        };

        Future<Void> future = EXECUTOR.submit(callable);
        RUNNING.put(currentFireInstanceId, future);

        try {
            // 等待任务完成（如果被中断，这里会抛出 InterruptedException 或 ExecutionException）
            future.get();
        } catch (InterruptedException e) {
            // 当前线程被中断，尝试取消 future，并设置中断状态
            log.info("任务被中断（fireInstanceId={}）", currentFireInstanceId);
            future.cancel(true);
            Thread.currentThread().interrupt(); // 保留中断状态
            throw new JobExecutionException("任务被中断", e, false);
        } catch (ExecutionException e) {
            // 任务内部异常，封装为 JobExecutionException 或记录
            log.error("任务执行异常（fireInstanceId={}）: {}", currentFireInstanceId, e.getMessage(), e);
            throw new JobExecutionException(e.getCause());
        } finally {
            // 清理
            RUNNING.remove(currentFireInstanceId);
            currentFireInstanceId = null;
            currentThread = null;
        }
    }

    @Override
    public void interrupt() throws UnableToInterruptJobException {
        log.info("收到 interrupt 请求, fireInstanceId={}", currentFireInstanceId);
        // 标记中断并尝试取消 Future
        if (currentFireInstanceId != null) {
            Future<?> future = RUNNING.get(currentFireInstanceId);
            if (future != null) {
                future.cancel(true);
            }
        }
        // 同时中断当前线程（如果在等待或检查中断，这能帮助退出）
        if (currentThread != null) {
            currentThread.interrupt();
        }
    }

    private static Result getResult(JobExecutionContext context) {
        IProcessDefinitionService processDefinitionService = SpringUtils.getBean(IProcessDefinitionService.class);
        ITaskDefinitionService taskDefinitionService = SpringUtils.getBean(ITaskDefinitionService.class) ;
        IEnvironmentService environmentService = SpringUtils.getBean(IEnvironmentService.class) ;
        IResourceService resourceService = SpringUtils.getBean(IResourceService.class) ;

        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();
        Long processId = jobDataMap.getLong(PipeConstants.PROCESS_ID);

        List<String> resourceIds = new ArrayList<>();

        ProcessDefinitionEntity processDefinition = processDefinitionService.getById(processId);
        List<TaskDefinitionEntity> taskDefinitionList = taskDefinitionService.lambdaQuery().eq(TaskDefinitionEntity::getProcessId, processId).list();
        EnvironmentEntity environment = null ;

        try{
            environment = environmentService.lambdaQuery().eq(EnvironmentEntity::getProcessId, processId).getEntity();
        }catch (Exception e){
            log.error("获取环境信息失败:{}", e.getMessage());
        }

        taskDefinitionList.forEach(taskDefinition -> resourceIds.add(taskDefinition.getResourceId()));
        List<ResourceEntity> resourceList = new ArrayList<>() ;
        if(!resourceIds.isEmpty()){
            resourceList = resourceService.lambdaQuery().in(ResourceEntity::getId, resourceIds).list();
        }

        return new Result(processDefinitionService, processDefinition, taskDefinitionList, environment, resourceList);
    }

    private record Result(IProcessDefinitionService processDefinitionService,
                          ProcessDefinitionEntity processDefinition,
                          List<TaskDefinitionEntity> taskDefinitionList,
                          EnvironmentEntity environment,
                          List<ResourceEntity> resourceList) {
    }
}