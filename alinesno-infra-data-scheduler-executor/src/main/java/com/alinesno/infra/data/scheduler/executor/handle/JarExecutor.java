package com.alinesno.infra.data.scheduler.executor.handle;

import com.alinesno.infra.data.scheduler.api.ParamsDto;
import com.alinesno.infra.data.scheduler.constants.PipeConstants;
import com.alinesno.infra.data.scheduler.executor.BaseExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import com.alinesno.infra.data.scheduler.executor.shell.ShellHandle;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;

@Slf4j
@Service("jarExecutor")
public class JarExecutor extends BaseExecutorService {

    @SneakyThrows
    @Override
    public void execute(TaskInfoBean taskInfo) {
        log.debug("jarExecutor execute");

        ParamsDto params = getParamsDto(taskInfo) ;
        List<String> resources = downloadResource(params.getResourceId() , getWorkspace(taskInfo)) ;

        for (String resource : resources) {
            writeLog(taskInfo, "resource: " + resource);

            // 设置命令行
            File logFile = new File(getWorkspace(taskInfo), PipeConstants.RUNNING_LOGGER);

            log.debug("logFile: {}", logFile.getAbsoluteFile());

            ShellHandle shellHandle = new ShellHandle("/bin/sh", "-c", "java -jar " + resource);
            shellHandle.setLogPath(logFile.getAbsolutePath());

            shellHandle.execute();
        }
    }

}
