package com.alinesno.infra.data.scheduler.constants;

public class PipeConstants {

    public static final String READER_SUFFIX = "_reader" ; // 读取后缀
    public static final String WRITER_SUFFIX = "_writer" ; // 写入后缀
    public static final String PLUGIN_SUFFIX = "_plugin" ; // 插件后缀

    public static String TRIGGER_GROUP_NAME = "quartz_scheduler_trigger"; // 触发器组名称
    public static String JOB_GROUP_NAME = "quartz_job"; // 任务组名称

    public static String PIPELINE_READER_COUNT = "alinesno:scheduler:%s:reader:count";  // 读取数量
    public static String PIPELINE_WRITER_COUNT = "alinesno:scheduler:%s:writer:count";  // 写入数量
}
