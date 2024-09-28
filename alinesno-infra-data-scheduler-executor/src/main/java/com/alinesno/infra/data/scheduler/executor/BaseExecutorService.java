package com.alinesno.infra.data.scheduler.executor;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alinesno.infra.common.web.log.utils.SpringUtils;
import com.alinesno.infra.data.scheduler.api.ParamsDto;
import com.alinesno.infra.data.scheduler.constants.PipeConstants;
import com.alinesno.infra.data.scheduler.entity.EnvironmentEntity;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import com.alinesno.infra.data.scheduler.service.IDataSourceService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Value;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * BaseExecutorService
 */
@Slf4j
public abstract class BaseExecutorService implements IExecutorService {

    @Value("${alinesno.data.scheduler.workspacePath:#{systemProperties['java.io.tmpdir']}}")
    private String workspacePath;

    /**
     * 获取到运行实例的工作空间
     *
     * @param task
     * @return
     */
    protected String getWorkspace(TaskInfoBean task) {
        return workspacePath + File.separator +  task.getWorkspace();
    }

    /**
     * 从数据源ID获取数据源信息
     *
     * @param dataSourceId
     * @return
     */
    protected DataSource getDataSource(long dataSourceId) {
        log.debug("getDataSource: {}", dataSourceId);

        IDataSourceService dataSourceService = SpringUtils.getBean(IDataSourceService.class);
        if (dataSourceService != null) {
            return dataSourceService.getDataSource(dataSourceId);
        }

        return null;
    }

    /**
     * 获取到M2_Home环境配置，如果没有的话，则自动获取系统环境变量
     *
     * @return
     */
    protected String getHomeFromConfig(EnvironmentEntity environment, String envVarName) {
        if (environment == null || environment.getConfig() == null || environment.getConfig().isEmpty()) {
            return System.getenv(envVarName);
        }

        try {
            Map<String, String> mapEnv = JSONObject.parseObject(environment.getConfig(), new TypeReference<Map<String, String>>() {
            });
            return mapEnv.getOrDefault(envVarName, System.getenv(envVarName));
        } catch (Exception e) {
            log.error("Error parsing environment config for " + envVarName, e);
            return System.getenv(envVarName);
        }
    }

    /**
     * 获取Maven安装目录
     *
     * @param environment 环境实体，用于获取配置信息
     * @return Maven安装目录的路径
     */
    protected String getM2Home(EnvironmentEntity environment) {
        return getHomeFromConfig(environment, "M2_HOME");
    }

    /**
     * 获取Python安装目录
     *
     * @param environment 环境实体，用于获取配置信息
     * @return Python安装目录的路径
     */
    protected String getPythonHome(EnvironmentEntity environment) {
        return getHomeFromConfig(environment, "PYTHON_HOME");
    }

    /**
     * 获取Java安装目录
     *
     * @param environment 环境实体，用于获取配置信息
     * @return Java安装目录的路径
     */
    protected String getJavaHome(EnvironmentEntity environment) {
        return getHomeFromConfig(environment, "JAVA_HOME");
    }


    /**
     * 从任务信息中获取参数
     *
     * @param task
     * @return
     */
    protected ParamsDto getParamsDto(TaskInfoBean task) {
        ParamsDto paramsDto = JSONObject.parseObject(task.getTask().getTaskParams(), ParamsDto.class);
        log.debug("getParamsDto: {}", paramsDto);
        return paramsDto;
    }

    /**
     * 写入项目空间的日志文件中，每个任务开始的时候都会调用这个方法
     */
    @SneakyThrows
    protected void writeLog(TaskInfoBean task, String logText) {

        String workspace = getWorkspace(task);
        File logFile = new File(workspace, PipeConstants.RUNNING_LOGGER);

        // 确保日志文件的父目录存在
        if (!logFile.getParentFile().exists() && !logFile.getParentFile().mkdirs()) {
            log.error("无法创建日志文件的父目录: {}", logFile.getParent());
            FileUtils.forceMkdir(logFile.getParentFile());
        }

        // 尝试将日志内容追加到文件中
        try {
            FileUtils.writeStringToFile(logFile, logText + System.lineSeparator(), StandardCharsets.UTF_8, true);
        } catch (IOException e) {
            log.error("写入日志失败: {}", e.getMessage(), e);
            return;
        }

        // 打印一条信息到控制台或者其他的日志系统
        log.debug("任务[{}]的日志已成功写入: {}", task.getTask().getId(), logText);
    }

    /**
     * 写入项目空间的日志文件中，每个任务开始的时候都会调用这个方法
     */
    @SneakyThrows
    protected void writeLog(TaskInfoBean task, Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        e.printStackTrace(pw);
        String stackTrace = sw.toString(); // 获取完整的堆栈跟踪信息

        writeLog(task, stackTrace);
    }

}
