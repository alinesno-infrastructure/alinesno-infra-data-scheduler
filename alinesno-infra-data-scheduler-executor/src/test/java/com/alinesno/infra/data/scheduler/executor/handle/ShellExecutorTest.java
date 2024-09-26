package com.alinesno.infra.data.scheduler.executor.handle;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.exec.*;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Slf4j
public class ShellExecutorTest {

    @SneakyThrows
    @Test
    public void testExecute() {
        String rawScript = "ping www.baidu.com" ;

        log.debug("SQL Executor rawScript: {}", rawScript) ;

        // 创建命令行对象
        CommandLine cmdLine = CommandLine.parse(rawScript);

        // 创建执行器
        DefaultExecutor executor = DefaultExecutor.builder().get();

        // 设置输出流处理器
        // 设置输出流处理器
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PumpStreamHandler streamHandler = new PumpStreamHandler(outputStream);
        executor.setStreamHandler(streamHandler);

        // 设置超时时间（例如：5分钟）
        int timeoutInSeconds = 300;
        ExecuteWatchdog watchdog = ExecuteWatchdog.builder().setTimeout(Duration.ofSeconds(timeoutInSeconds)).get();
        executor.setWatchdog(watchdog);

        // 使用结果处理器来异步处理执行结果
        DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler() {
            @SneakyThrows
            @Override
            public void onProcessComplete(int exitValue) {
                if (exitValue == 0) {
                    log.info("命令执行成功, 输出: \n{}", outputStream.toString(StandardCharsets.UTF_8));
                } else {
                    log.error("命令执行失败, 退出码: {}, 输出: \n{}", exitValue, outputStream.toString(StandardCharsets.UTF_8));
                }
            }

            @Override
            public void onProcessFailed(ExecuteException e) {
                log.error("命令执行失败, 错误信息: {}", e.getMessage());
                log.error("错误输出: \n{}", outputStream.toString(StandardCharsets.UTF_8));
            }
        };

        // 执行命令
        try {
            executor.execute(cmdLine, resultHandler);
        } catch (ExecuteException e) {
            log.error("命令执行失败, 错误信息: {}", e.getMessage());
            log.error("错误输出: \n{}", outputStream.toString(StandardCharsets.UTF_8));
        }

        Thread.sleep(10000);
    }

}
