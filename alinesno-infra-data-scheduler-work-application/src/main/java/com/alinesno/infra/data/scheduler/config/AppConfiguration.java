package com.alinesno.infra.data.scheduler.config;

import com.alinesno.infra.common.core.auto.EnableCore;
import com.alinesno.infra.common.extend.datasource.enable.EnableInfraDataScope;
import com.alinesno.infra.common.facade.enable.EnableActable;
import com.dtflys.forest.springboot.annotation.ForestScan;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;

/**
 * 应用配置
 *
 * @author luoxiaodong
 * @version 1.0.0
 */
@Slf4j
@EnableActable
@EnableInfraDataScope
@MapperScan({
        "com.alinesno.infra.data.scheduler.quartz.mapper" ,
        "com.alinesno.infra.data.scheduler.llm.mapper",
        "com.alinesno.infra.data.scheduler.notice.mapper",
        "com.alinesno.infra.data.scheduler.workflow.mapper"
})
@ForestScan({
        "com.alinesno.infra.data.scheduler.adapter"
})
@EnableCore
@Configuration
public class AppConfiguration implements CommandLineRunner {

    @Override
    public void run(String... args) throws Exception {
        log.debug("alinesnoDataScheduler Worker appConfiguration run ...");
    }

}
