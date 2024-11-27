package com.alinesno.infra.data.scheduler.api.session;

import com.alinesno.infra.common.facade.pageable.ConditionDto;
import com.alinesno.infra.common.facade.pageable.DatatablesPageBean;
import com.alinesno.infra.common.web.log.utils.SpringUtils;
import com.alinesno.infra.data.scheduler.entity.ProjectAccountEntity;
import com.alinesno.infra.data.scheduler.entity.ProjectEntity;
import com.alinesno.infra.data.scheduler.service.IProjectAccountService;
import com.alinesno.infra.data.scheduler.service.IProjectService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * 获取当前应用
 *
 * @author luoxiaodong
 * @since 1.0.0
 */
@Slf4j
//@Component
public class CurrentProjectSession {

	public static ProjectEntity get() {
		// TODO 待处理账号获取异常的问题

		IProjectService managerProjectService = SpringUtils.getBean(IProjectService.class);

		//  CurrentAccountJwt.getUserId();
		long userId = 1L ;
		return managerProjectService.getProjectByAccountId(userId) ;
	}

	public static void set(long applicationId) {

		IProjectAccountService projectAccountService = SpringUtils.getBean(IProjectAccountService.class);

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

	public static void filterProject(DatatablesPageBean page) {
		// 过滤项目
		List<ConditionDto> condition = page.getConditionList() ;

		ConditionDto conditionDto = new ConditionDto()  ;
		conditionDto.setValue(CurrentProjectSession.get().getId()+"") ;
		conditionDto.setColumn("projectId");
		conditionDto.setType("eq");

		condition.add(conditionDto) ;

		page.setConditionList(condition);
	}
}
