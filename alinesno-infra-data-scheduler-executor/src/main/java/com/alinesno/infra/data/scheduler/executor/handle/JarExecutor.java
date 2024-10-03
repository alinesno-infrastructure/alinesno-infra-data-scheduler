package com.alinesno.infra.data.scheduler.executor.handle;

import com.alinesno.infra.data.scheduler.executor.AbstractExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service("jarExecutor")
public class JarExecutor extends AbstractExecutorService {

    @SneakyThrows
    @Override
    public void execute(TaskInfoBean taskInfo) {
        log.debug("jarExecutor execute");

        for (String resource : getResources()) {
            writeLog("resource: " + resource);

            // 设置命令行
            String command = "java -jar "  + resource ;
            runCommand(command) ;
        }
    }

}
