package com.alinesno.infra.data.scheduler.workflow.nodes.step;

import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.data.scheduler.adapter.SparkSqlConsumer;
import com.alinesno.infra.data.scheduler.entity.ComputeEngineEntity;
import com.alinesno.infra.data.scheduler.service.IComputeEngineService;
import com.alinesno.infra.data.scheduler.workflow.constants.FlowConst;
import com.alinesno.infra.data.scheduler.workflow.logger.NodeLog;
import com.alinesno.infra.data.scheduler.workflow.nodes.AbstractFlowNode;
import com.alinesno.infra.data.scheduler.workflow.nodes.variable.step.SparkNodeData;
import com.alinesno.infra.data.scheduler.workflow.utils.StackTraceUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
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

    public SparkNode() {
        setType("spark");
    }

    @Override
    protected CompletableFuture<Void> handleNode() {

        ComputeEngineEntity computeEngine = computeEngineService.getCurrentConfig(flowExecution.getOrgId());

        long startTs = System.currentTimeMillis();
        try {
            SparkNodeData nodeData = getNodeData();
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
            String sqlPreview = sqlContent == null ? "" :
                    (sqlContent.length() > 1000 ? sqlContent.substring(0, 1000) + "..." : sqlContent);
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

            JSONObject syncResp = consumer.submitSync(sqlContent , params, computeEngine.getAdminUser(), waitMs);

            long durationMs = System.currentTimeMillis() - startTs;
            String respStr = syncResp == null ? "{}" : syncResp.toJSONString();
            String respPreview = respStr.length() > 1000 ? respStr.substring(0, 1000) + "..." : respStr;

            // 提交返回日志
            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "INFO",
                    "Spark SQL 同步提交完成",
                    Map.of(
                            "durationMs", durationMs,
                            "responseLength", respStr.length(),
                            "responsePreview", respPreview
                    )
            ));

            // TODO 获取到执行完成的文件并解析下载（若有）
            output.put(node.getStepName() + ".result", respStr);

            // 成功结束日志
            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "INFO",
                    "Spark 节点执行成功",
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