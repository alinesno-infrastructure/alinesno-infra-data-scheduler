package com.alinesno.infra.data.scheduler.workflow.nodes.step;

import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.data.scheduler.workflow.constants.FlowConst;
import com.alinesno.infra.data.scheduler.workflow.nodes.AbstractFlowNode;
import com.alinesno.infra.data.scheduler.workflow.nodes.variable.step.SqlNodeData;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 该类表示指定回复节点，继承自 AbstractFlowNode 类。
 * 用于指定回复内容，并且引用变量会转换为字符串进行输出。
 * 在工作流中，当需要给出固定的回复信息时，会使用该节点。
 */
@Slf4j
@Data
@Scope("prototype")
@Service(FlowConst.FLOW_STEP_NODE + "sql")
@EqualsAndHashCode(callSuper = true)
public class SqlNode extends AbstractFlowNode {

    /**
     * 构造函数，初始化节点类型为 "reply"。
     */
    public SqlNode() {
        setType("reply");
    }

    @Override
    protected CompletableFuture<Void> handleNode() {
        try {
            SqlNodeData nodeData = getNodeData();
            log.debug("nodeData = {}" , nodeData);
            log.debug("node type = {} output = {}" , node.getType() , output);

            String sqlContent = nodeData.getSqlContent();

            String sqlResult =  sqlContent + "执行结果" ;
            output.put(node.getStepName() + ".sql_result", sqlResult);

            if (node.isPrint()) {
                eventMessageCallbackMessage(sqlResult);
            }

            return CompletableFuture.completedFuture(null);
        } catch (Exception ex) {
            log.error("ReplyNode 执行异常: {}", ex.getMessage(), ex);
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