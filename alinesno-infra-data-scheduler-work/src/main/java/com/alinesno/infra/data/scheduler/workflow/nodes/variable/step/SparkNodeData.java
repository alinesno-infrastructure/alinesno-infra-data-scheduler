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
    private boolean isAsync = true;  // 是否异步执行


    private Integer pollIntervalSeconds = 5 ;  // 轮询间隔5S执行一次
    private Long maxWaitSeconds = 7200L ; // 最大等待时间60*60*2=7200

}

