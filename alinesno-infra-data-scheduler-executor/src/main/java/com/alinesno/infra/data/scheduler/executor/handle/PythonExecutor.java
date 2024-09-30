package com.alinesno.infra.data.scheduler.executor.handle;

import cn.hutool.core.util.IdUtil;
import com.alinesno.infra.data.scheduler.api.ParamsDto;
import com.alinesno.infra.data.scheduler.constants.PipeConstants;
import com.alinesno.infra.data.scheduler.executor.BaseExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import com.alinesno.infra.data.scheduler.executor.shell.ShellHandle;
import com.alinesno.infra.data.scheduler.executor.utils.OSUtils;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.charset.Charset;

@Slf4j
@Service("pythonExecutor")
public class PythonExecutor extends BaseExecutorService {

    @SneakyThrows
    @Override
    public void execute(TaskInfoBean taskInfo) {
        log.debug("PythonExecutor execute");

        ParamsDto paramsDto = getParamsDto(taskInfo) ;
        String rawScript = paramsDto.getRawScript();

        log.debug("rawScript: {}", rawScript) ;

        // 设置命令行
        File logFile = new File(getWorkspace(taskInfo), PipeConstants.RUNNING_LOGGER);
        log.debug("logFile: {}", logFile.getAbsoluteFile());

        // 将python脚本写到临时文件
        File pythonFile = new File(getWorkspace(taskInfo), "python_" + IdUtil.getSnowflakeNextIdStr() + ".py") ;
        FileUtils.writeStringToFile(pythonFile, rawScript  , Charset.defaultCharset() , false);

        ShellHandle shellHandle = new ShellHandle("/bin/sh", "-c", "python " + pythonFile.getAbsolutePath());

        if(OSUtils.isWindows()){
            shellHandle = new ShellHandle("cmd.exe", "/C", "python " + pythonFile.getAbsolutePath());
        }

        shellHandle.setLogPath(logFile.getAbsolutePath());

        shellHandle.execute();
    }
}
