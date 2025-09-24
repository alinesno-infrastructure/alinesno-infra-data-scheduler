package com.alinesno.infra.data.scheduler.spark.controller;

import com.alinesno.infra.data.scheduler.spark.model.SqlRequest;
import com.alinesno.infra.data.scheduler.spark.model.SqlResponse;
import com.alinesno.infra.data.scheduler.spark.service.SparkSqlService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/spark-sql")
public class SparkSqlController {

    private final SparkSqlService sparkSqlService;

    @Autowired
    public SparkSqlController(SparkSqlService sparkSqlService) {
        this.sparkSqlService = sparkSqlService;
    }

    @PostMapping("/execute")
    public SqlResponse executeSql(@RequestBody SqlRequest request) {
        if (request.getSql() == null || request.getSql().trim().isEmpty()) {
            SqlResponse response = new SqlResponse();
            response.setSuccess(false);
            response.setMessage("SQL statement cannot be empty");
            return response;
        }
        
        return sparkSqlService.executeSql(request.getSql());
    }
}
