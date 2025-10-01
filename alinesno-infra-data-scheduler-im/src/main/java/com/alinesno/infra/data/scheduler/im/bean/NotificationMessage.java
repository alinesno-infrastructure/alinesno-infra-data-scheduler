// NotificationMessage.java
package com.alinesno.infra.data.scheduler.im.bean;

import com.alinesno.infra.data.scheduler.im.enums.NotificationType;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class NotificationMessage {
    private NotificationType type;
    private Long configId; // 使用哪个机器人/邮箱配置
    private String title;
    private String content;
    private List<String> tos; // 收件人/@用户列表等
    private Map<String, Object> extras; // 扩展参数
}