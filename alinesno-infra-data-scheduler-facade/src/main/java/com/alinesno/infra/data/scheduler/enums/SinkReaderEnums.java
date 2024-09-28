package com.alinesno.infra.data.scheduler.enums;

import lombok.Getter;

/**
 * 数据源写入器枚举类
 * 用于表示不同的数据源类型
 */
@Getter
public enum SinkReaderEnums {

    CLICKHOUSE("clickhouse", "clickhouse.png", "高性能列式数据库", "SELECT 1"), // ClickHouse 数据源
    DORIS("doris", "doris.png", "高性能分析型数据库", "SELECT 1"), // Doris 数据源
    MYSQL("mysql", "mysql.png", "开放源码的小型关系数据库", "SELECT 1"), // MySQL 数据源
    HIVE("hive", "hive.png", "基于Hadoop的数据仓库工具", "SELECT 1"), // Hive 数据源
    POSTGRESQL("postgresql", "postgresql.png", "强大的开源对象关系数据库系统", "SELECT 1"), // PostgreSQL 数据源

    S3FILE("s3file", "s3file.png", "Amazon S3 文件存储服务", "SELECT * FROM s3object LIMIT 1"), // S3 文件数据源
    QINIU("qiniu", "qiniu.png", "云存储服务提供商", "SELECT * FROM qiniuobject LIMIT 1"), // Qiniu 数据源
    MINIO("minio", "minio.png", "高性能对象存储服务", "SELECT * FROM minioobject LIMIT 1"), // MinIO 数据源

    KAFKA("kafka", "kafka.png", "分布式流处理平台", "DESCRIBE TOPICS"), // Kafka 数据源
    ELASTICSEARCH("elasticsearch", "elasticsearch.png", "分布式搜索和分析引擎", "GET /_cluster/health"), // Elasticsearch 数据源
    REDIS("redis", "redis.png", "内存中的数据结构存储系统", "PING"), // Redis 数据源
    FTP("ftp", "ftp.png", "文件传输协议", "ls"); // FTP 数据源

    /**
     * 数据源代码值
     */
    private final String code;

    /**
     * 图标路径
     */
    private final String icon;

    /**
     * 描述信息
     */
    private final String desc;

    /**
     * 验证查询
     */
    private final String validateQuery;

    /**
     * 构造函数
     *
     * @param code 数据源代码值
     * @param icon 图标路径
     * @param desc 描述信息
     * @param validateQuery 验证查询
     */
    SinkReaderEnums(String code, String icon, String desc, String validateQuery) {
        this.code = code;
        this.icon = icon;
        this.desc = desc;
        this.validateQuery = validateQuery;
    }

    /**
     * 获取到所有数据源类型List列表
     */
    public static String[] getAllSourceReaderCodes() {
        SinkReaderEnums[] values = SinkReaderEnums.values();
        String[] sourceReaderCodes = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            sourceReaderCodes[i] = values[i].getCode();
        }
        return sourceReaderCodes;
    }

    /**
     * 通过类型获取验证查询
     */
    public static String getValidateQuery(String type) {
        for (SinkReaderEnums sinkEnum : SinkReaderEnums.values()) {
            if (sinkEnum.getCode().equalsIgnoreCase(type)) {
                return sinkEnum.getValidateQuery();
            }
        }
        return null;
    }
}