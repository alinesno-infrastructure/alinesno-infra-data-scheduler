package com.alinesno.infra.data.scheduler.trigger.controller;

import cn.hutool.core.util.IdUtil;
import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.data.scheduler.trigger.bean.JobCreateDto;
import com.alinesno.infra.data.scheduler.trigger.entity.JobEntity;
import com.alinesno.infra.data.scheduler.trigger.entity.TriggerEntity;
import com.alinesno.infra.data.scheduler.trigger.service.BuildQueueService;
import com.alinesno.infra.data.scheduler.trigger.service.IJobService;
import com.alinesno.infra.data.scheduler.trigger.service.ITriggerService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Job 管理接口：CRUD + 入队触发
 */
@RestController
@RequestMapping("/api/infra/data/scheduler/trigger/job")
public class JobManageController {

    private final ITriggerService triggerService ;
    private final IJobService jobService;
    private final BuildQueueService buildQueueService;

    public JobManageController(ITriggerService triggerService, IJobService jobService, BuildQueueService buildQueueService) {
        this.triggerService = triggerService;
        this.jobService = jobService;
        this.buildQueueService = buildQueueService;
    }

    // 列出所有 job（如需分页请改用服务层分页接口）
    @GetMapping("/all")
    public AjaxResult listAll() {
        List<JobEntity> list = jobService.list();
        return AjaxResult.success(list);
    }

    // 按 id 查询
    @GetMapping("/{id}")
    public AjaxResult getById(@PathVariable String id) {
        JobEntity job = jobService.getById(id);
        if (job == null) return AjaxResult.error("job not found: " + id);
        return AjaxResult.success(job);
    }

    // 根据名称模糊查询（简单实现，服务层可替换成 DB 查询）
    @GetMapping("/search")
    public AjaxResult searchByName(@RequestParam("q") String q) {
        if (q == null || q.trim().isEmpty()) {
            return AjaxResult.error("query required");
        }
        // 简单过滤实现：服务层若支持可直接调用带条件的方法
        List<JobEntity> all = jobService.list();
        List<JobEntity> filtered = all.stream()
                .filter(j -> j.getName() != null && j.getName().toLowerCase().contains(q.toLowerCase()))
                .toList();
        return AjaxResult.success(filtered);
    }

    // 创建 job
    @PostMapping
    public AjaxResult createJob(@RequestBody @Validated JobCreateDto payload) {
        if (payload == null){
            return AjaxResult.error("payload required");
        }

        // 若 id 为空可以由 DB 或服务生成；若需要客户端生成 id，可使用 UUID
        if (payload.getId() == null) {
            payload.setId(IdUtil.getSnowflakeNextId());
        }

        JobEntity job = new JobEntity();
        job.setId(IdUtil.getSnowflakeNextId());
        job.setProcessId(payload.getProcessId());
        job.setName(payload.getName());
        job.setRemark(payload.getRemark());

        jobService.save(job);

        TriggerEntity t1 = new TriggerEntity();
        t1.setId(IdUtil.getSnowflakeNextId());
        t1.setJobId(job.getId());
        t1.setProcessId(payload.getProcessId());
        t1.setCron(payload.getCron());

        triggerService.save(t1);

        return AjaxResult.success(payload) ;
    }

    // 更新 job（全量或部分更新，视服务实现）
    @PutMapping("/{id}")
    public AjaxResult updateJob(@PathVariable Long id, @RequestBody JobEntity payload) {
        JobEntity existing = jobService.getById(id);
        if (existing == null) return AjaxResult.error("job not found: " + id);

        // 以下示例为按字段复制，按需调整（或直接调用 service.updateById(payload)）
        if (payload.getName() != null) existing.setName(payload.getName());
        if (payload.getRemark() != null) existing.setRemark(payload.getRemark());

        existing.setId(id); // 确保 id 一致
        boolean ok = jobService.updateById(existing);
        return ok ? AjaxResult.success(existing) : AjaxResult.error("update failed");
    }

    // 删除 job
    @DeleteMapping("/{id}")
    public AjaxResult deleteJob(@PathVariable String id) {
        JobEntity job = jobService.getById(id);
        if (job == null) return AjaxResult.success("not-found");
        boolean ok = jobService.removeById(id);

        // 删除任务
        buildQueueService.cancel(id);

        return ok ? AjaxResult.success("deleted") : AjaxResult.error("delete failed");
    }
}