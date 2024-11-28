package com.alinesno.infra.data.scheduler.service;

import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.facade.services.IBaseService;
import com.alinesno.infra.data.scheduler.api.TreeSelectDto;
import com.alinesno.infra.data.scheduler.entity.CategoryEntity;

import java.util.List;

/**
 * 【请填写功能名称】Service接口
 *
 * @version 1.0.0
 * @autor luoxiaodong
 */
public interface ICategoryService extends IBaseService<CategoryEntity> {
    /**
     * 查询出指令类型列表
     *
     * @param promptCatalog
     * @param query
     * @param currentProject
     * @return
     */
    List<CategoryEntity> selectCatalogList(CategoryEntity promptCatalog, PermissionQuery query, long currentProject);

    /**
     * 保存用户类型
     * @param entity
     */
    void insertCatalog(CategoryEntity entity);

    /**
     * 查询类型列表树
     * @return
     */
    List<TreeSelectDto> selectCatalogTreeList(PermissionQuery query, long currentProject);
    
}
