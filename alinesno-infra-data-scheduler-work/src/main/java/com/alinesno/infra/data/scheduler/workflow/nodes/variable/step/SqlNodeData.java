package com.alinesno.infra.data.scheduler.workflow.nodes.variable.step;

import com.alinesno.infra.data.scheduler.workflow.nodes.variable.NodeData;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 回复相关的节点数据类
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SqlNodeData extends NodeData {

    /**
     * 数据源ID
     */
   private Long dataSourceId ;

    /**
     * SQL内容
     */
   private String sqlContent ;

}