package com.alinesno.infra.data.scheduler.trigger.controller;

import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.data.scheduler.trigger.entity.TriggerEntity;
import com.alinesno.infra.data.scheduler.trigger.scheduler.CentralScheduler;
import com.alinesno.infra.data.scheduler.trigger.service.ITriggerService;
import org.springframework.web.bind.annotation.*;

/**
 * Trigger 管理（部分）——包含更新并触发缓存失效
 */
@RestController
@RequestMapping("/api/infra/data/scheduler/trigger/trigger")
public class TriggerManageController {

    private final ITriggerService triggerService;
    private final CentralScheduler centralScheduler;

    public TriggerManageController(ITriggerService triggerService, CentralScheduler centralScheduler) {
        this.triggerService = triggerService;
        this.centralScheduler = centralScheduler;
    }

    // 用于更新 TriggerEntity 的接口（PUT 或 PATCH，根据需要选择）
    @PutMapping("/{id}")
    public AjaxResult updateTrigger(@PathVariable String id, @RequestBody TriggerEntity payload) {
        // 读取现有对象（或直接调用 updateById）
        TriggerEntity existing = triggerService.getById(id);
        if (existing == null) throw new IllegalArgumentException("trigger not found: " + id);

        // 根据入参更新字段（示例只更新 cron 和 jobId 及其它可直接替换的字段）
        if (payload.getCron() != null) existing.setCron(payload.getCron());
        if (payload.getJobId() != null) existing.setJobId(payload.getJobId());
        // 如果还有其它字段需要更新，按需补充

        boolean ok = triggerService.updateById(existing);
        if (!ok) throw new RuntimeException("update trigger failed: " + id);

        // 使 CentralScheduler 重新解析此触发器
        centralScheduler.invalidateCache(id);

        return AjaxResult.success(existing);
    }

    // 删除触发器时也应清理缓存
    @DeleteMapping("/{id}")
    public AjaxResult deleteTrigger(@PathVariable String id) {
        TriggerEntity t = triggerService.getById(id);
        if (t == null) return  AjaxResult.success("not-found");
        boolean ok = triggerService.removeById(id);
        centralScheduler.invalidateCache(id);
        return AjaxResult.success(ok ? "deleted" : "delete-failed");
    }
}