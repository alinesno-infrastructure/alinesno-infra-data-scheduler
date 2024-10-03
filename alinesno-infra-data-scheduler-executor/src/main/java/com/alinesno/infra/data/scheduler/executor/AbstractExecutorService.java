package com.alinesno.infra.data.scheduler.executor;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alinesno.infra.data.scheduler.api.ParamsDto;
import com.alinesno.infra.data.scheduler.constants.PipeConstants;
import com.alinesno.infra.data.scheduler.entity.EnvironmentEntity;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import com.alinesno.infra.data.scheduler.executor.shell.ShellHandle;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import javax.lang.exception.RpcServiceRuntimeException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * BaseExecutorService
 */
@Slf4j
public abstract class AbstractExecutorService extends BaseResourceService implements IExecutorService {

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
     * @return Maven安装目录的路径
     */
    protected String getM2Home() {
        return getHomeFromConfig(this.getEnvironment(), "M2_HOME");
    }

    /**
     * 获取Python安装目录
     *
     * @return Python安装目录的路径
     */
    protected String getPythonHome() {
        return getHomeFromConfig(this.getEnvironment(), "PYTHON_HOME");
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
     * 写入项目空间的日志文件中，每个任务开始的时候都会调用这个方法
     */
    @SneakyThrows
    protected void writeLog(String logText) {

        String workspace = getWorkspace();
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
        log.debug("任务[{}]的日志已成功写入: {}", this.getTaskInfo().getTask().getId(), logText);
    }

    /**
     * 写入项目空间的日志文件中，每个任务开始的时候都会调用这个方法
     */
    @SneakyThrows
    protected void writeLog(Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        e.printStackTrace(pw);
        String stackTrace = sw.toString(); // 获取完整的堆栈跟踪信息

        writeLog(stackTrace);
    }

    @Getter
    private ParamsDto paramsDto ;

    @Getter
    private String workspace ;

    @Getter
    private DruidDataSource dataSource;

    @Getter
    private EnvironmentEntity environment ;

    @Getter
    private List<String> resources ;

    @Getter
    private TaskInfoBean taskInfo ;

    @Override
    public void setParams(ParamsDto paramsDto) {
        this.paramsDto = paramsDto ;
    }

    @Override
    public void setWorkspace(String workspace) {
        this.workspace = workspace ;
    }

    @Override
    public void setDataSource(DruidDataSource source) {
        this.dataSource = source ;
    }

    @Override
    public void setEnvironment(EnvironmentEntity environment) {
        this.environment = environment ;
    }

    @SneakyThrows
    @Override
    public void runCommand(String command) {
        File logFile = new File(getWorkspace(), PipeConstants.RUNNING_LOGGER);

        ShellHandle shellHandle;

        if(getEnvironment().isMac() || getEnvironment().isLinux()){
            shellHandle = new ShellHandle("/bin/sh", "-c", command);
        }else if(getEnvironment().isWindows()){
            shellHandle = new ShellHandle("cmd.exe", "/C", command);
        }else{
            throw new RpcServiceRuntimeException("请设置任务运行环境.");
        }

        shellHandle.setLogPath(logFile.getAbsolutePath());

        shellHandle.execute();
    }

    @Override
    public void setTaskInfoBean(TaskInfoBean taskInfo) {
        this.taskInfo = taskInfo ;
    }

    @Override
    public void setResource(List<String> resources) {
        this.resources = resources ;
    }

}
