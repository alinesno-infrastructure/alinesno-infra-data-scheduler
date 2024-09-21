package com.alinesno.infra.data.scheduler.config;

import com.alinesno.infra.common.facade.enable.EnableActable;
import com.alinesno.infra.common.web.adapter.sso.enable.EnableInfraSsoApi;
import com.alinesno.infra.common.web.log.aspect.LogAspect;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 应用配置
 *
 * @author luoxiaodong
 * @version 1.0.0
 */
@Slf4j
@EnableActable
@EnableInfraSsoApi
@MapperScan({
        "com.alinesno.infra.data.scheduler.scheduler.mapper" ,
        "com.alinesno.infra.data.scheduler.mapper"
})
@Configuration
public class AppConfiguration implements CommandLineRunner {

    @Bean
    public LogAspect getLogAspect(){
        return new LogAspect() ;
    }

    @Override
    public void run(String... args) throws Exception {
        log.debug("AlinesnoDataScheduler AppConfiguration run ...");
    }
}
