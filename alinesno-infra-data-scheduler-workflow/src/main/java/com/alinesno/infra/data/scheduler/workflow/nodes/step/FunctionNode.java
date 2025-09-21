package com.alinesno.infra.data.scheduler.workflow.nodes.step;

import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.common.core.utils.StringUtils;
import com.alinesno.infra.data.scheduler.workflow.constants.FlowConst;
import com.alinesno.infra.data.scheduler.workflow.nodes.AbstractFlowNode;
import com.alinesno.infra.data.scheduler.workflow.nodes.variable.step.FunctionNodeData;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 该类表示脚本功能节点，继承自 AbstractFlowNode 类。
 * 允许使用 Groovy 脚本进行编辑开发，以实现一些自定义的功能和逻辑。
 * 在工作流中，当需要执行特定的脚本逻辑时，会使用该节点。
 */
@Slf4j
@Scope("prototype")
@Service(FlowConst.FLOW_STEP_NODE + "function")
@EqualsAndHashCode(callSuper = true)
public class FunctionNode extends AbstractFlowNode {

//    @Autowired
//    protected IMessageService messageService;

    /**
     * 构造函数，初始化节点类型为 "function"。
     */
    public FunctionNode() {
        setType("function");
    }

    @Override
    protected CompletableFuture<Void> handleNode() {
        log.debug("node type = {} output = {}", node.getType(), output);
        FunctionNodeData nodeData = getNodeData();
        log.debug("FunctionNodeData = {}", nodeData);

        return CompletableFuture.supplyAsync(() -> {

            // 上一个任务节点不为空，执行任务并记录
//            List<CodeContent> codeContentLis = null;
//            if (workflowExecution != null) {
//                String gentContent = workflowExecution.getContent();
//                codeContentLis = CodeBlockParser.parseCodeBlocks(gentContent);
//            }

            String scriptText = nodeData == null ? null : nodeData.getRawScript();

            String nodeOutput = executeGroovyScript(scriptText);

            log.debug("handleNode nodeOutput : {}", nodeOutput);
            outputContent.append("脚本执行结果:").append(nodeOutput);

            // 触发节点事件消息
            eventNodeMessage(nodeOutput);

            return nodeOutput;
        }).thenAccept(nodeOutput -> {
            log.debug("message = {}", nodeOutput);
            output.put(node.getStepName() + ".result", nodeOutput);

            if (node.isPrint() && StringUtils.isNotEmpty(nodeOutput)) {  // 是否为返回内容，如果是则输出消息
                eventMessageCallbackMessage(nodeOutput);
            }
        }).exceptionally(ex -> {
            log.error("FunctionNode 执行异常: {}", ex.getMessage(), ex);
            return null;
        });
    }

    /**
     * 获取节点数据，用于执行脚本逻辑。
     *
     * @return
     */
    private FunctionNodeData getNodeData() {
        String nodeDataJson = String.valueOf(node.getProperties().get("node_data"));
        return JSONObject.parseObject(nodeDataJson, FunctionNodeData.class);
    }

    /**
     * 执行groovy脚本并返回执行结果
     *
     * @param scriptText
     * @return
     */
    private String executeGroovyScript(String scriptText) {

        if (StringUtils.isEmpty(scriptText)) {
            return "角色脚本执行失败:脚本为空";
        }

        // 创建 Binding 对象，用于绑定变量到 Groovy 脚本
        Binding binding = new Binding();

        // 创建 GroovyShell 实例
        GroovyShell shell = new GroovyShell(this.getClass().getClassLoader(), binding);

        // 执行 Groovy 脚本
        try {
            return String.valueOf(shell.evaluate(scriptText));
        } catch (Exception e) {
            return "角色脚本执行失败:" + e.getMessage();
        }
    }

}