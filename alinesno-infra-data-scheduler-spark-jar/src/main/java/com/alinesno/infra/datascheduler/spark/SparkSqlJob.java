package com.alinesno.infra.datascheduler.spark;

import com.alinesno.infra.datascheduler.spark.utils.SqlSplitSparkSqlUtil;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SaveMode;
import org.apache.spark.sql.SparkSession;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 一个简单的 Spark SQL 任务入口，特性：
 * - 支持参数 --sql "SELECT ..." 或 --sql-file /path/to/sql.sql 或 http(s)://... 的 URL
 * - 支持 --output 指定输出路径（在 Spark 环境下写入），否则打印 show()
 * - 支持 --format parquet|csv|json（默认 parquet）
 * - 支持 --show N（默认 20）
 * - 支持 --enable-hive true|false（默认 false），在需要 Hive 支持的集群环境下生效
 *
 * 说明：
 * - 该程序不依赖 Hadoop 的 FileSystem 接口（去掉了 hadoop 相关依赖）。
 * - 如果你的 SQL 文件位于 OSS（oss://）或其他非 HTTP/本地协议位置，请在提交时
 *   将文件放到本地或提供可通过 HTTP(S) 访问的地址，或者在集群端提供相应的适配器。
 */
public class SparkSqlJob {

    public static void main(String[] args) throws Exception {
        Map<String, String> params = parseArgs(args);

        String sql = params.get("sql");
        String sqlFile = params.get("sql-file");
        String output = params.get("output");
        String format = params.getOrDefault("format", "parquet");

        int showN = Integer.parseInt(params.getOrDefault("show", "20"));
        boolean enableHive = Boolean.parseBoolean(params.getOrDefault("enable-hive", "false"));
        String appName = params.getOrDefault("appName", "spark-sql-job");

        if ((sql == null || sql.trim().isEmpty()) && (sqlFile == null || sqlFile.trim().isEmpty())) {
            System.err.println("错误：必须提供 --sql 或 --sql-file");
            System.exit(2);
        }

        if (sql == null || sql.trim().isEmpty()) {
            sql = readSqlFromPath(sqlFile);
        }

        System.out.println("启动 SparkSqlJob, appName=" + appName + ", enableHive=" + enableHive);
        System.out.println("要执行的 SQL:\n" + sql);

        SparkSession.Builder builder = SparkSession.builder().appName(appName);
        if (enableHive) {
            builder = builder.enableHiveSupport();
        }
        SparkSession spark = builder.getOrCreate();

        // 替换原来的执行代码
        try {
            // 分割多条SQL语句
            List<String> sqlStatements = SqlSplitSparkSqlUtil.splitSparkSql( sql , Integer.MAX_VALUE , Integer.MAX_VALUE) ;

            Dataset<Row> lastDs = null;
            for (String statement : sqlStatements) {
                statement = statement.trim();
                if (statement.isEmpty()) continue;

                System.out.println("执行SQL: " + statement);
                Dataset<Row> ds = spark.sql(statement);

                // 打印 schema 供调试
                System.out.println("结果 schema: " + ds.schema().treeString());

                // 仅当结果有列时才认为是可写/可展示的结果，保留为 lastDs
                if (ds.schema() != null && ds.schema().fields().length > 0) {
                    lastDs = ds;
                } else {
                    System.out.println("注意：该语句返回空 schema（可能为 DDL/SET/INSERT/SHOW 等），不会用于写出。");
                }
            }

            if (lastDs != null && lastDs.schema() != null && lastDs.schema().fields().length > 0) {
                if (output != null && !output.trim().isEmpty()) {
                    System.out.println("写出结果到: " + output + " 格式=" + format);
                    lastDs.write().mode(SaveMode.Overwrite).format(format).save(output);
                    System.out.println("写出完成。");
                } else {
                    System.out.println("未指定输出路径，展示前 " + showN + " 行：");
                    lastDs.show(showN, false);
                }
            } else {
                // 没有可写的结果 DataFrame，给出友好提示或选择默认行为
                System.out.println("没有可写的查询结果（没有找到带列的 SELECT 结果）。请检查传入的 SQL 是否包含 SELECT。");
            }
        } finally {
            spark.stop();
        }
    }

    /**
     * 简单的命令行参数解析，格式形如 --key value 或 --flag（表示 true）
     */
    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> m = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String a = args[i];
            if (a.startsWith("--")) {
                String key = a.substring(2);
                String value = "true";
                if (i + 1 < args.length && !args[i + 1].startsWith("--")) {
                    value = args[++i];
                }
                m.put(key, value);
            }
        }
        return m;
    }

    /**
     * 读取 SQL 内容：
     * - 如果是 http(s) URL，则通过 HTTP GET 下载并返回内容（使用 UTF-8）
     * - 如果是本地路径，则直接读取文件内容（使用 UTF-8）
     * - 其他带有自定义 scheme（例如 oss://）的路径当前不直接支持，会抛出异常
     *
     * 注意：如需直接支持 OSS 等协议，请在集群中提供相应的适配器或使用 OSS SDK。
     */
    private static String readSqlFromPath(String pathStr) throws Exception {
        if (pathStr == null) return null;
        pathStr = pathStr.trim();

        // 处理 HTTP(S) URL
        if (pathStr.startsWith("http://") || pathStr.startsWith("https://")) {
            URL url = new URL(pathStr);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(15_000);
            conn.setReadTimeout(30_000);
            conn.setDoInput(true);
            int code = conn.getResponseCode();
            if (code >= 200 && code < 300) {
                try (InputStream in = conn.getInputStream();
                     BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) {
                        sb.append(line).append('\n');
                    }
                    return sb.toString();
                }
            } else {
                throw new IllegalStateException("无法通过 HTTP 获取 SQL 文件，返回码: " + code);
            }
        }

        // 如果是带 scheme 的其他 URI（例如 oss://），提示不支持
        if (pathStr.matches("^[a-zA-Z][a-zA-Z0-9+.-]*://.*")) {
            // 仅保留中文提示信息
            throw new UnsupportedOperationException("检测到非本地或 HTTP 的 URI（例如 oss://）。当前程序未包含 Hadoop/OSS 适配器。"
                    + "请改为使用 --sql 直接传 SQL，或将 SQL 放到本地文件或可通过 HTTP 访问的位置。");
        }

        // 本地文件读取
        byte[] bytes = Files.readAllBytes(Paths.get(pathStr));
        return new String(bytes, StandardCharsets.UTF_8);
    }
}