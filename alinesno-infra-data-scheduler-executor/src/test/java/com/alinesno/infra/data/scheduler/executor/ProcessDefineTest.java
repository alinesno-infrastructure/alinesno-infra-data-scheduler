package com.alinesno.infra.data.scheduler.executor;

import com.alinesno.infra.data.scheduler.entity.ProcessDefinitionEntity;
import com.alinesno.infra.data.scheduler.entity.TaskDefinitionEntity;
import com.alinesno.infra.data.scheduler.enums.ExecutorTypeEnums;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * ProcessDefineTest
 */
@Slf4j
public class ProcessDefineTest {

    @Test
    public void processExecutorTest() {

        // 创建并初始化一个新的ProcessDefinitionEntity对象
        ProcessDefinitionEntity processDefinition = new ProcessDefinitionEntity()
                .setName("Sample Process")
                .setDescription("This is a sample process for demonstration purposes.")
                .setProjectCode("PROJECT-001")
                .setGlobalParams("{\"param1\":\"value1\",\"param2\":\"value2\"}")
                .setTimeout(300) // 5 minutes
                .setScheduleCron("0 0/5 * * * ?"); // 每5分钟执行一次

        // 打印出创建的对象以验证
        System.out.println(processDefinition);

        List<TaskDefinitionEntity> taskDefinitionEntities = new ArrayList<>() ;

        // 添加shell任务
        TaskDefinitionEntity shellTask = new TaskDefinitionEntity();
        shellTask.setName("ShellTask");
        shellTask.setTaskType(ExecutorTypeEnums.SHELL_SCRIPT.getCode());
        shellTask.setTaskParams("{\"command\":\"echo Shell task executing with 1\"}");
        taskDefinitionEntities.add(shellTask);

        // 添加python任务
        TaskDefinitionEntity pythonTask = new TaskDefinitionEntity();
        pythonTask.setName("PythonTask");
        pythonTask.setTaskType(ExecutorTypeEnums.PYTHON_SCRIPT.getCode());
        pythonTask.setTaskParams("{\"command\":\"print('Python task executing with 2')\"}");
        taskDefinitionEntities.add(pythonTask);

        // 添加sql任务
        TaskDefinitionEntity sqlTask = new TaskDefinitionEntity();
        sqlTask.setName("SqlTask");
        sqlTask.setTaskType(ExecutorTypeEnums.SQL_SCRIPT.getCode());
        sqlTask.setTaskParams("{\"command\":\"SELECT 'Sql task executing with 3' AS message;\"}");
        taskDefinitionEntities.add(sqlTask);

        // 添加http任务
        TaskDefinitionEntity httpTask = new TaskDefinitionEntity();
        httpTask.setName("HttpTask");
        httpTask.setTaskType(ExecutorTypeEnums.HTTP_REQUEST.getCode());
        httpTask.setTaskParams("{\"url\":\"http://example.com\",\"method\":\"GET\",\"headers\":{},\"body\":\"\",\"timeout\":60000,\"connectTimeout\":60000,\"followRedirects\":true,\"maxRedirects\":5,\"retryCount\":3,\"retryInterval\":1000,\"ignoreSslErrors\":false,\"command\":\"echo Http task executing with 4\"}");
        taskDefinitionEntities.add(httpTask);

        // 添加jar任务
        TaskDefinitionEntity jarTask = new TaskDefinitionEntity();
        jarTask.setName("JarTask");
        jarTask.setTaskType(ExecutorTypeEnums.JAR_EXECUTION.getCode());
        jarTask.setTaskParams("{\"mainClass\":\"com.example.Main\",\"args\":[\"-Dparam=Jar task executing with 5\"]}");
        taskDefinitionEntities.add(jarTask);

        // 添加maven任务
        TaskDefinitionEntity mavenTask = new TaskDefinitionEntity();
        mavenTask.setName("MavenTask");
        mavenTask.setTaskType(ExecutorTypeEnums.MAVEN_COMMAND.getCode());
        mavenTask.setTaskParams("{\"goals\":\"clean install\",\"arguments\":\"-Dmessage=Maven task executing with 6\",\"workingDirectory\":\"/path/to/project\"}");
        taskDefinitionEntities.add(mavenTask);

        // 打印taskDefinitionEntities
        for (TaskDefinitionEntity task : taskDefinitionEntities) {
            System.out.println(task);
        }

    }

}
