package com.alinesno.infra.data.scheduler;

import cn.hutool.core.util.IdUtil;
import com.alinesno.infra.data.scheduler.trigger.entity.JobEntity;
import com.alinesno.infra.data.scheduler.trigger.entity.TriggerEntity;
import com.alinesno.infra.data.scheduler.trigger.service.IJobService;
import com.alinesno.infra.data.scheduler.trigger.service.ITriggerService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 集成一个Java开发示例工具
 */
@SpringBootApplication
@EnableScheduling
public class DataSchedulerApplication {

	public static void main(String[] args) {
		SpringApplication.run(DataSchedulerApplication.class, args);
	}

	@Bean
	CommandLineRunner init(IJobService jobService, ITriggerService triggerService) {
		return args -> {
			if (jobService.count() == 0) {
				JobEntity job1 = new JobEntity();
				job1.setId(IdUtil.getSnowflakeNextId());
				job1.setName("Demo Job 1");
				jobService.save(job1);

				JobEntity job2 = new JobEntity();
				job2.setId(IdUtil.getSnowflakeNextId());
				job2.setName("Demo Job 2");
				jobService.save(job2);

				TriggerEntity t1 = new TriggerEntity();
				t1.setId(IdUtil.getSnowflakeNextId());
				t1.setJobId(job1.getId());
				t1.setCron("H * * * *"); // 5-field
				triggerService.save(t1);

				TriggerEntity t2 = new TriggerEntity();
				t2.setId(IdUtil.getSnowflakeNextId());
				t2.setJobId(job2.getId());
				t2.setCron("0 H * * *");
				triggerService.save(t2);

				TriggerEntity t3 = new TriggerEntity();
				t3.setId(IdUtil.getSnowflakeNextId());
				t3.setJobId(job2.getId());
				t3.setCron("H(10-20) H/2 * * MON-FRI");
				triggerService.save(t3);
			}
		};
	}
}