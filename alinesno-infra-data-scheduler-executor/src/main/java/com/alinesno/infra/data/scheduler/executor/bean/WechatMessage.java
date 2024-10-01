package com.alinesno.infra.data.scheduler.executor.bean;

import lombok.Data;

@Data
public class WechatMessage {
    private String msgtype;
    private Text text;
    private Markdown markdown;

    @Data
    static class Text {
        private String content;
    }

    @Data
    static class Markdown {
        private String content;
    }

    public
    static WechatMessage type(String msgtype) {
        WechatMessage message = new WechatMessage();
        message.setMsgtype(msgtype);
        return message;
    }

    public WechatMessage content(String content) {
        this.text = new Text();
        this.text.setContent(content);
        return this; // 返回当前实例以支持链式调用
    }

    public WechatMessage markdown(String content) {
        this.markdown = new Markdown();
        this.markdown.setContent(content);
        return this; // 返回当前实例以支持链式调用
    }

}
