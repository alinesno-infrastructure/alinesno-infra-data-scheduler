package com.alinesno.infra.data.scheduler.workflow.nodes.step;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.common.core.utils.StringUtils;
import com.alinesno.infra.data.scheduler.service.IDataSourceService;
import com.alinesno.infra.data.scheduler.workflow.constants.FlowConst;
import com.alinesno.infra.data.scheduler.workflow.nodes.AbstractFlowNode;
import com.alinesno.infra.data.scheduler.workflow.nodes.variable.step.FunctionNodeData;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
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

    @Autowired
    private IDataSourceService dataSourceService;

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

        assert nodeData != null;

        return CompletableFuture.supplyAsync(() -> {

            String scriptText = nodeData.getRawScript();

            String nodeOutput = executeGroovyScript(scriptText , nodeData , outputContent);

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
     * @param nodeData
     * @param outputContent
     * @return
     */
    private String executeGroovyScript(String scriptText, FunctionNodeData nodeData, StringBuilder outputContent) {

        PrintStream oldPrintStream = System.out; //将原来的System.out交给printStream 对象保存
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bos)); //设置新的out

        outputContent.append("脚本执行开始:").append(scriptText);

        // 创建 Binding 对象，用于绑定变量到 Groovy 脚本
        Binding binding = new Binding();

        binding.setVariable("executorService", this);
        binding.setVariable("log", log);

        // 数据源配置
        if(nodeData.getReaderDataSourceId() != null) {
            DruidDataSource dataSource = dataSourceService.getDataSource(nodeData.getReaderDataSourceId()) ;
            if(dataSource != null){
                JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                binding.setVariable("readerDataSource", dataSource);
                binding.setVariable("readerJdbcTemplate", jdbcTemplate);
            }
        }

        if(nodeData.getSinkDataSourceId() != null) {
            DruidDataSource sinkDataSource = dataSourceService.getDataSource(nodeData.getSinkDataSourceId()) ;
            if(sinkDataSource != null){
                JdbcTemplate jdbcTemplate = new JdbcTemplate(sinkDataSource);
                binding.setVariable("sinkDataSource", sinkDataSource);
                binding.setVariable("sinkJdbcTemplate", jdbcTemplate);
            }
        }

        // 创建 GroovyShell 实例
        GroovyShell shell = new GroovyShell(FunctionNode.class.getClassLoader(), binding);

        // 执行 Groovy 脚本
        Object resultObj = shell.evaluate(scriptText) ;

        System.setOut(oldPrintStream); //恢复原来的System.out
        System.out.println(bos); //将bos中保存的信息输出,这就是我们上面准备要输出的内容

        outputContent.append("脚本执行结束:").append(resultObj);

        return resultObj == null ? "" : resultObj.toString() ;
    }

}