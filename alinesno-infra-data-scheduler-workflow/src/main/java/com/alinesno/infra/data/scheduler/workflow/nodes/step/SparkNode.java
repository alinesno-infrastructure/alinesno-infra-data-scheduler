package com.alinesno.infra.data.scheduler.workflow.nodes.step;

import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.data.scheduler.adapter.SparkSqlConsumer;
import com.alinesno.infra.data.scheduler.entity.ComputeEngineEntity;
import com.alinesno.infra.data.scheduler.service.IComputeEngineService;
import com.alinesno.infra.data.scheduler.workflow.constants.FlowConst;
import com.alinesno.infra.data.scheduler.workflow.nodes.AbstractFlowNode;
import com.alinesno.infra.data.scheduler.workflow.nodes.variable.step.SparkNodeData;
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
 * 其功能是根据提供的文本内容生成相应的图片。
 * 在工作流中，如果需要根据文本描述生成图片，就会用到这个节点。
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

        try {
            SparkNodeData nodeData = getNodeData();
            log.debug("nodeData = {}", nodeData);
            log.debug("node type = {} output = {}", node.getType(), output);

            String sqlContent = nodeData.getSqlContent();

            // 2. 创建 SparkSqlConsumer
            SparkSqlConsumer consumer = new SparkSqlConsumer(computeEngine);

            long waitMs = 600_000L; // 等待 600 秒
            Map<String, Object> params = new HashMap<>();

            JSONObject syncResp = consumer.submitSync(sqlContent , params, computeEngine.getAdminUser(), waitMs);
            System.out.println("同步提交响应: " + syncResp.toJSONString());

            // TODO 获取到执行完成的文件并解析下载
            output.put(node.getStepName() + ".result", syncResp.toJSONString());

            return CompletableFuture.completedFuture(null);
        } catch (Exception ex) {
            log.error("ImageGenerateNode 执行异常: {}", ex.getMessage(), ex);
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