package com.alinesno.infra.data.scheduler.executor;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import com.alinesno.infra.data.scheduler.api.ParamsDto;
import com.alinesno.infra.data.scheduler.constants.PipeConstants;
import com.alinesno.infra.data.scheduler.entity.EnvironmentEntity;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import com.alinesno.infra.data.scheduler.executor.shell.ShellHandle;
import com.alinesno.infra.data.scheduler.executor.utils.FreeMarkerStringRenderer;
import com.alinesno.infra.data.scheduler.executor.utils.OSUtils;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * BaseExecutorService
 */
@Getter
@Slf4j
public abstract class AbstractExecutorService extends BaseResourceService implements IExecutorService {

    /**
     * 定义一个用于存储参数的DTO（数据传输对象）
     */
    private ParamsDto paramsDto ;

    /**
     * 工作空间的标识符，用于区分不同的工作环境或项目
     */
    private String workspace ;

    /**
     * 数据源对象，用于连接数据库
     */
    private DruidDataSource dataSource;

    /**
     * 环境实体对象，用于存储运行环境相关信息
     */
    private EnvironmentEntity environment ;

    /**
     * 资源列表，存储与任务或服务相关的资源名称或标识
     */
    private List<String> resources ;

    /**
     * 任务信息对象，包含任务的详细信息
     */
    private TaskInfoBean taskInfo ;

    /**
     * 全局环境变量映射，存储全局适用的环境配置
     */
    private Map<String , String> globalEnv;

    /**
     * 密钥映射，用于存储敏感信息或需要保密的配置项
     */
    private Map<String , String> secretMap ;

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
            Map<String, String> mapEnv = JSONObject.parseObject(environment.getConfig(), new TypeReference<>() {
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

    @Override
    public void setGlobalEnv(Map<String , String> globalEnv) {
        this.globalEnv = globalEnv ;
    }

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

        boolean isWindows = OSUtils.isWindows() ;
        if(isWindows){
            shellHandle = new ShellHandle("cmd.exe", "/C", command);
        }else if(OSUtils.isMacOS()){
            shellHandle = new ShellHandle("/bin/sh", "-c", command);
        }else{
            shellHandle = new ShellHandle("/bin/sh", "-c", command);
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

    @Override
    public void setSecretMap(Map<String, String> secretMap) {
        this.secretMap = secretMap ;
    }

    /**
     * 替换全局参数
     *
     * @param environment
     * @param globalParams
     * @param customParams
     */
    @Override
    public void replaceGlobalParams(EnvironmentEntity environment, String globalParams, Map<String, String> customParams) {
        Map<String, String> globalEnv = getGlobalEnvMap(environment);

        // 处理全局参数
        if( globalParams != null){
            Map<String, String> mapEnv = JSONObject.parseObject(environment.getConfig(), new TypeReference<>() {});
            globalEnv.putAll(mapEnv);
        }

        // 处理自定义参数
        if (customParams != null && !customParams.isEmpty()) {
            globalEnv.putAll(customParams);
        }

        // 打印全局变量
        globalEnv.forEach((k, v) -> log.debug("--->>> GlobalEnv: " + k + " = " + v));

        this.globalEnv = globalEnv ;
    }

    /**
     * 获取全局环境变量
     * @param environment
     * @return
     */
    @NotNull
    private static Map<String, String> getGlobalEnvMap(EnvironmentEntity environment) {
        Map<String , String> globalEnv = new HashMap<>() ;

        // 处理全局环境
        if(environment.getConfig() != null){
            StringTokenizer tokenizer = new StringTokenizer(environment.getConfig(), "\n");
            while (tokenizer.hasMoreTokens()) {
                String line = tokenizer.nextToken();
                if (line.contains("=") && line.split("=", 2).length == 2) {
                    String[] parts = line.split("=", 2);
                    globalEnv.put(parts[0], parts[1]);
                }
            }
        }
        return globalEnv;
    }

    /**
     * 结果渲染
     * @return
     */
    @SneakyThrows
    public String readerRawScript(){
        String templateContent = paramsDto.getRawScript();

        // 准备数据模型
        Map<String, Object> root = new HashMap<>();
        root.put("env", getGlobalEnv());
        root.put("secrets", secretMap);

        return FreeMarkerStringRenderer.getInstance().render("example", templateContent, root);
    }

    /**
     * 结果渲染
     * @return
     */
    @SneakyThrows
    public String readerTemplateContent(String templateContent){

        // 准备数据模型
        Map<String, Object> root = new HashMap<>();
        root.put("env", getGlobalEnv());
        root.put("secrets", secretMap);

        return FreeMarkerStringRenderer.getInstance().render("example", templateContent, root);
    }

    /**
     * 参数转换成map
     * type=&page=&page_size=&is_filter=&key=x7
     * @param requestBody
     * @return
     */
    protected Map<String, Object> bodyToMap(String requestBody) {
        Map<String, Object> map = new HashMap<>();
        if (StringUtils.hasLength(requestBody)) {
            String[] params = requestBody.split("&");
            for (String param : params) {
                String[] keyValue = param.split("=");
                if (keyValue.length == 2) {
                    map.put(keyValue[0], keyValue[1]);
                }
            }
        }
        return map;
    }

}
