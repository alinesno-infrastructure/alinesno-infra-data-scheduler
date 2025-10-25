package com.alinesno.infra.data.scheduler.workflow.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;

import java.util.concurrent.TimeUnit;

/**
 * MongoDB 自动配置类
 */
@Configuration
@EnableConfigurationProperties(MongoProperties.class)
public class MongoAutoConfiguration {

    private final NodeLogProperties nodeLogProperties;


    public MongoAutoConfiguration(NodeLogProperties nodeLogProperties) {
        this.nodeLogProperties = nodeLogProperties;
    }

    @Bean
    public MongoClient mongoClient(MongoProperties props) {
        if (props.getUri() != null && !props.getUri().isBlank()) {
            return MongoClients.create(props.getUri());
        }
        throw new IllegalStateException("mongodb uri is not configured");
    }

    @Bean
    public MongoTemplate schedulerMongoTemplate(MongoClient client, MongoProperties props) {
        return new MongoTemplate(client, props.getDatabase());
    }


    // 在应用启动后确保 TTL 索引：timestamp 字段按配置过期
    @EventListener(ApplicationReadyEvent.class)
    public void ensureTtlIndex(ApplicationReadyEvent event) {
        MongoTemplate mongoTemplate =
                event.getApplicationContext().getBean("schedulerMongoTemplate", MongoTemplate.class);

        mongoTemplate.indexOps(nodeLogProperties.getCollection())
                .ensureIndex(new Index().on("timestamp", Sort.Direction.ASC)
                        .expire(nodeLogProperties.getTtlDays(), TimeUnit.DAYS));

        mongoTemplate.indexOps(nodeLogProperties.getCollection())
                .ensureIndex(new Index().on("nodeId", Sort.Direction.ASC).on("timestamp", Sort.Direction.DESC));

        mongoTemplate.indexOps(nodeLogProperties.getCollection())
                .ensureIndex(new Index().on("taskId", Sort.Direction.ASC).on("timestamp", Sort.Direction.DESC));
    }
}