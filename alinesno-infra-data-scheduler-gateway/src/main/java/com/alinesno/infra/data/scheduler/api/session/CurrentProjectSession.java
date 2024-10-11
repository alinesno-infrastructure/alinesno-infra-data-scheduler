package com.alinesno.infra.data.scheduler.api.session;

import com.alinesno.infra.data.scheduler.entity.ProjectAccountEntity;
import com.alinesno.infra.data.scheduler.entity.ProjectEntity;
import com.alinesno.infra.data.scheduler.service.IProjectAccountService;
import com.alinesno.infra.data.scheduler.service.IProjectService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 获取当前应用
 *
 * @author luoxiaodong
 * @since 1.0.0
 */
@Slf4j
@Component
public class CurrentProjectSession {

	@Autowired
	private IProjectAccountService projectAccountService;  ;

	@Autowired
	private IProjectService managerProjectService ;

	public ProjectEntity get() {
		// TODO 待处理账号获取异常的问题

		//  CurrentAccountJwt.getUserId();
		long userId = 1L ;
		return managerProjectService.getProjectByAccountId(userId) ;
	}

	public void set(long applicationId) {

		//  CurrentAccountJwt.getUserId();
		long userId = 1L ;

		// 查询当前用户配置记录
		long count = projectAccountService.count(new LambdaQueryWrapper<ProjectAccountEntity>()
				.eq(ProjectAccountEntity::getAccountId , userId)) ;

		ProjectAccountEntity e = new ProjectAccountEntity() ;

		e.setAccountId(userId);
		e.setApplicationId(applicationId);
		e.setAppOrder(count+1);

		projectAccountService.save(e);
	}

}
