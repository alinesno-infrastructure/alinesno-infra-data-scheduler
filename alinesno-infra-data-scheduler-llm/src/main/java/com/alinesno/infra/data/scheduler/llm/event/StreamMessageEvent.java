package com.alinesno.infra.data.scheduler.llm.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;

/**
 * 流消息传播
 */
@Getter
public class StreamMessageEvent extends ApplicationEvent {

    private final String message; // 消息内容
//    private final IndustryRoleEntity role ;
//    private final MessageTaskInfo taskInfo ;
    private final long bId;
    private final Long messageId ;

    public StreamMessageEvent(Object source, String message ,
                              long bId , Long messageId) {
        super(source);
        this.message = message;
//        this.role = role ;
//        this.taskInfo = taskInfo ;
        this.bId = bId;
        this.messageId = messageId ;
    }

}
