package com.alinesno.infra.data.scheduler.executor.handle;

import com.alinesno.infra.data.scheduler.entity.TaskDefinitionEntity;
import com.alinesno.infra.data.scheduler.executor.BaseExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ShellExecutor extends BaseExecutorService {

    @Override
    public void execute(TaskInfoBean taskInfo) {
        log.debug("执行Shell任务");
    }

}
