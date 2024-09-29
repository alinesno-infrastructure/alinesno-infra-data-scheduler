package com.alinesno.infra.data.scheduler.executor.shell;

import cn.hutool.core.util.IdUtil;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

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
                    mvn clean package -f pom.xml
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
                    mvn clean package -f pom.xml
                done
                echo "End of script"
                """;

        String res = ShellHandle.execCommand("/bin/sh", "-c", command);
        System.out.println("thread id:" + Thread.currentThread().getId() + ", result:" + res.substring(0, 5));
    }

    // 测试执行 python
    @Test
    public void testPython() throws IOException, InterruptedException {

        // 多行的python脚本
        String command =
              """
              # 定义一个数字列表
              numbers = [1, 2, 3, 4, 5]
              
              # 使用 for 循环遍历列表中的每个数字
              for number in numbers:
                  # 计算当前数字的平方
                  square = number ** 2
                  # 打印数字和它的平方
                  print(f"数字 {number} 的平方是 {square}")
              """ ;

        // 将python脚本写到临时文件
        String tempFile = "/tmp/test" + IdUtil.getSnowflakeNextIdStr() + ".py" ;
        FileUtils.writeStringToFile(new File(tempFile), command  , Charset.defaultCharset() , false);

        String res = ShellHandle.execCommand("cmd.exe", "/C", "python " + tempFile);
        System.out.println("thread id:" + Thread.currentThread().getId() + ", result:" + new String(res.getBytes() , StandardCharsets.UTF_8));
    }
}
