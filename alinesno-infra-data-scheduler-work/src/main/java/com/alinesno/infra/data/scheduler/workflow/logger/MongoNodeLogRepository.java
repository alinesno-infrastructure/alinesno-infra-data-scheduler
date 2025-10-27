package com.alinesno.infra.data.scheduler.workflow.logger;

import com.alinesno.infra.data.scheduler.api.logger.NodeLog;
import com.alinesno.infra.data.scheduler.workflow.config.NodeLogProperties;
import org.bson.Document;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Repository
public class MongoNodeLogRepository {

    private final MongoTemplate mongoTemplate;
    private final NodeLogProperties props;

    public MongoNodeLogRepository(MongoTemplate mongoTemplate, NodeLogProperties props) {
        this.mongoTemplate = mongoTemplate;
        this.props = props;
    }

    public void insertBatch(List<NodeLog> logs) {
        if (logs == null || logs.isEmpty()) return;
        List<Document> docs = logs.stream().map(n -> {
            Document d = new Document();
            d.put("taskId", n.getTaskId());
            d.put("nodeId", n.getNodeId());
            d.put("nodeName", n.getNodeName());
            d.put("level", n.getLevel());
            d.put("message", n.getMessage());
            d.put("meta", n.getMeta() == null ? new Document() : new Document(n.getMeta()));
            d.put("timestamp", Date.from(n.getTimestamp()));
            return d;
        }).collect(Collectors.toList());
        mongoTemplate.getCollection(props.getCollection()).insertMany(docs);
    }
}