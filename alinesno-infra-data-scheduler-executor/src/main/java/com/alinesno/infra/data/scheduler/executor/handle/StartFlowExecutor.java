package com.alinesno.infra.data.scheduler.executor.handle;

import com.alinesno.infra.data.scheduler.executor.BaseExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("startFlowExecutor")
public class StartFlowExecutor extends BaseExecutorService {

    @Override
    public void execute(TaskInfoBean taskInfo) {
        log.debug("StartFlowExecutor execute");

        // 输出运行空间、M2HOME
        log.info("--->>> 运行空间: {}", getWorkspace(taskInfo));
        log.info("--->>> PythonHome : {}", getPythonHome(taskInfo.getEnvironment()));
        log.info("--->>> M2Home: {}", getM2Home(taskInfo.getEnvironment()));
        log.info("--->>> JavaHome: {}", getJavaHome(taskInfo.getEnvironment()));

    }
}
