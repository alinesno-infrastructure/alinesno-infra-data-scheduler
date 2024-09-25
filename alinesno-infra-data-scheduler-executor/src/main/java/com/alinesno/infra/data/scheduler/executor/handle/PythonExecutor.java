package com.alinesno.infra.data.scheduler.executor.handle;

import com.alinesno.infra.data.scheduler.executor.BaseExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("pythonExecutor")
public class PythonExecutor extends BaseExecutorService {

    @Override
    public void execute(TaskInfoBean task) {
        log.debug("PythonExecutor execute");
    }
}
