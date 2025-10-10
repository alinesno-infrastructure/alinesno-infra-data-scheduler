package com.alinesno.infra.data.scheduler.workflow.nodes.step;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.data.scheduler.service.IDataSourceService;
import com.alinesno.infra.data.scheduler.workflow.constants.FlowConst;
import com.alinesno.infra.data.scheduler.workflow.logger.NodeLog;
import com.alinesno.infra.data.scheduler.workflow.nodes.AbstractFlowNode;
import com.alinesno.infra.data.scheduler.workflow.nodes.variable.step.SqlNodeData;
import com.alinesno.infra.data.scheduler.workflow.utils.CommonsTextSecrets;
import com.alinesno.infra.data.scheduler.workflow.utils.StackTraceUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;

import javax.lang.exception.RpcServiceRuntimeException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * SqlNode：增加了详细节点日志（NodeLog）
 */
@Slf4j
@Data
@Scope("prototype")
@Service(FlowConst.FLOW_STEP_NODE + "sql")
@EqualsAndHashCode(callSuper = true)
public class SqlNode extends AbstractFlowNode {

    @Autowired
    private IDataSourceService dataSourceService;

    public SqlNode() {
        setType("reply");
    }

    @Override
    protected CompletableFuture<Void> handleNode() {
        long startTs = System.currentTimeMillis();
        DruidDataSource druidDataSource = null;
        Connection connection = null;
        try {
            SqlNodeData nodeData = getNodeData();
            log.debug("nodeData = {}" , nodeData);
            log.debug("node type = {} output = {}" , node.getType() , output);

            // 配置解析日志
            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "INFO",
                    "SQL节点配置解析完成",
                    Map.of(
                            "dataSourceId", nodeData.getDataSourceId(),
                            "sqlContent", nodeData.getSqlContent(),
                            "sqlLength", nodeData.getSqlContent() != null ? nodeData.getSqlContent().length() : 0,
                            "printEnable", node.isPrint()
                    )
            ));

            Long datasourceId = nodeData.getDataSourceId();
            druidDataSource = dataSourceService.getDataSource(datasourceId);

            // 数据源信息日志（谨慎暴露敏感信息）
            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "INFO",
                    "获取数据源配置",
                    Map.of(
                            "dataSourceId", datasourceId,
                            "druidProps", druidDataSource != null ? druidDataSource.getProperties() : "null"
                    )
            ));

            outputContent.append("数据源ID：").append(datasourceId).append("\r\n");
            outputContent.append("数据源名称：").append(druidDataSource != null ? druidDataSource.getProperties() : "null").append("\r\n");

            String sqlContent = nodeData.getSqlContent();

            // 执行前日志
            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "INFO",
                    "准备执行 SQL 脚本",
                    Map.of(
                            "sqlPreview", sqlContent
                    )
            ));

            try {
                connection = druidDataSource.getConnection();
                ScriptUtils.executeSqlScript(connection, new ByteArrayResource(CommonsTextSecrets.replace(sqlContent , getOrgSecret()).getBytes()));
            } catch (SQLException e) {
                // 数据源获取或执行异常，包装并记录
                String errMsg = "获取或执行数据源异常: " + e.getMessage();
                nodeLogService.append(NodeLog.of(
                        flowExecution.getId().toString(),
                        node.getId(),
                        node.getStepName(),
                        "ERROR",
                        "SQL 执行异常",
                        Map.of(
                                "exception", errMsg,
                                "stackTrace", StackTraceUtils.getStackTrace(e)
                        )
                ));
                throw new RpcServiceRuntimeException(errMsg);
            } finally {
                // 释放连接（如果拿到）
                try {
                    DataSourceUtils.releaseConnection(connection, druidDataSource);
                } catch (Exception releaseEx) {
                    nodeLogService.append(NodeLog.of(
                            flowExecution.getId().toString(),
                            node.getId(),
                            node.getStepName(),
                            "WARN",
                            "释放数据库连接失败",
                            Map.of("exception", releaseEx.getMessage())
                    ));
                }
                // 关闭数据源（注意：如果数据源由连接池管理，关闭可能不合适；按原代码保持）
                try {
                    if (druidDataSource != null) druidDataSource.close();
                    nodeLogService.append(NodeLog.of(
                            flowExecution.getId().toString(),
                            node.getId(),
                            node.getStepName(),
                            "INFO",
                            "数据源已关闭",
                            Map.of("dataSourceId", datasourceId)
                    ));
                } catch (Exception closeEx) {
                    nodeLogService.append(NodeLog.of(
                            flowExecution.getId().toString(),
                            node.getId(),
                            node.getStepName(),
                            "WARN",
                            "关闭数据源失败",
                            Map.of("exception", closeEx.getMessage())
                    ));
                }
            }

            long durationMs = System.currentTimeMillis() - startTs;
            String sqlResult = sqlContent + "执行结果";
            output.put(node.getStepName() + ".sql_result", sqlResult);

            // 成功日志
            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "INFO",
                    "SQL 脚本执行完成",
                    Map.of(
                            "durationMs", durationMs,
                            "outputKey", node.getStepName() + ".sql_result",
                            "resultPreview", sqlResult
                    )
            ));

            if (node.isPrint()) {
                eventMessageCallbackMessage(sqlResult);
            }

            return CompletableFuture.completedFuture(null);
        } catch (Exception ex) {
            String errMsg = ex.getMessage() != null ? ex.getMessage() : "未知异常";
            String stack = StackTraceUtils.getStackTrace(ex);

            // if (stack.length() > 4000) stack = stack.substring(0, 4000) + "...";

            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "ERROR",
                    "Sql 节点执行异常: " + errMsg,
                    Map.of(
                            "exception", errMsg,
                            "stackTrace", stack
                    )
            ));

            log.error("SqlNode 执行异常: {}", ex.getMessage(), ex);
            CompletableFuture<Void> failed = new CompletableFuture<>();
            failed.completeExceptionally(ex);
            return failed;
        }
    }

    private SqlNodeData getNodeData() {
        String nodeDataJson = String.valueOf(node.getProperties().get("node_data"));
        return JSONObject.parseObject(nodeDataJson, SqlNodeData.class);
    }
}