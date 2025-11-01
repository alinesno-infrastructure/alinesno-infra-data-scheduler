package com.alinesno.infra.data.scheduler.trigger.controller;

import cn.hutool.core.util.IdUtil;
import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.data.scheduler.trigger.bean.BuildTask;
import com.alinesno.infra.data.scheduler.trigger.bean.QueueStatusDTO;
import com.alinesno.infra.data.scheduler.trigger.bean.TaskState;
import com.alinesno.infra.data.scheduler.trigger.entity.JobEntity;
import com.alinesno.infra.data.scheduler.trigger.entity.TriggerEntity;
import com.alinesno.infra.data.scheduler.trigger.service.BuildQueueService;
import com.alinesno.infra.data.scheduler.trigger.service.IJobService;
import com.alinesno.infra.data.scheduler.trigger.service.ITriggerService;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 提供队列可观测与操作接口
 */
@RestController
@RequestMapping("/api/infra/data/scheduler/trigger/queue")
public class QueueController {

    private final BuildQueueService buildQueueService;

    private final IJobService jobService ;

    private final ITriggerService triggerService ;

    public QueueController(BuildQueueService buildQueueService, IJobService jobService, ITriggerService triggerService) {
        this.buildQueueService = buildQueueService;
        this.jobService = jobService;
        this.triggerService = triggerService;
    }

    // 查询当前并发数（同时执行任务的数量）
    @GetMapping("/concurrency")
    public AjaxResult getConcurrency() {
        int c = buildQueueService.getConcurrency();
        return AjaxResult.success(Collections.singletonMap("concurrency", c));
    }

    // 更新并发数（可通过 request param 或 request body）, 返回新的并发数
    @PatchMapping("/concurrency")
    public AjaxResult updateConcurrency(@RequestParam("concurrency") int concurrency) {
        buildQueueService.setConcurrency(concurrency);
        int c = buildQueueService.getConcurrency();
        Map<String,Object> resp = new HashMap<>();
        resp.put("concurrency", c);
        resp.put("message", "updated");
        return AjaxResult.success(resp);
    }

    // 获取队列总体状态：queued/running/总计
    @GetMapping("/status")
    public AjaxResult status() {
        QueueStatusDTO dto = new QueueStatusDTO();
        dto.setQueued(buildQueueService.getQueueSize());
        dto.setRunning(buildQueueService.getRunningCount());
        dto.setTotalKnownTasks(buildQueueService.listAll().size());
        return AjaxResult.success(dto);
    }

    // 列出所有任务（可用 status 过滤）
    @GetMapping("/tasks")
    public AjaxResult tasks(@RequestParam(required = false) TaskState status) {
        List<BuildTask> all = buildQueueService.listAll();
        if (status == null) return AjaxResult.success();
        return AjaxResult.success(all.stream().filter(t -> t.getState() == status).collect(Collectors.toList()));
    }

    // 获取单个任务详情
    @GetMapping("/task/{id}")
    public AjaxResult task(@PathVariable String id) {
        BuildTask t = buildQueueService.getById(id);
        if (t == null) throw new IllegalArgumentException("task not found: " + id);
        return AjaxResult.success(t);
    }

    // 取消任务（queued 或 running）
    @PostMapping("/task/{id}/cancel")
    public AjaxResult cancel(@PathVariable String id) {
        boolean ok = buildQueueService.cancel(id);
        return AjaxResult.success(ok ? "cancelled" : "not-cancelled");
    }

    // 删除任务（从队列/运行/历史中彻底移除）
    @DeleteMapping("/task/{id}")
    public AjaxResult deleteTask(@PathVariable String id) {
        boolean ok = buildQueueService.delete(id);
        return AjaxResult.success(ok ? "deleted" : "not-found");
    }

    // 添加测试任务
    @PostMapping("/task/add")
    public AjaxResult addTask(String name , String cron) {

        JobEntity job1 = new JobEntity();
        job1.setId(IdUtil.getSnowflakeNextId());
        job1.setName(name);
        jobService.save(job1);

        TriggerEntity t1 = new TriggerEntity();
        t1.setId(IdUtil.getSnowflakeNextId());
        t1.setJobId(job1.getId());
        t1.setCron(cron);
        triggerService.save(t1);

        return AjaxResult.success("ok") ;
    }
}