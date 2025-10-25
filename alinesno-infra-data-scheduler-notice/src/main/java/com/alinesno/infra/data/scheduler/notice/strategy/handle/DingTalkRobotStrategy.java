// DingTalkRobotStrategy.java
package com.alinesno.infra.data.scheduler.notice.strategy.handle;

import com.alinesno.infra.data.scheduler.entity.NotificationConfigEntity;
import com.alinesno.infra.data.scheduler.notice.bean.NotificationMessage;
import com.alinesno.infra.data.scheduler.notice.bean.NotificationResult;
import com.alinesno.infra.data.scheduler.notice.enums.NotificationType;
import com.alinesno.infra.data.scheduler.notice.strategy.NotificationStrategy;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class DingTalkRobotStrategy implements NotificationStrategy {

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public NotificationType type() {
        return NotificationType.DINGTALK;
    }

    @Override
    public NotificationResult send(NotificationMessage message, NotificationConfigEntity config) {
        if (config == null || config.getWebhook() == null) {
            return NotificationResult.fail("CONFIG_MISSING", "DingTalk webhook missing", null);
        }
        try {
            String webhook = config.getWebhook();
            if (config.getSecret() != null) {
                long timestamp = System.currentTimeMillis();
                String stringToSign = timestamp + "\n" + config.getSecret();
                Mac mac = Mac.getInstance("HmacSHA256");
                mac.init(new SecretKeySpec(config.getSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
                byte[] signData = mac.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8));
                String sign = URLEncoder.encode(Base64.getEncoder().encodeToString(signData), "UTF-8");
                webhook = webhook + "&timestamp=" + timestamp + "&sign=" + sign;
            }

            Map<String, Object> body = new HashMap<>();
            body.put("msgtype", "text");
            Map<String, String> text = new HashMap<>();
            text.put("content", message.getTitle() + "\n" + message.getContent());
            body.put("text", text);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> resp = restTemplate.postForEntity(webhook, request, String.class);
            String respBody = resp.getBody();
            if (resp.getStatusCode().is2xxSuccessful()) {
                return NotificationResult.success(respBody);
            } else {
                return NotificationResult.fail(String.valueOf(resp.getStatusCodeValue()), "dingtalk robot http error", respBody);
            }
        } catch (Exception ex) {
            return NotificationResult.fail("EXCEPTION", ex.getMessage(), null);
        }
    }
}