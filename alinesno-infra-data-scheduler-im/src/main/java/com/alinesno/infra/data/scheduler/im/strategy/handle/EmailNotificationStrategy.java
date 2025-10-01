// EmailNotificationStrategy.java
package com.alinesno.infra.data.scheduler.im.strategy.handle;

import com.alinesno.infra.data.scheduler.entity.NotificationConfigEntity;
import com.alinesno.infra.data.scheduler.im.bean.NotificationMessage;
import com.alinesno.infra.data.scheduler.im.bean.NotificationResult;
import com.alinesno.infra.data.scheduler.im.enums.NotificationType;
import com.alinesno.infra.data.scheduler.im.strategy.NotificationStrategy;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;

import java.util.Properties;

@Component
public class EmailNotificationStrategy implements NotificationStrategy {

    @Override
    public NotificationType type() {
        return NotificationType.EMAIL;
    }

    @Override
    public NotificationResult send(NotificationMessage message, NotificationConfigEntity config) {
        if (config == null || config.getSmtpHost() == null) {
            return NotificationResult.fail("CONFIG_MISSING", "Email config missing", null);
        }
        try {
            JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
            mailSender.setHost(config.getSmtpHost());
            if (config.getSmtpPort() != null) mailSender.setPort(Integer.parseInt(config.getSmtpPort()));
            mailSender.setUsername(config.getSmtpUsername());
            mailSender.setPassword(config.getSmtpPassword());
            Properties props = mailSender.getJavaMailProperties();
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", "true");
            // 更多属性可按需设置

            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(config.getFromAddress());
            if (message.getTos() != null && !message.getTos().isEmpty()) {
                mail.setTo(message.getTos().toArray(new String[0]));
            }
            mail.setSubject(message.getTitle());
            mail.setText(message.getContent());

            mailSender.send(mail);
            return NotificationResult.success("mail_sent");
        } catch (Exception ex) {
            return NotificationResult.fail("EXCEPTION", ex.getMessage(), null);
        }
    }
}