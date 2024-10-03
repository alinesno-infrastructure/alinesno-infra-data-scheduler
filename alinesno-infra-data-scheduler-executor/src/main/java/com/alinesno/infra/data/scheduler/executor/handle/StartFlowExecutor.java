package com.alinesno.infra.data.scheduler.executor.handle;

import com.alinesno.infra.data.scheduler.executor.AbstractExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("startFlowExecutor")
public class StartFlowExecutor extends AbstractExecutorService {

    @Override
    public void execute(TaskInfoBean taskInfo) {
        log.debug("StartFlowExecutor execute");

        writeLog("--->>> StartFlowExecutor execute");
        writeLog("--->>> 运行空间: " + getWorkspace());
        writeLog("--->>> PythonHome : " + getPythonHome());
        writeLog("--->>> M2Home: " + getM2Home());
        writeLog("--->>> JavaHome: " + getJavaHome(taskInfo.getEnvironment()));
    }
}
