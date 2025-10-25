package com.alinesno.infra.data.scheduler.workflow.enums;

import lombok.Getter;

/**
 * Spark运行类型枚举
 */
@Getter
public enum SparkRunTypeEnums {

    SPARK_SQL("spark-sql" , "SPark-SQL") ,
    PY_SPARK("pyspark" , "PySpark") ;

    private final String value;
    private final String label;

    SparkRunTypeEnums(String value , String label){
        this.value = value;
        this.label = label;
    }

}
