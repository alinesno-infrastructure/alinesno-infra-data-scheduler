package com.alinesno.infra.data.scheduler.executor.handle;

import com.alinesno.infra.data.scheduler.executor.AbstractExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * K8S操作执行器
 */
@Slf4j
@Service("groovyExecutor")
public class GroovyExecutor extends AbstractExecutorService {

    @Override
    public void execute(TaskInfoBean task) {
        log.debug("groovyExecutor execute");

        PrintStream oldPrintStream = System.out; //将原来的System.out交给printStream 对象保存
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(bos)); //设置新的out

        String rawScript = readerRawScript() ;

        // 创建 Binding 对象，用于绑定变量到 Groovy 脚本
        Binding binding = new Binding();

        binding.setVariable("taskInfo", task);
        binding.setVariable("executorService", this);
        binding.setVariable("log", log);

        // 创建 GroovyShell 实例
        GroovyShell shell = new GroovyShell(this.getClass().getClassLoader(), binding);

        // 执行 Groovy 脚本
        shell.evaluate(rawScript) ;

        System.setOut(oldPrintStream); //恢复原来的System.out
        System.out.println(bos); //将bos中保存的信息输出,这就是我们上面准备要输出的内容

    }
}
