package com.alinesno.infra.data.scheduler.executor.handle;

import com.alinesno.infra.data.scheduler.api.ParamsDto;
import com.alinesno.infra.data.scheduler.executor.AbstractExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.maven.shared.invoker.*;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.Collections;

@Slf4j
@Service("mavenExecutor")
public class MavenExecutor extends AbstractExecutorService {

    @SneakyThrows
    @Override
    public void execute(TaskInfoBean task) {
        configTaskParams(task, this);
        log.debug("maven executor");

        ParamsDto params = getParamsDto() ;

        String pomXml = params.getPomXml() == null ? "pom.xml" : params.getPomXml() ;
        String goals = params.getGoals() == null ? "clean package" : params.getGoals();

        log.debug("pomXml:{} , goals:{}" , pomXml , goals) ;

        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(new File(pomXml));
        request.setGoals(Collections.singletonList(goals));

        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(new File(getM2Home())) ;

        invoker.setLogger(new PrintStreamLogger(System.err, InvokerLogger.ERROR) {

        });

        invoker.setOutputHandler(s -> {
            log.debug("-->> {}" , s);
            writeLog(s);
        });

        invoker.execute(request);
        if (invoker.execute(request).getExitCode() == 0) {
            log.debug("success");
        } else {
            log.error("error");
        }
    }

}
