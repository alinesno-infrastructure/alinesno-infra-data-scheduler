package com.alinesno.infra.data.scheduler.workflow.nodes.variable.step;

import com.alinesno.infra.data.scheduler.workflow.nodes.variable.NodeData;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 图片生成
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SparkNodeData extends NodeData {

    private String runType;  // 运行类型(spark-sql,pyspark)
    private String sqlContent;  // sql 内容
    private String pysparkContent;  // pyspark 内容
    private boolean isAsync;  // 是否异步执行

}

