package com.alinesno.infra.data.scheduler.trigger.controller;

import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.data.scheduler.trigger.bean.ParseResultDTO;
import com.alinesno.infra.data.scheduler.trigger.entity.TriggerEntity;
import com.alinesno.infra.data.scheduler.trigger.resolver.CronResolverAdvanced;
import com.alinesno.infra.data.scheduler.trigger.resolver.CronResolverAdvanced.CronSchedule;
import com.alinesno.infra.data.scheduler.trigger.service.ITriggerService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

/**
 * 调试接口：解析 cron 并返回允许值与未来若干次触发时间
 */
@RestController
@RequestMapping("/api/infra/data/scheduler/trigger/debug")
public class DebugController {

    private final CronResolverAdvanced resolver = new CronResolverAdvanced();
    private final ITriggerService triggerService;

    public DebugController(ITriggerService triggerService) {
        this.triggerService = triggerService;
    }

    // 通过直接传入 cron & jobId 解析
    @GetMapping("/parse")
    public AjaxResult parseCron(@RequestParam String cron,
                                @RequestParam(required = false) String jobId,
                                @RequestParam(defaultValue = "5") int next) {
        CronSchedule schedule = resolver.parse(cron, jobId);
        return AjaxResult.success(buildDto(schedule, cron, jobId, next));
    }

    // 通过 DB 中 triggerId 解析
    @GetMapping("/trigger/{id}/parse")
    public AjaxResult parseTrigger(@PathVariable String id,
                                       @RequestParam(defaultValue = "5") int next) {
        TriggerEntity t = triggerService.getById(id);
        if (t == null) throw new NoSuchElementException("trigger not found: " + id);
        CronSchedule schedule = resolver.parse(t.getCron(), t.getJobId()+"");
        return AjaxResult.success(buildDto(schedule, t.getCron(), t.getJobId() + "" , next));
    }

    private ParseResultDTO buildDto(CronSchedule schedule, String cron, String jobId, int next) {
        ParseResultDTO dto = new ParseResultDTO();
        dto.setCron(cron);
        dto.setJobId(jobId);

        // 使用 CronSchedule 提供的公有 getter 获取允许值与特殊规则
        dto.setAllowedSeconds(schedule.getAllowedSeconds());
        dto.setAllowedMinutes(schedule.getAllowedMinutes());
        dto.setAllowedHours(schedule.getAllowedHours());
        dto.setAllowedDom(schedule.getAllowedDom());
        dto.setAllowedMonths(schedule.getAllowedMonths());
        dto.setAllowedDow(schedule.getAllowedDow());

        dto.setDomL(schedule.isDomL());
        dto.setDomLW(schedule.isDomLW());
        dto.setDomNearestWeekdays(schedule.getDomNearestWeekdays());
        dto.setDowNthMap(schedule.getDowNthMap());

        // 计算下 next 次触发时间（按分钟步进，最多向前查找 365 天）
        List<String> nextMatches = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime cursor = now;
        int found = 0;
        int safety = 60 * 24 * 366; // upper bound minutes (1 year approx)
        while (found < next && safety-- > 0) {
            if (schedule.matches(cursor)) {
                nextMatches.add(cursor.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                found++;
            }
            cursor = cursor.plusMinutes(1);
        }
        dto.setNextMatches(nextMatches);
        return dto;
    }

}