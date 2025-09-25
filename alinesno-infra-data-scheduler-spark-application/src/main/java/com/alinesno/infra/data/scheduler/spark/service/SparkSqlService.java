//package com.alinesno.infra.data.scheduler.spark.service;
//
//import com.alinesno.infra.data.scheduler.spark.model.SqlResponse;
//import org.apache.spark.sql.Dataset;
//import org.apache.spark.sql.Row;
//import org.apache.spark.sql.SparkSession;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.stereotype.Service;
//
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//@Service
//public class SparkSqlService {
//
//    private final SparkSession sparkSession;
//
//    @Autowired
//    public SparkSqlService(SparkSession sparkSession) {
//        this.sparkSession = sparkSession;
//    }
//
//    public SqlResponse executeSql(String sql) {
//        SqlResponse response = new SqlResponse();
//
//        try {
//            long startTime = System.currentTimeMillis();
//
//            // 执行SQL查询
//            Dataset<Row> result = sparkSession.sql(sql);
//
//            // 获取列名
//            List<String> columns = new ArrayList<>();
//            for (String col : result.columns()) {
//                columns.add(col);
//            }
//
//            // 转换结果为List<Map>
//            List<Row> rows = result.collectAsList();
//            List<Map<String, Object>> data = new ArrayList<>();
//
//            for (Row row : rows) {
//                Map<String, Object> rowMap = new HashMap<>();
//                for (int i = 0; i < columns.size(); i++) {
//                    rowMap.put(columns.get(i), row.get(i));
//                }
//                data.add(rowMap);
//            }
//
//            long endTime = System.currentTimeMillis();
//
//            // 设置响应信息
//            response.setSuccess(true);
//            response.setMessage("SQL executed successfully");
//            response.setColumns(columns);
//            response.setData(data);
//            response.setRowCount(rows.size());
//            response.setExecutionTimeMs(endTime - startTime);
//
//        } catch (Exception e) {
//            response.setSuccess(false);
//            response.setMessage("Error executing SQL: " + e.getMessage());
//            response.setExecutionTimeMs(0);
//        }
//
//        return response;
//    }
//}
