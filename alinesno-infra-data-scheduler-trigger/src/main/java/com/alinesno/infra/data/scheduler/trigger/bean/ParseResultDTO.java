package com.alinesno.infra.data.scheduler.trigger.bean;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ParseResultDTO {
    private String cron;
    private String jobId;
    private List<Integer> allowedSeconds;
    private List<Integer> allowedMinutes;
    private List<Integer> allowedHours;
    private List<Integer> allowedDom;
    private List<Integer> allowedMonths;
    private List<Integer> allowedDow;
    private boolean domL;
    private boolean domLW;
    private List<Integer> domNearestWeekdays;
    private Map<Integer,Integer> dowNthMap;
    private List<String> nextMatches;
}