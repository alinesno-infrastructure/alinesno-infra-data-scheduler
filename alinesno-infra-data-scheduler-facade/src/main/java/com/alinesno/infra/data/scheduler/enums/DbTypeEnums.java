package com.alinesno.infra.data.scheduler.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import com.google.common.base.Functions;
import lombok.Getter;

import java.util.Arrays;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Getter
public enum DbTypeEnums {

    MYSQL(0, "mysql"),
    POSTGRESQL(1, "postgresql"),
    HIVE(2, "hive"),
    SPARK(3, "spark"),
    CLICKHOUSE(4, "clickhouse"),
    ORACLE(5, "oracle"),
    SQLSERVER(6, "sqlserver"),
    DB2(7, "db2"),
    PRESTO(8, "presto"),
    H2(9, "h2");

    @EnumValue
    private final int code;
    private final String descp;

    DbTypeEnums(int code, String descp) {
        this.code = code;
        this.descp = descp;
    }

    private static final Map<Integer, DbTypeEnums> DB_TYPE_MAP =
            Arrays.stream(DbTypeEnums.values()).collect(toMap(DbTypeEnums::getCode, Functions.identity()));

    public static DbTypeEnums of(int type) {
        if (DB_TYPE_MAP.containsKey(type)) {
            return DB_TYPE_MAP.get(type);
        }
        return null;
    }

}
