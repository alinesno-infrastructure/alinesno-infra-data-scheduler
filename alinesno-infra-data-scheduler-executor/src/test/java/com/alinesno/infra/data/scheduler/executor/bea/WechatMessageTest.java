package com.alinesno.infra.data.scheduler.executor.bea;

import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.data.scheduler.executor.bean.WechatMessage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
public class WechatMessageTest {

    @Test
    public void testType_ShouldSetMsgTypeCorrectly() {
        WechatMessage message = WechatMessage.type("text");
        assertEquals("text", message.getMsgtype(), "The message type should be set correctly.");
    }

    @Test
    public void testContent_ShouldSetTextContentCorrectly() {
        WechatMessage message = WechatMessage.type("text").content("Hello, World!");
        System.out.println(JSONObject.toJSONString(message));
    }

    @Test
    public void testMarkdown_ShouldSetMarkdownContentCorrectly() {
        WechatMessage message = WechatMessage.type("markdown").markdown("### Hello, World!");
        System.out.println(JSONObject.toJSONString(message));
    }

    @Test
    public void testFullMessage_ShouldConstructMessageCorrectly() {
        WechatMessage message = WechatMessage.type("text").content("Hello, World!");
        assertEquals("text", message.getMsgtype(), "The message type should be set correctly.");
        System.out.println(JSONObject.toJSONString(message));

        message = WechatMessage.type("markdown").markdown("### Hello, World!");
        assertEquals("markdown", message.getMsgtype(), "The message type should be set correctly.");
        System.out.println(JSONObject.toJSONString(message));
    }
}
