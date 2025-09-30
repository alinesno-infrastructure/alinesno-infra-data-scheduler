package com.alinesno.infra.data.scheduler.workflow.nodes.step;

import com.alibaba.druid.pool.DruidDataSource;
import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.data.scheduler.entity.DataSourceEntity;
import com.alinesno.infra.data.scheduler.service.IDataSourceService;
import com.alinesno.infra.data.scheduler.workflow.constants.FlowConst;
import com.alinesno.infra.data.scheduler.workflow.nodes.AbstractFlowNode;
import com.alinesno.infra.data.scheduler.workflow.nodes.variable.step.SqlNodeData;
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

    @Autowired
    private IDataSourceService dataSourceService;

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

            Long datasourceId = nodeData.getDataSourceId();
            DruidDataSource druidDataSource = dataSourceService.getDataSource(datasourceId)  ;

            outputContent.append("数据源ID：").append(datasourceId).append("\r\n")  ;
            outputContent.append("数据源名称：").append(druidDataSource.getProperties()).append("\r\n") ;

            String sqlContent = nodeData.getSqlContent();

            // 创建一个数据源
            Connection connection = null;
            try {
                connection = druidDataSource.getConnection();
                // 执行SQL脚本
                ScriptUtils.executeSqlScript(connection, new ByteArrayResource(sqlContent.getBytes()));
            } catch (SQLException e) {
                throw new RpcServiceRuntimeException("获取数据源异常:" + e.getMessage());
            }finally {
                DataSourceUtils.releaseConnection(connection, druidDataSource);
                druidDataSource.close();
            }

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