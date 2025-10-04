package com.alinesno.infra.data.scheduler.workflow.logger;

import com.alinesno.infra.data.scheduler.workflow.config.NodeLogProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public class MongoNodeLogQueryRepository {

    private final MongoTemplate mongoTemplate;
    private final NodeLogProperties props;

    @Autowired
    public MongoNodeLogQueryRepository(MongoTemplate mongoTemplate, NodeLogProperties props) {
        this.mongoTemplate = mongoTemplate;
        this.props = props;
    }

    public Page<NodeLog> findByFilters(String nodeId,
                                       String taskId, String level,
                                       Date from,
                                       Date to,
                                       Pageable pageable) {
        Query q = new Query();
        if (nodeId != null) q.addCriteria(Criteria.where("nodeId").is(nodeId));
        if (taskId != null) q.addCriteria(Criteria.where("taskId").is(taskId));
        if (level != null) q.addCriteria(Criteria.where("level").is(level));
        if (from != null && to != null) q.addCriteria(Criteria.where("timestamp").gte(from).lte(to));
        else if (from != null) q.addCriteria(Criteria.where("timestamp").gte(from));
        else if (to != null) q.addCriteria(Criteria.where("timestamp").lte(to));

        long total = mongoTemplate.count(q, props.getCollection());
        q.with(pageable);
        List<NodeLog> list = mongoTemplate.find(q, NodeLog.class, props.getCollection());
        return new PageImpl<>(list, pageable, total);
    }


}