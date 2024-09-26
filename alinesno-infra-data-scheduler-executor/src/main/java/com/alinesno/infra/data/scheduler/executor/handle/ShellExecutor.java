package com.alinesno.infra.data.scheduler.executor.handle;

import com.alinesno.infra.data.scheduler.api.ParamsDto;
import com.alinesno.infra.data.scheduler.executor.BaseExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.LinkedList;
import java.util.List;

@Slf4j
@Service("shellExecutor")
public class ShellExecutor extends BaseExecutorService {

    @SneakyThrows
    @Override
    public void execute(TaskInfoBean taskInfo) {

        ParamsDto paramsDto = getParamsDto(taskInfo) ;
        String rawScript = paramsDto.getRawScript();

        log.debug("SQL Executor rawScript: {}", rawScript) ;

        // 设置命令行
        CommandLine cmdLine = CommandLine.parse(rawScript);

        // 创建用于捕获输出的流
        CollectingLogOutputStream outputStream = new CollectingLogOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
//        PumpStreamHandler streamHandler = new PumpStreamHandler(new CollectingLogOutputStream());

        // 设置执行器
        DefaultExecutor executor = DefaultExecutor.builder().get();
        executor.setStreamHandler(streamHandler);

        // 设置超时时间
        int timeoutInSeconds = 300;
        ExecuteWatchdog watchdog = ExecuteWatchdog.builder().setTimeout(Duration.ofSeconds(timeoutInSeconds)).get();
        executor.setWatchdog(watchdog);

        try {
            // 执行命令
            executor.execute(cmdLine);
            // 输出命令执行结果
            // log.debug("命令输出: " + outputStream.toString("GBK")) ;
        } catch (ExecuteException e) {
            log.error("命令执行失败: " + e.getMessage());
        } catch (Exception e) {
            log.error("运行异常" , e);
        }
    }

    @Getter
    static class CollectingLogOutputStream extends LogOutputStream {
        private final List<String> lines = new LinkedList<String>();
        @Override
        protected void processLine(String line, int level) {
            lines.add(line);
            log.debug("-->> {}" , line);
        }
    }
}