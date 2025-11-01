package com.alinesno.infra.data.scheduler.trigger.bean;

import lombok.Data;

@Data
public class QueueStatusDTO {
    private int queued;
    private int running;
    private int totalKnownTasks;
}