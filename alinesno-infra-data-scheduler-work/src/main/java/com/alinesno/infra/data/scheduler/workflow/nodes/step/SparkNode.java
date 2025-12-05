package com.alinesno.infra.data.scheduler.workflow.nodes.step;

import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.common.facade.response.R;
import com.alinesno.infra.data.scheduler.adapter.spark.BaseSparkConsumer;
import com.alinesno.infra.data.scheduler.adapter.spark.SparkPythonConsumer;
import com.alinesno.infra.data.scheduler.adapter.spark.SparkSqlConsumer;
import com.alinesno.infra.data.scheduler.adapter.spark.SparkTaskConsumer;
import com.alinesno.infra.data.scheduler.api.logger.NodeLog;
import com.alinesno.infra.data.scheduler.entity.ComputeEngineEntity;
import com.alinesno.infra.data.scheduler.workflow.constants.FlowConst;
import com.alinesno.infra.data.scheduler.workflow.nodes.AbstractFlowNode;
import com.alinesno.infra.data.scheduler.workflow.nodes.variable.step.SparkNodeData;
import com.alinesno.infra.data.scheduler.workflow.service.IComputeEngineService;
import com.alinesno.infra.data.scheduler.workflow.utils.CommonsTextSecrets;
import com.alinesno.infra.data.scheduler.workflow.utils.StackTraceUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import javax.lang.exception.RpcServiceRuntimeException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * SparkNode 类继承自 AbstractFlowNode
 */
@Slf4j
@Data
@Scope("prototype")
@Service(FlowConst.FLOW_STEP_NODE + "spark")
@EqualsAndHashCode(callSuper = true)
public class SparkNode extends AbstractFlowNode {

    @Autowired
    private IComputeEngineService computeEngineService ;

    public SparkNode() {
        setType("spark");
    }

    @Override
    protected CompletableFuture<Void> handleNode() {

        ComputeEngineEntity computeEngine = computeEngineService.getCurrentConfig(flowExecution.getOrgId());
        Assert.notNull(computeEngine , "未找到计算引擎配置");

        SparkNodeData nodeData = getNodeData();

        String runType = nodeData.getRunType() ;

        if (runType.equals("spark-sql")) {
            return getSparkSqlCompletableFuture(computeEngine , nodeData);
        }else if(runType.equals("pyspark")){
            return getPySparkCompletableFuture(computeEngine , nodeData);
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * 获取 PySpark 节点的 CompletableFuture
     * @param computeEngine
     * @param nodeData
     * @return
     */

    private CompletableFuture<Void> getPySparkCompletableFuture(ComputeEngineEntity computeEngine, SparkNodeData nodeData) {
        // 异步执行主逻辑
        return CompletableFuture.runAsync(() -> {
            String taskId = String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown");
            String stepId = node != null ? node.getId() : "unknown";
            String stepName = node != null ? node.getStepName() : "unknown";

            // 记录开始日志（容错）
            try {
                nodeLogService.append(NodeLog.of(
                        taskId,
                        stepId,
                        stepName,
                        "INFO",
                        "Spark 节点开始执行",
                        Map.of(
                                "nodeType", "spark",
                                "runType", "pyspark",
                                "async", Boolean.toString(nodeData.isAsync())
                        )
                ));
            } catch (Exception ignore) {
                log.warn("记录 SparkNode 开始日志失败: {}", ignore.getMessage());
            }

            try {
                // 获取脚本内容
                String scriptText = nodeData.getPysparkContent();

                String scriptTextNoSec = CommonsTextSecrets.replace(scriptText , orgSecret) ;
                String script = replacePlaceholders(scriptTextNoSec);

                if (script == null || script.trim().isEmpty()) {
                    RuntimeException re = new RuntimeException("pysparkContent 为空，无法提交任务");
                    // 记录错误日志（容错）
                    try {
                        nodeLogService.append(NodeLog.of(
                                taskId,
                                stepId,
                                stepName,
                                "ERROR",
                                "提交失败: pysparkContent 为空",
                                Map.of("nodeType", "spark")
                        ));
                    } catch (Exception ignore) {
                        log.warn("记录 SparkNode 错误日志失败: {}", ignore.getMessage());
                    }
                    throw re;
                }

                SparkPythonConsumer pythonConsumer = new SparkPythonConsumer(computeEngine);

                // 提交任务：异步或同步（同步使用 submitRaw，传入 waitMs）
                R<BaseSparkConsumer.SubmitRespDto> submitResp;

                // 同步提交，使用 consumer 的超时作为 waitMs（秒 -> 毫秒）
                submitResp = pythonConsumer.submitRaw(script, 60_000L);

                if (submitResp == null) {
                    RuntimeException re = new RuntimeException("提交 Spark 任务返回 null");
                    try {
                        nodeLogService.append(NodeLog.of(
                                taskId,
                                stepId,
                                stepName,
                                "ERROR",
                                "提交 Spark 任务返回 null",
                                Map.of("nodeType", "spark")
                        ));
                    } catch (Exception ignore) {
                        log.warn("记录 SparkNode 错误日志失败: {}", ignore.getMessage());
                    }
                    throw re;
                }

                BaseSparkConsumer.SubmitRespDto submitData = submitResp.getData();
                String appId = submitData != null ? submitData.getApplicationId() : null;
                String submitStatus = submitData != null ? submitData.getStatus() : null;

                if (appId == null || appId.trim().isEmpty()) {
                    RpcServiceRuntimeException ex = new RpcServiceRuntimeException("提交成功但未返回 applicationId，无法跟踪任务状态");
                    try {
                        nodeLogService.append(NodeLog.of(
                                taskId,
                                stepId,
                                stepName,
                                "ERROR",
                                "提交成功但未返回 applicationId，无法跟踪任务状态",
                                Map.of("nodeType", "spark", "submitStatus", submitStatus == null ? "" : submitStatus)
                        ));
                    } catch (Exception ignore) {
                        log.warn("记录 SparkNode 错误日志失败: {}", ignore.getMessage());
                    }
                    throw ex;
                }

                log.info("pyspark 提交成功 appId={}, submitStatus={}", appId, submitStatus);
                try {
                    nodeLogService.append(NodeLog.of(
                            taskId,
                            stepId,
                            stepName,
                            "INFO",
                            "pyspark 提交成功",
                            Map.of(
                                    "nodeType", "spark",
                                    "appId", appId,
                                    "submitStatus", submitStatus == null ? "" : submitStatus
                            )
                    ));
                } catch (Exception ignore) {
                    log.warn("记录 SparkNode 提交日志失败: {}", ignore.getMessage());
                }

                // 轮询配置（可按需改为从 nodeData 或 computeEngine 读取）
                final int pollIntervalSeconds = 5;
                final long maxWaitSeconds = 3600L; // 默认 1 小时
                long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(maxWaitSeconds);

                SparkTaskConsumer taskConsumer = new SparkTaskConsumer(computeEngine);

                // logOffset 用于增量获取日志
                final int fetchLen = 8192;
                long logOffset = 0L;

                // 如果是同步调用，则等待任务完成
                if (!nodeData.isAsync()) {
                    while (true) {
                        if (System.currentTimeMillis() > deadline) {
                            RuntimeException re = new RuntimeException("等待 Spark 任务达到终态超时 appId=" + appId);
                            try {
                                nodeLogService.append(NodeLog.of(
                                        taskId,
                                        stepId,
                                        stepName,
                                        "ERROR",
                                        "等待 Spark 任务达到终态超时",
                                        Map.of("appId", appId)
                                ));
                            } catch (Exception ignore) {
                                log.warn("记录 SparkNode 超时日志失败: {}", ignore.getMessage());
                            }
                            throw re;
                        }

                        // 先查询状态
                        R<SparkTaskConsumer.SparkTaskStatusDTO> statusResp = taskConsumer.queryTaskStatus(appId);
                        if (statusResp == null) {
                            log.warn("queryTaskStatus 返回 null, appId={}, {}s 后重试", appId, pollIntervalSeconds);
                        } else {
                            SparkTaskConsumer.SparkTaskStatusDTO statusDto = statusResp.getData();
                            if (statusDto != null) {
                                String state = statusDto.getStatus();
                                log.info("查询任务状态 appId={}, state={}, message={}", appId, state, statusDto.getStatusDesc());
                                try {
                                    nodeLogService.append(NodeLog.of(
                                            taskId,
                                            stepId,
                                            stepName,
                                            "INFO",
                                            "查询任务状态",
                                            Map.of(
                                                    "appId", appId,
                                                    "state", state == null ? "" : state,
                                                    "message", statusDto.getStatusDesc() == null ? "" : statusDto.getStatusDesc()
                                            )
                                    ));
                                } catch (Exception ignore) {
                                    log.warn("记录 SparkNode 状态日志失败: {}", ignore.getMessage());
                                }
                            } else {
                                log.info("queryTaskStatus 无 data, appId={}, {}s 后重试", appId, pollIntervalSeconds);
                            }
                        }

                        // 然后拉取增量日志并记录
                        try {
                            R<SparkTaskConsumer.LogContentDTO> logResp = taskConsumer.getLogContent(appId, logOffset, fetchLen);
                            if (logResp != null && logResp.getData() != null) {
                                SparkTaskConsumer.LogContentDTO logDto = logResp.getData();
                                String content = logDto.getLogContent();
                                if (content != null && !content.isEmpty()) {
                                    try {
                                        nodeLogService.append(NodeLog.of(
                                                taskId,
                                                stepId,
                                                stepName,
                                                "INFO",
                                                "Spark 运行日志",
                                                Map.of(
                                                        "appId", appId,
                                                        "offset", String.valueOf(logDto.getOffset()),
                                                        "length", String.valueOf(logDto.getLength()),
                                                        "contentPreview", content.length() > 1000 ? content.substring(0, 1000) + "..." : content
                                                )
                                        ));
                                    } catch (Exception ignore) {
                                        log.warn("记录 SparkNode 运行日志失败: {}", ignore.getMessage());
                                    }
                                }
                                // 更新偏移（防止重复）
                                if (logDto.getOffset() != null && logDto.getLength() != null) {
                                    logOffset = logDto.getOffset() + logDto.getLength();
                                }
                            }
                        } catch (Exception e) {
                            log.warn("获取或记录 Spark 日志时发生异常 appId={}, err={}", appId, e.getMessage());
                        }

                        // 再次判断状态以决定是否结束循环
                        R<SparkTaskConsumer.SparkTaskStatusDTO> statusResp2 = taskConsumer.queryTaskStatus(appId);
                        if (statusResp2 == null) {
                            Thread.sleep(TimeUnit.SECONDS.toMillis(pollIntervalSeconds));
                            continue;
                        }

                        SparkTaskConsumer.SparkTaskStatusDTO statusDto2 = statusResp2.getData();
                        if (statusDto2 == null) {
                            Thread.sleep(TimeUnit.SECONDS.toMillis(pollIntervalSeconds));
                            continue;
                        }

                        String state2 = statusDto2.getStatus();
                        if (state2 == null) {
                            Thread.sleep(TimeUnit.SECONDS.toMillis(pollIntervalSeconds));
                            continue;
                        }

                        String s = state2.trim().toUpperCase();
                        if (s.equals("FINISHED") || s.equals("SUCCEEDED") || s.equals("SUCCESS")) {
                            log.info("Spark 任务完成成功 appId={}", appId);
                            try {
                                nodeLogService.append(NodeLog.of(
                                        taskId,
                                        stepId,
                                        stepName,
                                        "INFO",
                                        "Spark 任务完成成功",
                                        Map.of("appId", appId, "finalState", state2)
                                ));
                            } catch (Exception ignore) {
                                log.warn("记录 SparkNode 成功日志失败: {}", ignore.getMessage());
                            }
                            return;
                        } else if (s.equals("FAILED") || s.equals("KILLED") || s.equals("ERROR") || s.equals("FAILED_WITH_ERRORS")) {
                            RuntimeException re = new RuntimeException("Spark 任务失败 appId=" + appId + ", state=" + state2 + ", message=" + statusDto2.getStatusDesc());
                            String stack = StackTraceUtils.getStackTrace(re);
                            if (stack.length() > 4000) stack = stack.substring(0, 4000) + "...";
                            try {
                                nodeLogService.append(NodeLog.of(
                                        taskId,
                                        stepId,
                                        stepName,
                                        "ERROR",
                                        "Spark 任务失败: " + re.getMessage(),
                                        Map.of(
                                                "appId", appId,
                                                "state", state2,
                                                "statusDesc", statusDto2.getStatusDesc() == null ? "" : statusDto2.getStatusDesc(),
                                                "stackTrace", stack
                                        )
                                ));
                            } catch (Exception ignore) {
                                log.warn("记录 SparkNode 失败日志失败: {}", ignore.getMessage());
                            }
                            throw re;
                        } else {
                            // 非终态，继续轮询
                            Thread.sleep(TimeUnit.SECONDS.toMillis(pollIntervalSeconds));
                        }
                    }
                } else {
                    // 如果是异步调用，则在后台线程监控并持续获取日志
                    final long bgMaxWaitSeconds = maxWaitSeconds; // 可根据需要调整
                    final long bgDeadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(bgMaxWaitSeconds);
                    final long initialLogOffset = logOffset;

                    CompletableFuture.runAsync(() -> {
                        long bgOffset = initialLogOffset;
                        try {
                            SparkTaskConsumer bgTaskConsumer = new SparkTaskConsumer(computeEngine);
                            while (true) {
                                if (System.currentTimeMillis() > bgDeadline) {
                                    log.warn("后台监控 Spark 任务超时 appId={}", appId);
                                    try {
                                        nodeLogService.append(NodeLog.of(
                                                taskId,
                                                stepId,
                                                stepName,
                                                "WARN",
                                                "后台监控 Spark 任务超时",
                                                Map.of("appId", appId)
                                        ));
                                    } catch (Exception ignore) {
                                        log.warn("记录 SparkNode 后台超时日志失败: {}", ignore.getMessage());
                                    }
                                    break;
                                }

                                try {
                                    R<SparkTaskConsumer.SparkTaskStatusDTO> statusResp = bgTaskConsumer.queryTaskStatus(appId);
                                    if (statusResp == null) {
                                        log.warn("后台 queryTaskStatus 返回 null, appId={}, {}s 后重试", appId, pollIntervalSeconds);
                                    } else {
                                        SparkTaskConsumer.SparkTaskStatusDTO statusDto = statusResp.getData();
                                        if (statusDto != null) {
                                            String state = statusDto.getStatus();
                                            log.info("后台监控任务状态 appId={}, state={}, message={}", appId, state, statusDto.getStatusDesc());
                                            try {
                                                nodeLogService.append(NodeLog.of(
                                                        taskId,
                                                        stepId,
                                                        stepName,
                                                        "INFO",
                                                        "后台监控任务状态",
                                                        Map.of(
                                                                "appId", appId,
                                                                "state", state == null ? "" : state,
                                                                "message", statusDto.getStatusDesc() == null ? "" : statusDto.getStatusDesc()
                                                        )
                                                ));
                                            } catch (Exception ignore) {
                                                log.warn("记录 SparkNode 后台状态日志失败: {}", ignore.getMessage());
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    log.warn("后台查询任务状态异常 appId={}, err={}", appId, e.getMessage());
                                }

                                // 拉取增量日志
                                try {
                                    R<SparkTaskConsumer.LogContentDTO> logResp = bgTaskConsumer.getLogContent(appId, bgOffset, fetchLen);
                                    if (logResp != null && logResp.getData() != null) {
                                        SparkTaskConsumer.LogContentDTO logDto = logResp.getData();
                                        String content = logDto.getLogContent();
                                        if (content != null && !content.isEmpty()) {
                                            try {
                                                nodeLogService.append(NodeLog.of(
                                                        taskId,
                                                        stepId,
                                                        stepName,
                                                        "INFO",
                                                        "Spark 运行日志（后台）",
                                                        Map.of(
                                                                "appId", appId,
                                                                "offset", String.valueOf(logDto.getOffset()),
                                                                "length", String.valueOf(logDto.getLength()),
                                                                "contentPreview", content.length() > 1000 ? content.substring(0, 1000) + "..." : content
                                                        )
                                                ));
                                            } catch (Exception ignore) {
                                                log.warn("记录 SparkNode 后台运行日志失败: {}", ignore.getMessage());
                                            }
                                        }
                                        if (logDto.getOffset() != null && logDto.getLength() != null) {
                                            bgOffset = logDto.getOffset() + logDto.getLength();
                                        }
                                    }
                                } catch (Exception e) {
                                    log.warn("后台获取或记录 Spark 日志时发生异常 appId={}, err={}", appId, e.getMessage());
                                }

                                // 检查终态
                                try {
                                    R<SparkTaskConsumer.SparkTaskStatusDTO> statusResp2 = bgTaskConsumer.queryTaskStatus(appId);
                                    if (statusResp2 != null && statusResp2.getData() != null) {
                                        SparkTaskConsumer.SparkTaskStatusDTO sd = statusResp2.getData();
                                        String state = sd.getStatus();
                                        if (state != null) {
                                            String s = state.trim().toUpperCase();
                                            if (s.equals("FINISHED") || s.equals("SUCCEEDED") || s.equals("SUCCESS")) {
                                                log.info("后台监控 Spark 任务完成成功 appId={}", appId);
                                                try {
                                                    nodeLogService.append(NodeLog.of(
                                                            taskId,
                                                            stepId,
                                                            stepName,
                                                            "INFO",
                                                            "后台监控 Spark 任务完成成功",
                                                            Map.of("appId", appId, "finalState", state)
                                                    ));
                                                } catch (Exception ignore) {
                                                    log.warn("记录 SparkNode 后台完成日志失败: {}", ignore.getMessage());
                                                }
                                                break;
                                            } else if (s.equals("FAILED") || s.equals("KILLED") || s.equals("ERROR") || s.equals("FAILED_WITH_ERRORS")) {
                                                log.error("后台监控 Spark 任务失败 appId={}, state={}, message={}", appId, state, sd.getStatusDesc());
                                                try {
                                                    nodeLogService.append(NodeLog.of(
                                                            taskId,
                                                            stepId,
                                                            stepName,
                                                            "ERROR",
                                                            "后台监控 Spark 任务失败",
                                                            Map.of(
                                                                    "appId", appId,
                                                                    "state", state,
                                                                    "message", sd.getStatusDesc() == null ? "" : sd.getStatusDesc()
                                                            )
                                                    ));
                                                } catch (Exception ignore) {
                                                    log.warn("记录 SparkNode 后台失败日志失败: {}", ignore.getMessage());
                                                }
                                                break;
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    log.warn("后台查询任务终态时发生异常 appId={}, err={}", appId, e.getMessage());
                                }

                                Thread.sleep(TimeUnit.SECONDS.toMillis(pollIntervalSeconds));
                            }
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.warn("后台监控线程被中断 appId={}", appId);
                            try {
                                nodeLogService.append(NodeLog.of(
                                        taskId,
                                        stepId,
                                        stepName,
                                        "WARN",
                                        "后台监控线程被中断",
                                        Map.of("appId", appId)
                                ));
                            } catch (Exception ignore) {
                                log.warn("记录 SparkNode 后台中断日志失败: {}", ignore.getMessage());
                            }
                        } catch (Exception e) {
                            log.error("后台监控 Spark 任务时发生异常 appId={}", appId, e);
                            try {
                                String stack = StackTraceUtils.getStackTrace(e);
                                if (stack.length() > 4000) stack = stack.substring(0, 4000) + "...";
                                nodeLogService.append(NodeLog.of(
                                        taskId,
                                        stepId,
                                        stepName,
                                        "ERROR",
                                        "后台监控 Spark 任务异常: " + e.getMessage(),
                                        Map.of("appId", appId, "stackTrace", stack)
                                ));
                            } catch (Exception ignore) {
                                log.warn("记录 SparkNode 后台异常日志失败: {}", ignore.getMessage());
                            }
                        }
                    });
                }

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                RuntimeException re = new RuntimeException("任务轮询线程被中断", ie);
                try {
                    nodeLogService.append(NodeLog.of(
                            taskId,
                            stepId,
                            stepName,
                            "ERROR",
                            "任务轮询线程被中断",
                            Map.of("nodeType", "spark")
                    ));
                } catch (Exception ignore) {
                    log.warn("记录 SparkNode 中断日志失败: {}", ignore.getMessage());
                }
                throw re;
            } catch (RuntimeException re) {
                // 记录运行时异常日志（容错）
                try {
                    String stack = StackTraceUtils.getStackTrace(re);
                    if (stack.length() > 4000) stack = stack.substring(0, 4000) + "...";
                    nodeLogService.append(NodeLog.of(
                            taskId,
                            stepId,
                            stepName,
                            "ERROR",
                            "Spark 节点执行异常: " + re.getMessage(),
                            Map.of("nodeType", "spark", "stackTrace", stack)
                    ));
                } catch (Exception ignore) {
                    log.warn("记录 SparkNode 运行时异常日志失败: {}", ignore.getMessage());
                }
                throw re;
            } catch (Exception ex) {
                // 记录未知异常日志（容错）
                try {
                    String stack = StackTraceUtils.getStackTrace(ex);
                    if (stack.length() > 4000) stack = stack.substring(0, 4000) + "...";
                    nodeLogService.append(NodeLog.of(
                            taskId,
                            stepId,
                            stepName,
                            "ERROR",
                            "执行 pyspark 节点时发生异常",
                            Map.of("nodeType", "spark", "exception", ex.getMessage(), "stackTrace", stack)
                    ));
                } catch (Exception ignore) {
                    log.warn("记录 SparkNode 异常日志失败: {}", ignore.getMessage());
                }
                throw new RuntimeException("执行 pyspark 节点时发生异常", ex);
            }
        });
    }

    /**
     * 获取 Spark SQL 节点的 CompletableFuture
     * @param computeEngine
     * @param nodeData
     * @return
     */
    private CompletableFuture<Void> getSparkSqlCompletableFuture(ComputeEngineEntity computeEngine, SparkNodeData nodeData) {
        String taskId = String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown");
        String stepId = node != null ? node.getId() : "unknown";
        String stepName = node != null ? node.getStepName() : "unknown";

        // 记录开始日志（容错）
        try {
            nodeLogService.append(NodeLog.of(
                    taskId,
                    stepId,
                    stepName,
                    "INFO",
                    "Spark SQL 节点开始执行",
                    Map.of("nodeType", "spark", "runType", "spark-sql")
            ));
        } catch (Exception ignore) {
            log.warn("记录 Spark SQL 开始日志失败: {}", ignore.getMessage());
        }

        return CompletableFuture.runAsync(() -> {
            try {
                // 从原始 node_data JSON 中弹性获取 SQL 内容和是否为文件标志（兼容多种字段名）
                String nodeDataJson = String.valueOf(node.getProperties().get("node_data"));
                JSONObject nodeObj = JSONObject.parseObject(nodeDataJson == null ? "{}" : nodeDataJson);

                String sql = null;
                // 尝试多个常见字段名
                if (nodeObj.containsKey("sql")) sql = nodeObj.getString("sql");
                if ((sql == null || sql.trim().isEmpty()) && nodeObj.containsKey("sqlContent")) sql = nodeObj.getString("sqlContent");
                if ((sql == null || sql.trim().isEmpty()) && nodeObj.containsKey("sparkSql")) sql = nodeObj.getString("sparkSql");
                if ((sql == null || sql.trim().isEmpty()) && nodeObj.containsKey("sparkSqlContent")) sql = nodeObj.getString("sparkSqlContent");
                // 如果是 sqlFile 模式，sql 字段 可能 存储文件名或路径
                boolean isSqlFile = false;
                if (nodeObj.containsKey("isSqlFile")) isSqlFile = Boolean.TRUE.equals(nodeObj.getBoolean("isSqlFile"));
                if (!isSqlFile && nodeObj.containsKey("sqlFile")) isSqlFile = Boolean.TRUE.equals(nodeObj.getBoolean("sqlFile"));

                // 备用：如果没有从 JSON 中得到内容，尝试从 nodeData (对象) 中获取 preview 字段（保持兼容，不强依赖）
                if ((sql == null || sql.trim().isEmpty())) {
                    try {
                        // 试图通过反射获取常见 getter（若类定义了则可使用）
                        java.lang.reflect.Method m = nodeData.getClass().getMethod("getSql");
                        Object o = m.invoke(nodeData);
                        if (o != null) sql = String.valueOf(o);
                    } catch (Throwable ignored) {
                    }
                }

                if (sql == null || sql.trim().isEmpty()) {
                    RuntimeException re = new RuntimeException("Spark SQL 内容为空，无法提交任务");
                    try {
                        nodeLogService.append(NodeLog.of(
                                taskId,
                                stepId,
                                stepName,
                                "ERROR",
                                "提交失败: Spark SQL 内容为空",
                                Map.of("nodeType", "spark", "runType", "spark-sql")
                        ));
                    } catch (Exception ignore) {
                        log.warn("记录 Spark SQL 错误日志失败: {}", ignore.getMessage());
                    }
                    throw re;
                }

                String sqlTextNoSec = CommonsTextSecrets.replace(sql , orgSecret) ;
                String sqlScript = replacePlaceholders(sqlTextNoSec);


                // 发送提交请求
                SparkSqlConsumer sqlConsumer = new SparkSqlConsumer(computeEngine);
                R<BaseSparkConsumer.SubmitRespDto> submitResp;

                // 如果配置为异步，尝试使用 submitAsync（此方法会立即返回），否则使用 raw 提交并等待 waitMs
                if (nodeData.isAsync()) {
                    String apiToken = nodeObj.containsKey("user") ? nodeObj.getString("user") : nodeObj.getString("apiToken");
                    // submitAsync 接受 sqlOrFile, isSqlFile, apiToke
                    submitResp = sqlConsumer.submitAsync(sqlScript, isSqlFile, apiToken);
                } else {
                    // 同步等待，使用一个合理的 waitMs（毫秒），这里采用 60s
                    submitResp = sqlConsumer.submitRaw(sqlScript, 60_000L);
                }

                if (submitResp == null) {
                    RuntimeException re = new RuntimeException("提交 Spark SQL 任务返回 null");
                    try {
                        nodeLogService.append(NodeLog.of(
                                taskId,
                                stepId,
                                stepName,
                                "ERROR",
                                "提交 Spark SQL 任务返回 null",
                                Map.of("nodeType", "spark", "runType", "spark-sql")
                        ));
                    } catch (Exception ignore) {
                        log.warn("记录 Spark SQL 错误日志失败: {}", ignore.getMessage());
                    }
                    throw re;
                }

                BaseSparkConsumer.SubmitRespDto submitData = submitResp.getData();
                String appId = submitData != null ? submitData.getApplicationId() : null;
                String submitStatus = submitData != null ? submitData.getStatus() : null;

                if (appId == null || appId.trim().isEmpty()) {
                    // 对于异步提交通常会返回 appId；若没有返回，记录并结束（或抛出）
                    try {
                        nodeLogService.append(NodeLog.of(
                                taskId,
                                stepId,
                                stepName,
                                "WARN",
                                "Spark SQL 提交返回无 appId",
                                Map.of("nodeType", "spark", "submitStatus", submitStatus == null ? "" : submitStatus)
                        ));
                    } catch (Exception ignore) {
                        log.warn("记录 Spark SQL 提交警告日志失败: {}", ignore.getMessage());
                    }
                    // 若是同步提交且没有 appId，直接返回完成
                    if (nodeData.isAsync()) {
                        RpcServiceRuntimeException ex = new RpcServiceRuntimeException("提交成功但未返回 applicationId，无法跟踪任务状态");
                        try {
                            nodeLogService.append(NodeLog.of(
                                    taskId,
                                    stepId,
                                    stepName,
                                    "ERROR",
                                    "提交成功但未返回 applicationId，无法跟踪任务状态",
                                    Map.of("nodeType", "spark")
                            ));
                        } catch (Exception ignore) {
                            log.warn("记录 Spark SQL 错误日志失败: {}", ignore.getMessage());
                        }
                        throw ex;
                    } else {
                        // 同步且无 appId，认为提交已完成
                        try {
                            nodeLogService.append(NodeLog.of(
                                    taskId,
                                    stepId,
                                    stepName,
                                    "INFO",
                                    "Spark SQL 同步提交已返回（无 appId）",
                                    Map.of("nodeType", "spark", "submitStatus", submitStatus == null ? "" : submitStatus)
                            ));
                        } catch (Exception ignore) {
                            log.warn("记录 Spark SQL 完成日志失败: {}", ignore.getMessage());
                        }
                        return;
                    }
                }

                log.info("spark-sql 提交成功 appId={}, submitStatus={}", appId, submitStatus);
                try {
                    nodeLogService.append(NodeLog.of(
                            taskId,
                            stepId,
                            stepName,
                            "INFO",
                            "spark-sql 提交成功",
                            Map.of("nodeType", "spark", "appId", appId, "submitStatus", submitStatus == null ? "" : submitStatus)
                    ));
                } catch (Exception ignore) {
                    log.warn("记录 Spark SQL 提交日志失败: {}", ignore.getMessage());
                }

                // 如果是异步提交且调用者希望立即返回，则不阻塞当前线程；若需要后台监控则同 pyspark 那样启动 bg 线程
                final int pollIntervalSeconds = 5;
                final long maxWaitSeconds = 3600L;
                long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(maxWaitSeconds);

                SparkTaskConsumer taskConsumer = new SparkTaskConsumer(computeEngine);
                final int fetchLen = 8192;
                long logOffset = 0L;

                if (!nodeData.isAsync()) {
                    // 同步等待任务终态并实时抓取日志
                    while (true) {
                        if (System.currentTimeMillis() > deadline) {
                            RuntimeException re = new RuntimeException("等待 Spark SQL 任务达到终态超时 appId=" + appId);
                            try {
                                nodeLogService.append(NodeLog.of(
                                        taskId,
                                        stepId,
                                        stepName,
                                        "ERROR",
                                        "等待 Spark SQL 任务达到终态超时",
                                        Map.of("appId", appId)
                                ));
                            } catch (Exception ignore) {
                                log.warn("记录 Spark SQL 超时日志失败: {}", ignore.getMessage());
                            }
                            throw re;
                        }

                        // 查询状态
                        R<SparkTaskConsumer.SparkTaskStatusDTO> statusResp = taskConsumer.queryTaskStatus(appId);
                        if (statusResp != null && statusResp.getData() != null) {
                            SparkTaskConsumer.SparkTaskStatusDTO statusDto = statusResp.getData();
                            String state = statusDto.getStatus();
                            try {
                                nodeLogService.append(NodeLog.of(
                                        taskId,
                                        stepId,
                                        stepName,
                                        "INFO",
                                        "查询任务状态",
                                        Map.of("appId", appId, "state", state == null ? "" : state, "message", statusDto.getStatusDesc() == null ? "" : statusDto.getStatusDesc())
                                ));
                            } catch (Exception ignore) {
                                log.warn("记录 Spark SQL 状态日志失败: {}", ignore.getMessage());
                            }
                        } else {
                            log.info("queryTaskStatus 返回 null 或无 data, appId={}, {}s 后重试", appId, pollIntervalSeconds);
                        }

                        // 获取日志增量
                        try {
                            R<SparkTaskConsumer.LogContentDTO> logResp = taskConsumer.getLogContent(appId, logOffset, fetchLen);
                            if (logResp != null && logResp.getData() != null) {
                                SparkTaskConsumer.LogContentDTO logDto = logResp.getData();
                                String content = logDto.getLogContent();
                                if (content != null && !content.isEmpty()) {
                                    try {
                                        nodeLogService.append(NodeLog.of(
                                                taskId,
                                                stepId,
                                                stepName,
                                                "INFO",
                                                "Spark SQL 运行日志",
                                                Map.of(
                                                        "appId", appId,
                                                        "offset", String.valueOf(logDto.getOffset()),
                                                        "length", String.valueOf(logDto.getLength()),
                                                        "contentPreview", content.length() > 1000 ? content.substring(0, 1000) + "..." : content
                                                )
                                        ));
                                    } catch (Exception ignore) {
                                        log.warn("记录 Spark SQL 运行日志失败: {}", ignore.getMessage());
                                    }
                                }
                                if (logDto.getOffset() != null && logDto.getLength() != null) {
                                    logOffset = logDto.getOffset() + logDto.getLength();
                                }
                            }
                        } catch (Exception e) {
                            log.warn("获取或记录 Spark SQL 日志时发生异常 appId={}, err={}", appId, e.getMessage());
                        }

                        // 再次判断终态
                        R<SparkTaskConsumer.SparkTaskStatusDTO> statusResp2 = taskConsumer.queryTaskStatus(appId);
                        if (statusResp2 == null || statusResp2.getData() == null) {
                            Thread.sleep(TimeUnit.SECONDS.toMillis(pollIntervalSeconds));
                            continue;
                        }

                        SparkTaskConsumer.SparkTaskStatusDTO statusDto2 = statusResp2.getData();
                        String state2 = statusDto2.getStatus();
                        if (state2 == null) {
                            Thread.sleep(TimeUnit.SECONDS.toMillis(pollIntervalSeconds));
                            continue;
                        }

                        String s = state2.trim().toUpperCase();
                        if (s.equals("FINISHED") || s.equals("SUCCEEDED") || s.equals("SUCCESS")) {
                            try {
                                nodeLogService.append(NodeLog.of(
                                        taskId,
                                        stepId,
                                        stepName,
                                        "INFO",
                                        "Spark SQL 任务完成成功",
                                        Map.of("appId", appId, "finalState", state2)
                                ));
                            } catch (Exception ignore) {
                                log.warn("记录 Spark SQL 成功日志失败: {}", ignore.getMessage());
                            }
                            return;
                        } else if (s.equals("FAILED") || s.equals("KILLED") || s.equals("ERROR") || s.equals("FAILED_WITH_ERRORS")) {
                            RuntimeException re = new RuntimeException("Spark SQL 任务失败 appId=" + appId + ", state=" + state2 + ", message=" + statusDto2.getStatusDesc());
                            String stack = StackTraceUtils.getStackTrace(re);
                            if (stack.length() > 4000) stack = stack.substring(0, 4000) + "...";
                            try {
                                nodeLogService.append(NodeLog.of(
                                        taskId,
                                        stepId,
                                        stepName,
                                        "ERROR",
                                        "Spark SQL 任务失败: " + re.getMessage(),
                                        Map.of("appId", appId, "state", state2, "statusDesc", statusDto2.getStatusDesc() == null ? "" : statusDto2.getStatusDesc(), "stackTrace", stack)
                                ));
                            } catch (Exception ignore) {
                                log.warn("记录 Spark SQL 失败日志失败: {}", ignore.getMessage());
                            }
                            throw re;
                        } else {
                            Thread.sleep(TimeUnit.SECONDS.toMillis(pollIntervalSeconds));
                        }
                    }
                } else {
                    // 异步提交：在后台线程持续获取日志并监控终态（不阻塞当前线程）
                    final long bgMaxWaitSeconds = maxWaitSeconds;
                    final long bgDeadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(bgMaxWaitSeconds);
                    final long initialLogOffset = logOffset;

                    CompletableFuture.runAsync(() -> {
                        long bgOffset = initialLogOffset;
                        try {
                            SparkTaskConsumer bgTaskConsumer = new SparkTaskConsumer(computeEngine);
                            while (true) {
                                if (System.currentTimeMillis() > bgDeadline) {
                                    log.warn("后台监控 Spark SQL 任务超时 appId={}", appId);
                                    try {
                                        nodeLogService.append(NodeLog.of(
                                                taskId,
                                                stepId,
                                                stepName,
                                                "WARN",
                                                "后台监控 Spark SQL 任务超时",
                                                Map.of("appId", appId)
                                        ));
                                    } catch (Exception ignore) {
                                        log.warn("记录 Spark SQL 后台超时日志失败: {}", ignore.getMessage());
                                    }
                                    break;
                                }

                                // 拉取状态并记录
                                try {
                                    R<SparkTaskConsumer.SparkTaskStatusDTO> statusRespBg = bgTaskConsumer.queryTaskStatus(appId);
                                    if (statusRespBg != null && statusRespBg.getData() != null) {
                                        SparkTaskConsumer.SparkTaskStatusDTO sd = statusRespBg.getData();
                                        String state = sd.getStatus();
                                        try {
                                            nodeLogService.append(NodeLog.of(
                                                    taskId,
                                                    stepId,
                                                    stepName,
                                                    "INFO",
                                                    "后台监控任务状态",
                                                    Map.of("appId", appId, "state", state == null ? "" : state, "message", sd.getStatusDesc() == null ? "" : sd.getStatusDesc())
                                            ));
                                        } catch (Exception ignore) {
                                            log.warn("记录 Spark SQL 后台状态日志失败: {}", ignore.getMessage());
                                        }
                                    }
                                } catch (Exception e) {
                                    log.warn("后台 queryTaskStatus 异常 appId={}, err={}", appId, e.getMessage());
                                }

                                // 拉取日志增量
                                try {
                                    R<SparkTaskConsumer.LogContentDTO> logResp = bgTaskConsumer.getLogContent(appId, bgOffset, fetchLen);
                                    if (logResp != null && logResp.getData() != null) {
                                        SparkTaskConsumer.LogContentDTO logDto = logResp.getData();
                                        String content = logDto.getLogContent();
                                        if (content != null && !content.isEmpty()) {
                                            try {
                                                nodeLogService.append(NodeLog.of(
                                                        taskId,
                                                        stepId,
                                                        stepName,
                                                        "INFO",
                                                        "Spark SQL 运行日志（后台）",
                                                        Map.of("appId", appId, "offset", String.valueOf(logDto.getOffset()), "length", String.valueOf(logDto.getLength()), "contentPreview", content.length() > 1000 ? content.substring(0, 1000) + "..." : content)
                                                ));
                                            } catch (Exception ignore) {
                                                log.warn("记录 Spark SQL 后台运行日志失败: {}", ignore.getMessage());
                                            }
                                        }
                                        if (logDto.getOffset() != null && logDto.getLength() != null) {
                                            bgOffset = logDto.getOffset() + logDto.getLength();
                                        }
                                    }
                                } catch (Exception e) {
                                    log.warn("后台获取或记录 Spark SQL 日志时发生异常 appId={}, err={}", appId, e.getMessage());
                                }

                                // 检查终态
                                try {
                                    R<SparkTaskConsumer.SparkTaskStatusDTO> statusResp2 = bgTaskConsumer.queryTaskStatus(appId);
                                    if (statusResp2 != null && statusResp2.getData() != null) {
                                        SparkTaskConsumer.SparkTaskStatusDTO sd = statusResp2.getData();
                                        String state = sd.getStatus();
                                        if (state != null) {
                                            String s = state.trim().toUpperCase();
                                            if (s.equals("FINISHED") || s.equals("SUCCEEDED") || s.equals("SUCCESS")) {
                                                try {
                                                    nodeLogService.append(NodeLog.of(
                                                            taskId,
                                                            stepId,
                                                            stepName,
                                                            "INFO",
                                                            "后台监控 Spark SQL 任务完成成功",
                                                            Map.of("appId", appId, "finalState", state)
                                                    ));
                                                } catch (Exception ignore) {
                                                    log.warn("记录 Spark SQL 后台完成日志失败: {}", ignore.getMessage());
                                                }
                                                break;
                                            } else if (s.equals("FAILED") || s.equals("KILLED") || s.equals("ERROR") || s.equals("FAILED_WITH_ERRORS")) {
                                                try {
                                                    nodeLogService.append(NodeLog.of(
                                                            taskId,
                                                            stepId,
                                                            stepName,
                                                            "ERROR",
                                                            "后台监控 Spark SQL 任务失败",
                                                            Map.of("appId", appId, "state", state, "message", sd.getStatusDesc() == null ? "" : sd.getStatusDesc())
                                                    ));
                                                } catch (Exception ignore) {
                                                    log.warn("记录 Spark SQL 后台失败日志失败: {}", ignore.getMessage());
                                                }
                                                break;
                                            }
                                        }
                                    }
                                } catch (Exception e) {
                                    log.warn("后台查询任务终态时发生异常 appId={}, err={}", appId, e.getMessage());
                                }

                                Thread.sleep(TimeUnit.SECONDS.toMillis(pollIntervalSeconds));
                            }
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            log.warn("Spark SQL 后台监控线程被中断 appId={}", appId);
                            try {
                                nodeLogService.append(NodeLog.of(
                                        taskId,
                                        stepId,
                                        stepName,
                                        "WARN",
                                        "Spark SQL 后台监控线程被中断",
                                        Map.of("appId", appId)
                                ));
                            } catch (Exception ignore) {
                                log.warn("记录 Spark SQL 后台中断日志失败: {}", ignore.getMessage());
                            }
                        } catch (Exception e) {
                            log.error("Spark SQL 后台监控异常 appId={}", appId, e);
                            try {
                                String stack = StackTraceUtils.getStackTrace(e);
                                if (stack.length() > 4000) stack = stack.substring(0, 4000) + "...";
                                nodeLogService.append(NodeLog.of(
                                        taskId,
                                        stepId,
                                        stepName,
                                        "ERROR",
                                        "Spark SQL 后台监控异常: " + e.getMessage(),
                                        Map.of("appId", appId, "stackTrace", stack)
                                ));
                            } catch (Exception ignore) {
                                log.warn("记录 Spark SQL 后台异常日志失败: {}", ignore.getMessage());
                            }
                        }
                    });
                }

            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                RuntimeException re = new RuntimeException("任务轮询线程被中断", ie);
                try {
                    nodeLogService.append(NodeLog.of(
                            taskId,
                            stepId,
                            stepName,
                            "ERROR",
                            "任务轮询线程被中断",
                            Map.of("nodeType", "spark", "runType", "spark-sql")
                    ));
                } catch (Exception ignore) {
                    log.warn("记录 Spark SQL 中断日志失败: {}", ignore.getMessage());
                }
                throw re;
            } catch (RuntimeException re) {
                try {
                    String stack = StackTraceUtils.getStackTrace(re);
                    if (stack.length() > 4000) stack = stack.substring(0, 4000) + "...";
                    nodeLogService.append(NodeLog.of(
                            taskId,
                            stepId,
                            stepName,
                            "ERROR",
                            "Spark SQL 节点执行异常: " + re.getMessage(),
                            Map.of("nodeType", "spark", "stackTrace", stack)
                    ));
                } catch (Exception ignore) {
                    log.warn("记录 Spark SQL 运行时异常日志失败: {}", ignore.getMessage());
                }
                throw re;
            } catch (Exception ex) {
                try {
                    String stack = StackTraceUtils.getStackTrace(ex);
                    if (stack.length() > 4000) stack = stack.substring(0, 4000) + "...";
                    nodeLogService.append(NodeLog.of(
                            taskId,
                            stepId,
                            stepName,
                            "ERROR",
                            "执行 spark-sql 节点时发生异常",
                            Map.of("nodeType", "spark", "exception", ex.getMessage(), "stackTrace", stack)
                    ));
                } catch (Exception ignore) {
                    log.warn("记录 Spark SQL 异常日志失败: {}", ignore.getMessage());
                }
                throw new RuntimeException("执行 spark-sql 节点时发生异常", ex);
            }
        });
    }

    private SparkNodeData getNodeData() {
        String nodeDataJson = String.valueOf(node.getProperties().get("node_data"));
        return JSONObject.parseObject(nodeDataJson, SparkNodeData.class);
    }
}