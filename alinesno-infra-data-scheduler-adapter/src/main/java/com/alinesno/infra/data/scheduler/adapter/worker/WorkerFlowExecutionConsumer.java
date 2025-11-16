package com.alinesno.infra.data.scheduler.adapter.worker;

import com.alinesno.infra.common.facade.pageable.DatatablesPageBean;
import com.alinesno.infra.common.facade.pageable.TableDataInfo;
import com.alinesno.infra.common.facade.response.R;
import com.dtflys.forest.annotation.*;

/**
 * 流程执行工作流
 */
@BaseRequest(baseURL = "#{alinesno.data.scheduler.worker-node}/api/infra/data/scheduler/processInstance" , readTimeout = 3600_000)
public interface WorkerFlowExecutionConsumer {

    /**
     * 获取流程执行列表
     * @param page
     * @return
     */
    @Post("/datatables")
    TableDataInfo datatables(@JSONBody DatatablesPageBean page) ;

    /**
     * 删除流程执行
     * @param ids
     * @return
     */
    @Delete("/delete/{ids}")
    R<Void> delete(@Var("ids") String ids);
}
