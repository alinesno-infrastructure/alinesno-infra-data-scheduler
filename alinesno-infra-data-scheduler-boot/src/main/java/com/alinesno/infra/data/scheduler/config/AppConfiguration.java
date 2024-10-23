package com.alinesno.infra.data.scheduler.config;

import com.alinesno.infra.common.extend.datasource.enable.EnableInfraDataScope;
import com.alinesno.infra.common.facade.enable.EnableActable;
import com.alinesno.infra.common.web.adapter.sso.enable.EnableInfraSsoApi;
import com.alinesno.infra.common.web.log.aspect.LogAspect;
import com.alinesno.infra.data.scheduler.executor.handle.StartFlowExecutor;
import com.alinesno.infra.data.scheduler.service.ICategoryService;
import com.dtflys.forest.springboot.annotation.ForestScan;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.beans.factory.annotation.Autowired;
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
@EnableInfraDataScope
@EnableInfraSsoApi
@MapperScan({
        "com.alinesno.infra.data.scheduler.quartz.mapper" ,
        "com.alinesno.infra.data.scheduler.mapper"
})
@ForestScan({
        "com.alinesno.infra.common.web.adapter.base.consumer" ,
        "com.alinesno.infra.data.scheduler.adapter"
})
@Configuration
public class AppConfiguration implements CommandLineRunner {

    @Autowired
    private StartFlowExecutor startFlowExecutor ;

    @Autowired
    private ICategoryService categoryService ;

    @Bean
    public LogAspect getLogAspect(){
        return new LogAspect() ;
    }

    @Override
    public void run(String... args) throws Exception {
        log.debug("alinesnoDataScheduler appConfiguration run ...:{},categoryService:{}" , startFlowExecutor,categoryService);
    }

}
