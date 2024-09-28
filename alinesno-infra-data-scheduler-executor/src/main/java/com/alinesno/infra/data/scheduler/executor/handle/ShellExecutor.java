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

@Slf4j
@Service("shellExecutor")
public class ShellExecutor extends BaseExecutorService {

    @SneakyThrows
    @Override
    public void execute(TaskInfoBean taskInfo) {

        ParamsDto paramsDto = getParamsDto(taskInfo) ;
        String rawScript = paramsDto.getRawScript();

        log.debug("Shell Executor rawScript: {}", rawScript) ;

        // 设置命令行
        File logFile = new File(getWorkspace(taskInfo), PipeConstants.RUNNING_LOGGER);

        log.debug("logFile: {}", logFile.getAbsoluteFile());

        ShellHandle shellHandle = new ShellHandle("/bin/sh", "-c", rawScript);
        shellHandle.setLogPath(logFile.getAbsolutePath());

        shellHandle.execute();
    }

}