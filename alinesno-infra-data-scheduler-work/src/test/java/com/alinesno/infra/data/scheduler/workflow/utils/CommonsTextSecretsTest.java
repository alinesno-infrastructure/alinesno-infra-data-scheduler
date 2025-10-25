package com.alinesno.infra.data.scheduler.workflow.utils;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * 测试类
 */
public class CommonsTextSecretsTest {

    @Test
    public void replace() {

        String script = """
                #!/bin/bash
                
                # git sync
                
                # 设置 API 密钥和环境变量
                export ALINESNO_QIWEN_API_KEY=${{ secrets.ALINESNO_QIWEN_API_KEY }}
                
                export ALINESNO_SOURCE_GIT_USERNAME=${{ secrets.ALINESNO_SOURCE_GIT_USERNAME }}
                export ALINESNO_SOURCE_GIT_ACCESS_TOKEN=${{ secrets.ALINESNO_SOURCE_GIT_ACCESS_TOKEN }}
                
                export ALINESNO_TARGET_GIT_ACCESS_TOKEN=${{ secrets.ALINESNO_TARGET_GIT_ACCESS_TOKEN }}
                export ALINESNO_TARGET_GIT_USERNAME=${{ secrets.ALINESNO_TARGET_GIT_USERNAME }}
                
                export DB_POSTGRESQL_URL=${{ secrets.DEV_DB_POSTGRESQL_URL }}
                export DB_POSTGRESQL_USERNAME=${{ secrets.DEV_DB_POSTGRESQL_USERNAME }}
                export DB_POSTGRESQL_PASSWORD=${{ secrets.DEV_DB_POSTGRESQL_PASSWORD }}
                
                # 下载同步工具
                wget ${{ secrets.UDF_SYNC_GIT_REPOSITORY_DOWNLOAD_URL }} -O github-repos-mirror.jar
                
                # 设置 Java 数据库连接参数
                export JAVA_TOOL_OPTIONS="-Dspring.datasource.url=${DB_POSTGRESQL_URL} -Dspring.datasource.username=${DB_POSTGRESQL_USERNAME} -Dspring.datasource.password=${DB_POSTGRESQL_PASSWORD}"
                
                # 定义仓库名称数组
                repositoryNames=(
                    "alinesno-infra-data-stream"
                    "alinesno-infra-data-lake"\s
                    "alinesno-infra-data-mdm"
                    "alinesno-infra-data-pipeline"
                    "alinesno-infra-data-assets"
                    "alinesno-infra-data-integration"
                    "alinesno-infra-data-scheduler"
                )
                
                # 遍历数组中的每一个仓库名称
                for repoName in "${repositoryNames[@]}"; do
                    echo "同步仓库 ${repoName} >>>>>>>>>>>>>>>>>>>>>>>>> start"
                
                    # 执行 Java 程序并传递仓库名称
                    java -jar github-repos-mirror.jar "$repoName"
                
                    echo "Send Message To Platform ..."
                    echo "同步仓库 ${repoName} >>>>>>>>>>>>>>>>>>>>>>>>> end"
                    echo
                done
                """ ;

        Map<String, String> secrets = new HashMap<>();

        secrets.put("ALINESNO_QIWEN_API_KEY", "key-xyz");

        secrets.put("DEV_DB_POSTGRESQL_URL", "key-xyz");
        secrets.put("DEV_DB_POSTGRESQL_USERNAME", "key-xyz");
        secrets.put("DEV_DB_POSTGRESQL_PASSWORD", "key-xyz");

        secrets.put("UDF_SYNC_GIT_REPOSITORY_DOWNLOAD_URL", "https://example.com/jar");

        System.out.println(CommonsTextSecrets.replace(script, secrets));
    }
}
