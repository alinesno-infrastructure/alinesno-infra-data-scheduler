package com.alinesno.infra.data.scheduler.workflow.nodes.step;

import cn.hutool.core.util.IdUtil;
import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.data.scheduler.adapter.SparkPythonConsumer;
import com.alinesno.infra.data.scheduler.adapter.SparkSqlConsumer;
import com.alinesno.infra.data.scheduler.entity.ComputeEngineEntity;
import com.alinesno.infra.data.scheduler.workflow.service.IComputeEngineService;
import com.alinesno.infra.data.scheduler.workflow.constants.FlowConst;
import com.alinesno.infra.data.scheduler.api.logger.NodeLog;
import com.alinesno.infra.data.scheduler.workflow.nodes.AbstractFlowNode;
import com.alinesno.infra.data.scheduler.workflow.nodes.variable.step.SparkNodeData;
import com.alinesno.infra.data.scheduler.workflow.utils.CommonsTextSecrets;
import com.alinesno.infra.data.scheduler.workflow.utils.SecretUtils;
import com.alinesno.infra.data.scheduler.workflow.utils.StackTraceUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

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

    // 下载与预览相关配置（可根据需要调整）
    private static final int DOWNLOAD_TIMEOUT_MS = 30_000;
    private static final int MAX_DOWNLOAD_BYTES = 5 * 1024 * 1024; // 最大下载 5MB
    private static final int RAW_PREVIEW_MAX = 1000;
    private static final int ERROR_PREVIEW_MAX = 3000;
    private static final int SMALL_BINARY_BASE64_MAX = 100 * 1024; // 小于 100KB 的二进制文件可以 base64 返回

    public SparkNode() {
        setType("spark");
    }

    @Override
    protected CompletableFuture<Void> handleNode() {

        ComputeEngineEntity computeEngine = computeEngineService.getCurrentConfig(flowExecution.getOrgId());
        SparkNodeData nodeData = getNodeData();

        String runType = nodeData.getRunType() ;

        if (runType.equals("spark-sql")) {
            return getSparkSqlCompletableFuture(computeEngine , nodeData);
        }else if(runType.equals("pyspark")){
            return getPySparkCompletableFuture(computeEngine , nodeData);
        }

        return CompletableFuture.completedFuture(null);
    }

    @NotNull
    private CompletableFuture<Void> getPySparkCompletableFuture(ComputeEngineEntity computeEngine, SparkNodeData nodeData) {
        long startTs = System.currentTimeMillis();
        try {
            log.debug("nodeData = {}", nodeData);
            log.debug("node type = {} output = {}", node.getType(), output);

            // 记录解析配置 & 计算引擎信息
            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "INFO",
                    "PySpark 节点配置解析完成",
                    Map.of(
                            "scriptLength", nodeData.getPysparkContent() != null ? nodeData.getPysparkContent().length() : 0,
                            "orgId", flowExecution.getOrgId(),
                            "computeEngineId", computeEngine != null ? String.valueOf(computeEngine.getId()) : "unknown",
                            "computeEngineAdmin", computeEngine != null ? computeEngine.getAdminUser() : "unknown"
                    )
            ));

            // 从 nodeData 中取脚本或脚本文件 URL（注意：字段名根据你实际 SparkNodeData 调整）
            String scriptOrFile = nodeData.getPysparkContent() ;
            if (scriptOrFile == null) scriptOrFile = "";

            // 是否异步（优先使用 nodeData 中的配置，否则默认 false -> 同步）
            boolean async = nodeData.isAsync(); // 如无此字段请替换为相应的 getter 或常量

            // 记录提交前 preview
            String scriptPreview = scriptOrFile.length() > 2000 ? scriptOrFile.substring(0, 2000) + "..." : scriptOrFile;
            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "INFO",
                    (async ? "准备异步提交 PySpark" : "准备同步提交 PySpark"),
                    Map.of("scriptPreview", scriptPreview)
            ));

            SparkPythonConsumer consumer = new SparkPythonConsumer(computeEngine);

            JSONObject resp;
            String commandReplace = CommonsTextSecrets.replace(scriptOrFile , getOrgSecret()) ;
            if (async) {
                resp = consumer.submitAsync(commandReplace , true , computeEngine != null ? computeEngine.getAdminUser() : null);
            } else {
                resp = consumer.submitSync(commandReplace , true , computeEngine != null ? computeEngine.getAdminUser() : null, 600_000);
            }

            // 检查命令中是否有未解析的密钥
            Set<String> unresolvedSecrets = SecretUtils.checkAndLogUnresolvedSecrets(commandReplace , node , flowExecution, nodeLogService) ;
            log.debug("未解析的密钥：{}" , unresolvedSecrets);

            long durationMs = System.currentTimeMillis() - startTs;

            if (resp == null) {
                nodeLogService.append(NodeLog.of(
                        flowExecution.getId().toString(),
                        node.getId(),
                        node.getStepName(),
                        "WARN",
                        "PySpark 返回空响应",
                        Map.of("durationMs", durationMs)
                ));
                output.put(node.getStepName() + ".result", "{}");
                output.put(node.getStepName() + ".rawResponsePreview", "{}");
            } else {
                String id = resp.getString("id") ;

                if(id == null){
                    id = IdUtil.getSnowflakeNextIdStr() ;
                }

                String status = resp.getString("status");
                String createdAt = resp.getString("createdAt");
                String startedAt = resp.getString("startedAt");
                String finishedAt = resp.getString("finishedAt");
                String resultPath = resp.getString("resultPath");
                String errorMessage = resp.getString("errorMessage");

                String errorPreview = null;
                if (errorMessage != null) {
                    errorPreview = errorMessage.length() > ERROR_PREVIEW_MAX ? errorMessage.substring(0, ERROR_PREVIEW_MAX) + "..." : errorMessage;
                }

                Map<String, Object> structured = new HashMap<>();
                if (id != null) structured.put("id", id);
                if (status != null) structured.put("status", status);
                if (createdAt != null) structured.put("createdAt", createdAt);
                if (startedAt != null) structured.put("startedAt", startedAt);
                if (finishedAt != null) structured.put("finishedAt", finishedAt);
                if (resultPath != null) structured.put("resultPath", resultPath);
                if (errorPreview != null) structured.put("errorMessagePreview", errorPreview);
                structured.put("async", async);

                String rawRespStr = resp.toJSONString();
                String rawPreview = rawRespStr.length() > RAW_PREVIEW_MAX ? rawRespStr.substring(0, RAW_PREVIEW_MAX) + "..." : rawRespStr;

                output.put(node.getStepName() + ".result", JSONObject.toJSONString(structured));
                output.put(node.getStepName() + ".rawResponsePreview", rawPreview);
                output.put(node.getStepName() + ".rawResponse", rawRespStr);

                if (!async) {
                    // 同步模式下，依据 status 做失败处理
                    if ("SUCCESS".equalsIgnoreCase(status)) {
                        nodeLogService.append(NodeLog.of(
                                flowExecution.getId().toString(),
                                node.getId(),
                                node.getStepName(),
                                "INFO",
                                "PySpark 同步提交完成（SUCCESS）",
                                Map.of("durationMs", durationMs, "id", id, "status", status, "resultPath", resultPath)
                        ));
                    } else {
                        nodeLogService.append(NodeLog.of(
                                flowExecution.getId().toString(),
                                node.getId(),
                                node.getStepName(),
                                "ERROR",
                                "PySpark 执行失败",
                                Map.of("durationMs", durationMs, "id", id, "status", status, "errorMessagePreview", errorPreview)
                        ));
                        log.error("PySpark 执行异常: {}", errorPreview);
                        CompletableFuture<Void> failed = new CompletableFuture<>();
                        failed.completeExceptionally(new Throwable(errorPreview));
                        return failed;
                    }
                } else {
                    // 异步：返回 task id 等信息，记录并结束（不当作失败）
                    nodeLogService.append(NodeLog.of(
                            flowExecution.getId().toString(),
                            node.getId(),
                            node.getStepName(),
                            "INFO",
                            "PySpark 异步提交返回",
                            Map.of("id", id, "status", status)
                    ));
                }

                // 如果有 resultPath 并且是 http/https，尝试下载（与 spark-sql 实现一致）
                if (resultPath != null && (resultPath.startsWith("http://") || resultPath.startsWith("https://"))) {
                    try {
                        URL url = new URL(resultPath);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(DOWNLOAD_TIMEOUT_MS);
                        conn.setReadTimeout(DOWNLOAD_TIMEOUT_MS);
                        conn.setRequestMethod("GET");
                        conn.setInstanceFollowRedirects(true);

                        int code = conn.getResponseCode();
                        if (code == HttpURLConnection.HTTP_OK) {
                            String contentType = conn.getContentType();
                            boolean isText = contentType != null && (
                                    contentType.startsWith("text") ||
                                            contentType.contains("json") ||
                                            contentType.contains("xml") ||
                                            contentType.contains("csv") ||
                                            contentType.contains("plain")
                            );

                            try (InputStream in = conn.getInputStream();
                                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                                byte[] buffer = new byte[8192];
                                int read;
                                int total = 0;
                                boolean truncated = false;
                                while ((read = in.read(buffer)) != -1) {
                                    if (total + read > MAX_DOWNLOAD_BYTES) {
                                        baos.write(buffer, 0, Math.max(0, MAX_DOWNLOAD_BYTES - total));
                                        truncated = true;
                                        break;
                                    } else {
                                        baos.write(buffer, 0, read);
                                    }
                                    total += read;
                                }
                                byte[] contentBytes = baos.toByteArray();
                                int downloadedSize = contentBytes.length;

                                structured.put("resultFileDownloaded", true);
                                structured.put("resultFileSize", downloadedSize);
                                structured.put("resultFileTruncated", truncated);
                                structured.put("resultFileContentType", contentType != null ? contentType : "unknown");

                                if (isText) {
                                    String text = new String(contentBytes, StandardCharsets.UTF_8);
                                    String preview = text.length() > RAW_PREVIEW_MAX ? text.substring(0, RAW_PREVIEW_MAX) + "..." : text;
                                    structured.put("resultFileContentPreview", preview);
                                    if (!truncated && downloadedSize <= MAX_DOWNLOAD_BYTES) {
                                        structured.put("resultFileContent", text);
                                        output.put(node.getStepName() + ".resultFileContent", text);
                                    }
                                } else {
                                    if (downloadedSize <= SMALL_BINARY_BASE64_MAX) {
                                        String b64 = Base64.getEncoder().encodeToString(contentBytes);
                                        structured.put("resultFileBase64", b64);
                                        output.put(node.getStepName() + ".resultFileBase64", b64);
                                    } else {
                                        structured.put("resultFileBase64", null);
                                    }
                                }

                                output.put(node.getStepName() + ".result", JSONObject.toJSONString(structured));
                                nodeLogService.append(NodeLog.of(
                                        flowExecution.getId().toString(),
                                        node.getId(),
                                        node.getStepName(),
                                        "INFO",
                                        "已从 HTTP(S) resultPath 下载文件（或部分下载）",
                                        Map.of("resultPath", resultPath, "downloadedSize", structured.get("resultFileSize"), "truncated", structured.get("resultFileTruncated"))
                                ));
                            }
                        } else {
                            structured.put("resultFileDownloaded", false);
                            structured.put("resultFileDownloadHttpCode", code);
                            output.put(node.getStepName() + ".result", JSONObject.toJSONString(structured));
                            nodeLogService.append(NodeLog.of(
                                    flowExecution.getId().toString(),
                                    node.getId(),
                                    node.getStepName(),
                                    "WARN",
                                    "从 resultPath 下载时 HTTP 响应非 200",
                                    Map.of("resultPath", resultPath, "httpCode", code)
                            ));
                        }
                    } catch (Exception dx) {
                        String errMsg = dx.getMessage() != null ? dx.getMessage() : "下载异常";
                        nodeLogService.append(NodeLog.of(
                                flowExecution.getId().toString(),
                                node.getId(),
                                node.getStepName(),
                                "ERROR",
                                "从 HTTP(S) resultPath 下载失败: " + errMsg,
                                Map.of("resultPath", resultPath, "exception", errMsg)
                        ));
                        structured.put("resultFileDownloaded", false);
                        structured.put("resultFileDownloadError", errMsg);
                        output.put(node.getStepName() + ".result", JSONObject.toJSONString(structured));
                    }
                } else if (resultPath != null) {
                    nodeLogService.append(NodeLog.of(
                            flowExecution.getId().toString(),
                            node.getId(),
                            node.getStepName(),
                            "INFO",
                            "注意：resultPath 指向远端文件系统路径，当前环境无法直接读取。如需获取，请在远端提供 HTTP/下载接口或通过 SSH/SCP 拉取。",
                            Map.of("resultPath", resultPath)
                    ));
                    structured.put("resultFileDownloaded", false);
                    structured.put("resultFileDownloadError", "resultPath not http/https (remote path)");
                    output.put(node.getStepName() + ".result", JSONObject.toJSONString(structured));
                }
            }

            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "INFO",
                    "PySpark 节点执行完成（已记录结构化结果与响应预览）",
                    Map.of("outputKey", node.getStepName() + ".result")
            ));

            return CompletableFuture.completedFuture(null);
        } catch (Exception ex) {
            String errMsg = ex.getMessage() != null ? ex.getMessage() : "未知异常";
            String stack = StackTraceUtils.getStackTrace(ex);
            if (stack.length() > 4000) stack = stack.substring(0, 4000) + "...";

            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "ERROR",
                    "PySpark 节点执行异常: " + errMsg,
                    Map.of("exception", errMsg, "stackTrace", stack)
            ));

            log.error("PySpark 执行异常: {}", ex.getMessage(), ex);
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(ex);
            return failed;
        }
    }

    /**
     *
     * @param computeEngine
     * @param nodeData
     * @return
     */
    @NotNull
    private CompletableFuture<Void> getSparkSqlCompletableFuture(ComputeEngineEntity computeEngine, SparkNodeData nodeData) {
        long startTs = System.currentTimeMillis();
        try {
            log.debug("nodeData = {}", nodeData);
            log.debug("node type = {} output = {}", node.getType(), output);

            // 记录解析配置 & 计算引擎信息
            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "INFO",
                    "Spark节点配置解析完成",
                    Map.of(
                            "sqlLength", nodeData.getSqlContent() != null ? nodeData.getSqlContent().length() : 0,
                            "orgId", flowExecution.getOrgId(),
                            "computeEngineId", computeEngine != null ? String.valueOf(computeEngine.getId()) : "unknown",
                            "computeEngineAdmin", computeEngine != null ? computeEngine.getAdminUser() : "unknown"
                    )
            ));

            String sqlContent = nodeData.getSqlContent();

            // 记录提交前（包含 sql 预览）
            String sqlPreview = sqlContent == null ? "" : (sqlContent.length() > 2000 ? sqlContent.substring(0, 2000) + "..." : sqlContent);
            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "INFO",
                    "准备提交 Spark SQL",
                    Map.of(
                            "sqlPreview", sqlPreview
                    )
            ));

            // 2. 创建 SparkSqlConsumer
            SparkSqlConsumer consumer = new SparkSqlConsumer(computeEngine);

            long waitMs = 600_000L; // 等待 600 秒
            Map<String, Object> params = new HashMap<>();

            // 提交并等待结果
            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "INFO",
                    "开始同步提交 Spark SQL",
                    Map.of(
                            "waitMs", waitMs,
                            "paramsSize", params.size()
                    )
            ));

            String commandReplace = CommonsTextSecrets.replace(sqlContent , getOrgSecret()) ;
            JSONObject syncResp = consumer.submitSync(commandReplace , params, computeEngine != null ? computeEngine.getAdminUser() : null, waitMs);

            // 检查命令中是否有未解析的密钥
            Set<String> unresolvedSecrets = SecretUtils.checkAndLogUnresolvedSecrets(commandReplace , node , flowExecution, nodeLogService) ;
            log.debug("未解析的密钥：{}" , unresolvedSecrets);

            // ------------- 改进后的 syncResp 处理 -------------
            long durationMs = System.currentTimeMillis() - startTs;

            if (syncResp == null) {
                // 空响应处理
                nodeLogService.append(NodeLog.of(
                        flowExecution.getId().toString(),
                        node.getId(),
                        node.getStepName(),
                        "WARN",
                        "Spark 返回空响应",
                        Map.of("durationMs", durationMs)
                ));
                output.put(node.getStepName() + ".result", "{}");
                output.put(node.getStepName() + ".rawResponsePreview", "{}");
            } else {
                // 提取常用字段
                String id = syncResp.getString("id");
                String status = syncResp.getString("status");
                String createdAt = syncResp.getString("createdAt");
                String startedAt = syncResp.getString("startedAt");
                String finishedAt = syncResp.getString("finishedAt");
                String resultPath = syncResp.getString("resultPath");
                String errorMessage = syncResp.getString("errorMessage");

                // 截断长文本（errorMessage）
                String errorPreview = null;
                if (errorMessage != null) {
                    errorPreview = errorMessage.length() > ERROR_PREVIEW_MAX ? errorMessage.substring(0, ERROR_PREVIEW_MAX) + "..." : errorMessage;
                }

                // 组装结构化结果（初始）
                Map<String, Object> structured = new HashMap<>();
                if (id != null) structured.put("id", id);
                if (status != null) structured.put("status", status);
                if (createdAt != null) structured.put("createdAt", createdAt);
                if (startedAt != null) structured.put("startedAt", startedAt);
                if (finishedAt != null) structured.put("finishedAt", finishedAt);
                if (resultPath != null) structured.put("resultPath", resultPath); // 仅路径字符串
                if (errorPreview != null) structured.put("errorMessagePreview", errorPreview);

                String rawRespStr = syncResp.toJSONString();
                String rawPreview = rawRespStr.length() > RAW_PREVIEW_MAX ? rawRespStr.substring(0, RAW_PREVIEW_MAX) + "..." : rawRespStr;

                output.put(node.getStepName() + ".result", JSONObject.toJSONString(structured));
                output.put(node.getStepName() + ".rawResponsePreview", rawPreview);
                output.put(node.getStepName() + ".rawResponse", rawRespStr); // 可选：可能很大

                // 日志记录：根据状态分别记录
                if ("SUCCESS".equalsIgnoreCase(status)) {
                    nodeLogService.append(NodeLog.of(
                            flowExecution.getId().toString(),
                            node.getId(),
                            node.getStepName(),
                            "INFO",
                            "Spark SQL 同步提交完成（SUCCESS）",
                            Map.of(
                                    "durationMs", durationMs,
                                    "id", id,
                                    "status", status,
                                    "createdAt", createdAt,
                                    "startedAt", startedAt,
                                    "finishedAt", finishedAt,
                                    "resultPath", resultPath
                            )
                    ));
                } else {
                    nodeLogService.append(NodeLog.of(
                            flowExecution.getId().toString(),
                            node.getId(),
                            node.getStepName(),
                            "ERROR",
                            "Spark SQL 执行失败",
                            Map.of(
                                    "durationMs", durationMs,
                                    "id", id,
                                    "status", status,
                                    "errorMessagePreview", errorPreview,
                                    "resultPath", resultPath
                            )
                    ));

                    // 返回异常处理
                    log.error("SparkNode 执行异常: {}", errorPreview);
                    CompletableFuture<Void> failed = new CompletableFuture<>();
                    failed.completeExceptionally(new Throwable(errorPreview));
                    return failed;
                }

                // 处理 resultPath：如果以 http/https 开头，尝试下载；否则说明无法直接读取（远端文件系统路径）
                if (resultPath.startsWith("http://") || resultPath.startsWith("https://")) {
                    // 尝试下载（带超时与大小限制）
                    try {
                        URL url = new URL(resultPath);
                        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                        conn.setConnectTimeout(DOWNLOAD_TIMEOUT_MS);
                        conn.setReadTimeout(DOWNLOAD_TIMEOUT_MS);
                        conn.setRequestMethod("GET");
                        conn.setInstanceFollowRedirects(true);

                        int code = conn.getResponseCode();
                        if (code == HttpURLConnection.HTTP_OK) {
                            String contentType = conn.getContentType();
                            boolean isText = contentType != null && (
                                    contentType.startsWith("text") ||
                                            contentType.contains("json") ||
                                            contentType.contains("xml") ||
                                            contentType.contains("csv") ||
                                            contentType.contains("plain")
                            );

                            try (InputStream in = conn.getInputStream();
                                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                                byte[] buffer = new byte[8192];
                                int read;
                                int total = 0;
                                boolean truncated = false;
                                while ((read = in.read(buffer)) != -1) {
                                    if (total + read > MAX_DOWNLOAD_BYTES) {
                                        // 只读取到最大限制
                                        baos.write(buffer, 0, Math.max(0, MAX_DOWNLOAD_BYTES - total));
                                        truncated = true;
                                        break;
                                    } else {
                                        baos.write(buffer, 0, read);
                                    }
                                    total += read;
                                }
                                byte[] contentBytes = baos.toByteArray();
                                int downloadedSize = contentBytes.length;

                                structured.put("resultFileDownloaded", true);
                                structured.put("resultFileSize", downloadedSize);
                                structured.put("resultFileTruncated", truncated);
                                structured.put("resultFileContentType", contentType != null ? contentType : "unknown");

                                if (isText) {
                                    String charset = "UTF-8";
                                    // 不严格解析 charset，这里默认 UTF-8
                                    String text = new String(contentBytes, StandardCharsets.UTF_8);
                                    String preview = text.length() > RAW_PREVIEW_MAX ? text.substring(0, RAW_PREVIEW_MAX) + "..." : text;
                                    structured.put("resultFileContentPreview", preview);
                                    // 如果未截断且大小较小，可以保存完整内容（注意内存）
                                    if (!truncated && downloadedSize <= MAX_DOWNLOAD_BYTES) {
                                        structured.put("resultFileContent", text);
                                        output.put(node.getStepName() + ".resultFileContent", text);
                                    }
                                } else {
                                    // 二进制文件：若很小则 base64 返回，否则仅返回 size 与标识
                                    if (downloadedSize <= SMALL_BINARY_BASE64_MAX) {
                                        String b64 = Base64.getEncoder().encodeToString(contentBytes);
                                        structured.put("resultFileBase64", b64);
                                        output.put(node.getStepName() + ".resultFileBase64", b64);
                                    } else {
                                        structured.put("resultFileBase64", null);
                                    }
                                }

                                // 将 updated structured 写回 output.result（覆盖 earlier）
                                output.put(node.getStepName() + ".result", JSONObject.toJSONString(structured));
                                nodeLogService.append(NodeLog.of(
                                        flowExecution.getId().toString(),
                                        node.getId(),
                                        node.getStepName(),
                                        "INFO",
                                        "已从 HTTP(S) resultPath 下载文件（或部分下载）",
                                        Map.of("resultPath", resultPath, "downloadedSize", structured.get("resultFileSize"), "truncated", structured.get("resultFileTruncated"))
                                ));
                            }
                        } else {
                            // 非 200 返回
                            structured.put("resultFileDownloaded", false);
                            structured.put("resultFileDownloadHttpCode", code);
                            output.put(node.getStepName() + ".result", JSONObject.toJSONString(structured));
                            nodeLogService.append(NodeLog.of(
                                    flowExecution.getId().toString(),
                                    node.getId(),
                                    node.getStepName(),
                                    "WARN",
                                    "从 resultPath 下载时 HTTP 响应非 200",
                                    Map.of("resultPath", resultPath, "httpCode", code)
                            ));
                        }
                    } catch (Exception dx) {
                        String errMsg = dx.getMessage() != null ? dx.getMessage() : "下载异常";
                        nodeLogService.append(NodeLog.of(
                                flowExecution.getId().toString(),
                                node.getId(),
                                node.getStepName(),
                                "ERROR",
                                "从 HTTP(S) resultPath 下载失败: " + errMsg,
                                Map.of("resultPath", resultPath, "exception", errMsg)
                        ));
                        structured.put("resultFileDownloaded", false);
                        structured.put("resultFileDownloadError", errMsg);
                        output.put(node.getStepName() + ".result", JSONObject.toJSONString(structured));
                    }
                } else {
                    // 非 http(s) 路径，无法直接下载（远程机器路径）
                    nodeLogService.append(NodeLog.of(
                            flowExecution.getId().toString(),
                            node.getId(),
                            node.getStepName(),
                            "INFO",
                            "注意：resultPath 指向远端文件系统路径，当前环境无法直接读取。如需获取，请在远端提供 HTTP/下载接口或通过 SSH/SCP 拉取。",
                            Map.of("resultPath", resultPath)
                    ));
                    structured.put("resultFileDownloaded", false);
                    structured.put("resultFileDownloadError", "resultPath not http/https (remote path)");
                    output.put(node.getStepName() + ".result", JSONObject.toJSONString(structured));
                }

            }

            // 成功结束日志（整体）
            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "INFO",
                    "Spark 节点执行完成（已记录结构化结果与响应预览）",
                    Map.of(
                            "outputKey", node.getStepName() + ".result"
                    )
            ));

            return CompletableFuture.completedFuture(null);
        } catch (Exception ex) {
            String errMsg = ex.getMessage() != null ? ex.getMessage() : "未知异常";
            String stack = StackTraceUtils.getStackTrace(ex);
            if (stack.length() > 4000) stack = stack.substring(0, 4000) + "...";

            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "ERROR",
                    "Spark 节点执行异常: " + errMsg,
                    Map.of(
                            "exception", errMsg,
                            "stackTrace", stack
                    )
            ));

            log.error("SparkNode 执行异常: {}", ex.getMessage(), ex);
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(ex);
            return failed;
        }
    }

    private SparkNodeData getNodeData() {
        String nodeDataJson = String.valueOf(node.getProperties().get("node_data"));
        return JSONObject.parseObject(nodeDataJson, SparkNodeData.class);
    }
}