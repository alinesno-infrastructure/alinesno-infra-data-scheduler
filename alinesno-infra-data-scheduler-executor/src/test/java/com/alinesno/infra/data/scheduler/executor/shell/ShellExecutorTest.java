package com.alinesno.infra.data.scheduler.executor.shell;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.io.IOException;

@Slf4j
public class ShellExecutorTest {

    @SneakyThrows
    @Test
    public void test() {
        // 多行命令，这里使用了 for 循环和其他命令作为示例
        String command =
                """
                #!/bin/sh
                echo "Start of script"
                for i in 1 2 3 4 5; do
                    echo "Number $i"
                    mvn clean package -f /Users/luodong/GitRepository/alinesno-infra-base-storage/pom.xml
                done
                echo "End of script"
                """;
        ShellHandle shellExecutor = new ShellHandle("/bin/sh", "-c", command);
        shellExecutor.execute();
        String output = shellExecutor.getOutput() ;
        System.out.println(output);
    }

    @Test
    public void execCommand() throws InterruptedException {

        try {
            String res = ShellHandle.execCommand("groups");
            System.out.println("thread id:" + Thread.currentThread().getId() + ", result:" + res.substring(0, 5));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void execCommandMultiple() throws InterruptedException, IOException {

        // 多行命令，这里使用了 for 循环和其他命令作为示例
        String command =
                """
                #!/bin/sh
                echo "Start of script"
                for i in 1 2 3 4 5; do
                    echo "Number $i"
                    mvn clean package -f /Users/luodong/GitRepository/alinesno-infra-base-storage/pom.xml
                done
                echo "End of script"
                """;

        String res = ShellHandle.execCommand("/bin/sh", "-c", command);
        System.out.println("thread id:" + Thread.currentThread().getId() + ", result:" + res.substring(0, 5));
    }
}
