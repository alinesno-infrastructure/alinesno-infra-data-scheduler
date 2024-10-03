package com.alinesno.infra.data.scheduler.executor.handle;

import com.alinesno.infra.data.scheduler.executor.AbstractExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * K8S操作执行器
 */
@Slf4j
@Service("K8SExecutor")
public class K8SExecutor extends AbstractExecutorService {

    @Override
    public void execute(TaskInfoBean task) {
        log.debug("K8SExecutor execute");
    }
}
