package com.alinesno.infra.data.scheduler.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.common.facade.enums.HasStatusEnums;
import com.alinesno.infra.data.scheduler.api.ProjectDto;
import com.alinesno.infra.data.scheduler.entity.ProjectAccountEntity;
import com.alinesno.infra.data.scheduler.entity.ProjectEntity;
import com.alinesno.infra.data.scheduler.mapper.ProjectMapper;
import com.alinesno.infra.data.scheduler.service.IProjectAccountService;
import com.alinesno.infra.data.scheduler.service.IProjectService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
public class ProjectServiceImpl extends IBaseServiceImpl<ProjectEntity, ProjectMapper> implements IProjectService {

    private static final String DEFAULT_PROJECT_FIELD = "default" ;

    @Autowired
    private IProjectAccountService projectAccountService;

    /**
     * 获取支持的类型
     * @param appCode
     * @return
     */
    @Override
    public List<String> getDocumentType(String appCode) {

        ProjectEntity e = getOne(new LambdaQueryWrapper<ProjectEntity>().eq(ProjectEntity::getProjectCode , appCode)) ;

        if(e.getHasStatus() == HasStatusEnums.ILLEGAL.value){
            return List.of();
        }

        String documentStr = e.getDocumentType() ;

        return Arrays.asList(documentStr.split(","));
    }

    @Override
    public void saveDocumentType(String projectId, String documentStr) {

        ProjectEntity e = getById(projectId) ;
        e.setDocumentType(documentStr);

    }

    @Override
    public void initDefaultApp(long userId) {

        LambdaQueryWrapper<ProjectEntity> wrapper = new LambdaQueryWrapper<>() ;
        wrapper.eq(ProjectEntity::getOperatorId , userId)
                .eq(ProjectEntity::getFieldProp, DEFAULT_PROJECT_FIELD);

        long count = count(wrapper) ;
        if(count > 0){
            return ;
        }

        String code = IdUtil.getSnowflakeNextIdStr() ;

        ProjectEntity project = new ProjectEntity() ;

        project.setOperatorId(userId);
        project.setFieldProp(DEFAULT_PROJECT_FIELD);

        project.setProjectName("默认数据编排应用");
        project.setProjectDesc("包含所有的数据编排渠道查看权限，用于开发和验证场景");
        project.setProjectCode(code);

        save(project) ;
    }

    /**
     * 根据用户ID获取默认项目。
     *
     * @param userId 用户的ID，用于查询操作人与项目的关系。
     * @return 返回满足条件的默认项目实体，如果不存在，则返回null。
     *
     * 此方法通过查询满足特定条件（默认项目字段和操作人ID）的项目实体来实现。
     * 使用LambdaQueryWrapper来构建查询条件，以简化代码并提高可读性。
     */
    @Override
    public ProjectEntity getDefaultProject(long userId) {
        return getOne(new LambdaQueryWrapper<ProjectEntity>()
                .eq(ProjectEntity::getFieldProp , DEFAULT_PROJECT_FIELD)
                .eq(ProjectEntity::getOperatorId , userId));
    }

    /**
     * 检查项目是否处于合法状态。
     *
     * 本方法通过项目代码查询项目信息，并判断该项目的状态是否为合法状态。
     * 使用LambdaQueryWrapper进行条件查询，提高了代码的可读性和简洁性。
     *
     * @param projectCode 项目的唯一标识代码。
     * @return 如果项目处于合法状态，则返回true；否则返回false。
     */
    @Override
    public boolean isOpen(String projectCode) {
        // 根据项目代码查询项目实体
        ProjectEntity project = getOne(new LambdaQueryWrapper<ProjectEntity>()
                .eq(ProjectEntity::getProjectCode, projectCode));

        // 判断项目的状态是否为合法状态，返回相应的布尔值
        return project.getHasStatus() == HasStatusEnums.LEGAL.value;
    }

    @Override
public void saveProject(ProjectDto dto) {
    ProjectEntity project = new ProjectEntity();
    BeanUtils.copyProperties(dto, project);
    project.setProjectCode(IdUtil.getSnowflakeNextIdStr()); // 设置项目代码

    try {
        boolean result = save(project);
        if (result) {
            // 记录日志，保存成功
            log.info("项目保存成功，项目代码：{}", project.getProjectCode());
        } else {
            // 记录日志，保存失败
            log.error("项目保存失败，项目代码：{}", project.getProjectCode());
        }
    } catch (Exception e) {
        // 记录异常日志
        log.error("保存项目时发生异常", e);
    }
}

@Override
public ProjectEntity getProjectByAccountId(long userId) {
    LambdaQueryWrapper<ProjectAccountEntity> lambdaQueryWrapper = new LambdaQueryWrapper<>() ;

    lambdaQueryWrapper.eq(ProjectAccountEntity::getAccountId , userId)
            .orderByDesc(ProjectAccountEntity::getAppOrder) ;

    List<ProjectAccountEntity> es = projectAccountService.list(lambdaQueryWrapper) ;

    return CollectionUtils.isEmpty(es) ? getDefaultProject(userId) : getById(es.get(0).getApplicationId());
}


}
