package com.alinesno.infra.data.scheduler.service;

import com.alinesno.infra.common.facade.services.IBaseService;
import com.alinesno.infra.data.scheduler.api.ProjectDto;
import com.alinesno.infra.data.scheduler.entity.ProjectEntity;

import java.util.List;

/**
 *
 *
 * @author luoxiaodong
 * @version 1.0.0
 */
public interface IProjectService extends IBaseService<ProjectEntity> {

    /**
     * 获取当前应用支持的文档类型
     * @param appCode
     * @return
     */
    List<String> getDocumentType(String appCode);

    /**
     * 保存应用文档类型
     * @param documentStr
     */
    void saveDocumentType(String projectId , String documentStr) ;

    /**
     * 初始化默认应用
     *
     * @param userId
     * @param orgId
     */
    void initDefaultApp(long userId, long orgId);

    /**
     * 获取到默认应用标识
     * @return
     */
    ProjectEntity getDefaultProject(long userId);

    /**
     * 判断应用是否开启
     * @param projectCode
     * @return
     */
    boolean isOpen(String projectCode);

    /**
     * 保存项目
     *
     * @param dto
     */
    void saveProject(ProjectDto dto);

    /**
     * 根据账号获取应用
     * @param userId
     * @return
     */
    ProjectEntity getProjectByAccountId(long userId);
}
