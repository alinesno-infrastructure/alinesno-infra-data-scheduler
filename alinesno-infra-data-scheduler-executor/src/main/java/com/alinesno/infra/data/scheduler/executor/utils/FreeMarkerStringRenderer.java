package com.alinesno.infra.data.scheduler.executor.utils;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import lombok.Getter;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

/**
 * FreeMarker字符串渲染器，用于渲染字符串模板。
 * 该类使用FreeMarker模板引擎将字符串模板与数据模型合并，生成渲染后的字符串。
 */
public class FreeMarkerStringRenderer {

    /**
     * 获取到单实例instance
     */
    @Getter
    private static FreeMarkerStringRenderer instance = new FreeMarkerStringRenderer();

    private final Configuration configuration;

    /**
     * 私有构造方法，用于初始化FreeMarker配置。
     */
    private FreeMarkerStringRenderer() {
        // 创建FreeMarker配置实例
        this.configuration = new Configuration(Configuration.VERSION_2_3_31);

        // 使用StringTemplateLoader来处理字符串模板
        StringTemplateLoader stringLoader = new StringTemplateLoader();
        configuration.setTemplateLoader(stringLoader);
        configuration.setDefaultEncoding("UTF-8");
        configuration.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        configuration.setClassicCompatible(true); // 启用兼容模式
    }

    /**
     * 渲染指定名称的字符串模板，并返回渲染后的结果。
     *
     * @param templateName 模板名
     * @param templateContent 模板内容
     * @param model 数据模型
     * @return 渲染后的字符串
     * @throws IOException 如果读取或写入时发生I/O错误
     * @throws TemplateException 如果模板处理失败
     */
    public String render(String templateName, String templateContent, Map<String, Object> model)
            throws IOException, TemplateException {
        // 获取StringTemplateLoader
        StringTemplateLoader stringLoader = (StringTemplateLoader) configuration.getTemplateLoader();

        // 设置模板内容
        stringLoader.putTemplate(templateName, templateContent);

        // 获取模板
        Template template = configuration.getTemplate(templateName);

        // 准备输出流
        StringWriter out = new StringWriter();

        // 处理模板
        template.process(model, out);

        return out.toString();
    }

}
