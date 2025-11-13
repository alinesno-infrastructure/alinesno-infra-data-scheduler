package com.alinesno.infra.data.scheduler.api.worker;

import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.common.facade.response.R;
import com.alinesno.infra.data.scheduler.adapter.worker.WorkerNodeLogConsumer;
import com.alinesno.infra.data.scheduler.api.logger.NodeLog;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

/**
 * 节点日志控制类
 */
@Slf4j
@RestController
@RequestMapping("/api/infra/data/scheduler/nodeLogs")
public class WorkerNodeLogController {

    @Autowired
    private WorkerNodeLogConsumer workerNodeLogConsumer;

    /**
     * 读取日志
     * @param processInstanceId
     * @param nodeId
     * @param start
     * @return
     */
    @GetMapping("/readLog")
    public AjaxResult readLog(@RequestParam String processInstanceId,
                              @RequestParam(required = false) String nodeId ,
                              @RequestParam(required = false, defaultValue = "0") int start) {

        if(!StringUtils.hasLength(nodeId)){
            nodeId = null ;
        }

       R<Map<String , Object>> result = workerNodeLogConsumer.readLog(processInstanceId , nodeId ,  start) ;

       return AjaxResult.success(result.getData());
    }
}