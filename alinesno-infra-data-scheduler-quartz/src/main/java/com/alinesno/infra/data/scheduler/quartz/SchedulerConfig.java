package com.alinesno.infra.data.scheduler.quartz;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;
import java.io.IOException;

/**
 * 配置调度器的类
 *
 * @author luoxiaodong
 * @version 1.0.0
 */
@Configuration
public class SchedulerConfig {

   @Autowired
   private DataSource dataSource;

   /**
    * 创建SchedulerFactoryBean实例
    *
    * @return 创建的SchedulerFactoryBean实例
    * @throws IOException 如果创建过程中出现IO异常则抛出
    */
   @Bean
   public SchedulerFactoryBean schedulerFactoryBean() throws IOException {
      SchedulerFactoryBean factory = new SchedulerFactoryBean();
      factory.setSchedulerName("Cluster_Data_Scheduler");
      factory.setDataSource(dataSource);
      factory.setApplicationContextSchedulerContextKey("applicationContext");
      factory.setAutoStartup(true);

      factory.setStartupDelay(1); // 延迟启动调度器
      return factory;
   }

}
