package com.alinesno.infra.data.scheduler.spark.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

/**
 * SparkSqlController
 */
@RestController
@RequestMapping("/")
public class IndexController {

    // 控制器字段注入
    @Value("#{'${spark.admin-users}'.split(',')}")
    private Set<String> adminUsers;

    /**
     * 写一个是否健康的接口，并且adminUsers传递fpgor是正确，并返回SUCCESS
     */
    @GetMapping("/health")
    public String health(String adminUser) {
        return adminUsers.contains(adminUser) ? "SUCCESS" : "FAILURE";
    }

}
