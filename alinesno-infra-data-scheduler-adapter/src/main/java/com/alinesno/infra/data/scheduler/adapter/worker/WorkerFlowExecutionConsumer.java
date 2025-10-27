package com.alinesno.infra.data.scheduler.adapter.worker;

import com.alinesno.infra.common.facade.pageable.DatatablesPageBean;
import com.alinesno.infra.common.facade.pageable.TableDataInfo;
import com.alinesno.infra.common.facade.response.R;
import com.dtflys.forest.annotation.BaseRequest;
import com.dtflys.forest.annotation.Header;
import com.dtflys.forest.annotation.JSONBody;
import com.dtflys.forest.annotation.Post;

/**
 * 流程执行工作流
 */
@BaseRequest(baseURL = "#{alinesno.data.scheduler.worker-node}/api/infra/data/scheduler/processInstance" , connectTimeout = 30*1000)
public interface WorkerFlowExecutionConsumer {

    /**
     * 获取流程执行列表
     * @param page
     * @return
     */
    @Post("/datatables")
    TableDataInfo datatables(@JSONBody DatatablesPageBean page) ;

}
