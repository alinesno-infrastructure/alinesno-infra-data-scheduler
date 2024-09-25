package com.alinesno.infra.data.scheduler.executor.handle;

import com.alinesno.infra.data.scheduler.executor.BaseExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("shellExecutor")
public class ShellExecutor extends BaseExecutorService {

    @SneakyThrows
    @Override
    public void execute(TaskInfoBean taskInfo) {
        log.debug("执行Shell任务");
        Thread.sleep(5000);
    }

}
