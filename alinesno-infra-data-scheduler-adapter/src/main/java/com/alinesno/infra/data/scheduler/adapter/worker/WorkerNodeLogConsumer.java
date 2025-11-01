package com.alinesno.infra.data.scheduler.adapter.worker;

import com.alinesno.infra.common.facade.response.R;
import com.dtflys.forest.annotation.BaseRequest;
import com.dtflys.forest.annotation.Get;
import com.dtflys.forest.annotation.Query;

import java.util.Map;

/**
 * WorkerNodeLogConsumer
 */
@BaseRequest(baseURL = "#{alinesno.data.scheduler.worker-node}/api/infra/data/scheduler/nodeLogs" , readTimeout = 3600_000)
public interface WorkerNodeLogConsumer {

    /**
     * 读取任务日志
     * @param processInstanceId
     * @param nodeId
     * @param start
     * @return
     */
    @Get("/readLog")
    R<Map<String, Object>> readLog(@Query("processInstanceId") String processInstanceId,
                                   @Query("nodeId") String nodeId,
                                   @Query("start") int start);

}
