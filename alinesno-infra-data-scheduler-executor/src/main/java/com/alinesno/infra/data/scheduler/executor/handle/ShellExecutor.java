package com.alinesno.infra.data.scheduler.executor.handle;

import com.alinesno.infra.data.scheduler.api.ParamsDto;
import com.alinesno.infra.data.scheduler.executor.AbstractExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("shellExecutor")
public class ShellExecutor extends AbstractExecutorService {

    @SneakyThrows
    @Override
    public void execute(TaskInfoBean taskInfo) {

        ParamsDto paramsDto = getParamsDto() ;
        String rawScript = paramsDto.getRawScript();

        log.debug("Shell Executor rawScript: {}", rawScript) ;

        // 构建多行命令行
        String command = """
                cd %s
                %s
                """.formatted(getWorkspace() , rawScript);

        writeLog("执行SQL:" +command);
        runCommand(command);
    }

}