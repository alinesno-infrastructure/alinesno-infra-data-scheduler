package com.alinesno.infra.data.scheduler.executor.utils;

import org.stringtemplate.v4.ST;

import java.util.Map;

/**
 * StringTemplate模板框架工具类
 */
public class StringTemplateUtils {

    /**
     * 渲染指定名称的字符串模板，并返回渲染后的结果。
     *
     * @param templateContent 模板内容
     * @param model 数据模型
     * @return 渲染后的字符串
     * @throws IllegalArgumentException 如果模板内容为空
     */
    public static String render(String templateContent, Map<String, Object> model) {
        if (templateContent == null || templateContent.isEmpty()) {
            throw new IllegalArgumentException("Template content cannot be null or empty");
        }

        // 创建一个StringTemplate实例
        ST st = new ST(templateContent);

        // 将模型中的所有键值对添加到模板中
        if (model != null) {
            for (Map.Entry<String, Object> entry : model.entrySet()) {
                st.add(entry.getKey(), entry.getValue());
            }
        }

        // 渲染模板并返回结果
        return st.render();
    }
}