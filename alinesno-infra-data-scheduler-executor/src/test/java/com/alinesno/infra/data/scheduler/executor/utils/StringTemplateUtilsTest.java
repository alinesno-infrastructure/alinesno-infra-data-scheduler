package com.alinesno.infra.data.scheduler.executor.utils;

import org.junit.Test;
import org.stringtemplate.v4.ST;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class StringTemplateUtilsTest {
    @Test
    public void testStringTemplateRendering() {
        // 创建一个StringTemplate实例
        ST st = new ST("Hello, <name>!");

        // 设置变量值
        st.add("name", "World");

        // 渲染模板
        String output = st.render();

        // 验证输出结果
        assertEquals("Hello, World!", output);
    }
}
