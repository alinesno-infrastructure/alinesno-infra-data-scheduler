// NotificationResult.java
package com.alinesno.infra.data.scheduler.im.bean;

import lombok.Data;

@Data
public class NotificationResult {
    private boolean success;
    private String code;
    private String message;
    private String rawResponse;

    public static NotificationResult success(String rawResponse) {
        NotificationResult r = new NotificationResult();
        r.success = true; r.rawResponse = rawResponse; r.message = "ok";
        return r;
    }
    public static NotificationResult fail(String code, String message, String rawResponse) {
        NotificationResult r = new NotificationResult();
        r.success = false; r.code = code; r.message = message; r.rawResponse = rawResponse;
        return r;
    }
}