package com.alinesno.infra.data.scheduler.service.impl;

import cn.hutool.core.util.IdUtil;
import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.common.core.utils.StringUtils;
import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.data.scheduler.api.TreeSelectDto;
import com.alinesno.infra.data.scheduler.entity.CategoryEntity;
import com.alinesno.infra.data.scheduler.mapper.CategoryMapper;
import com.alinesno.infra.data.scheduler.service.ICategoryService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 【请填写功能名称】Service业务层处理
 *
 * @version 1.0.0
 * @autor luoxiaodong
 */
@Slf4j
@Service
public class CategoryServiceImpl extends IBaseServiceImpl<CategoryEntity, CategoryMapper> implements ICategoryService {

    @Override
    public List<CategoryEntity> selectCatalogList(CategoryEntity promptCatalog, PermissionQuery query) {

        LambdaQueryWrapper<CategoryEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.setEntityClass(CategoryEntity.class) ;
        query.toWrapper(queryWrapper);

        long count = count(queryWrapper);

        if(promptCatalog.getName() != null && !promptCatalog.getName().isEmpty()){
            queryWrapper.like(CategoryEntity::getName, promptCatalog.getName());
        }

        List<CategoryEntity> list = list(queryWrapper);

        if(count == 0){

            list = new ArrayList<>() ;

            // 创建默认父类【数据编排】
            CategoryEntity parent = new CategoryEntity() ;
            BeanUtils.copyProperties(query ,  parent);
            parent.setName("数据编排");
            parent.setDescription("数据编排是数据处理和可视化的解决方案，用于处理和可视化数据。"); // 50个字的描述
            parent.setId(IdUtil.getSnowflakeNextId());
            parent.setDataSecurityLevel("2");
            parent.setParentId(0L); // 标识为顶级节点
            list.add(parent) ;

            // 创建默认子类【默认业务】，作为【数据编排】的子节点
            CategoryEntity child = new CategoryEntity();
            BeanUtils.copyProperties(query ,  child);
            child.setName("默认业务");
            child.setDescription("默认业务是数据处理和可视化的默认业务。");
            child.setId(IdUtil.getSnowflakeNextId()); // 给一个唯一ID
            child.setParentId(parent.getId()); // 设置父节点为【数据编排】
            child.setAncestors(String.valueOf(parent.getId())); // 设置祖先为父节点ID
            child.setDataSecurityLevel("2");
            list.add(child);

            // 如果需要将默认分类保存到数据库，可以在这里添加保存逻辑
             this.saveBatch(list);
        }

        return list ;

    }

    @Override
    public void insertCatalog(CategoryEntity entity) {

        CategoryEntity info = this.getById(entity.getParentId());
        if(info != null){
            entity.setAncestors(info.getAncestors() + "," + entity.getParentId());
        }

        this.save(entity) ;
    }

    @Override
    public List<TreeSelectDto> selectCatalogTreeList(PermissionQuery query) {

        LambdaQueryWrapper<CategoryEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.setEntityClass(CategoryEntity.class) ;
        query.toWrapper(queryWrapper);

        List<CategoryEntity> deptTrees = buildDeptTree(list(queryWrapper));
        return deptTrees.stream().map(TreeSelectDto::new).collect(Collectors.toList());
    }

    /**
     * 构建前端所需要树结构
     *
     * @param prompts 部门列表
     * @return 树结构列表
     */
    public List<CategoryEntity> buildDeptTree(List<CategoryEntity> prompts) {

        List<CategoryEntity> returnList = new ArrayList<>();
        List<Long> tempList = prompts.stream().map(CategoryEntity::getId).toList();

        for (CategoryEntity dept : prompts) {
            // 如果是顶级节点, 遍历该父节点的所有子节点
            if (!tempList.contains(dept.getParentId())) {
                recursionFn(prompts, dept);
                returnList.add(dept);
            }
        }

        if (returnList.isEmpty()) {
            returnList = prompts;
        }
        return returnList;
    }

    /**
     * 递归列表
     */
    private void recursionFn(List<CategoryEntity> list, CategoryEntity t) {
        // 得到子节点列表
        List<CategoryEntity> childList = getChildList(list, t);
        t.setChildren(childList);
        for (CategoryEntity tChild : childList) {
            if (hasChild(list, tChild))
            {
                recursionFn(list, tChild);
            }
        }
    }

    /**
     * 得到子节点列表
     */
    private List<CategoryEntity> getChildList(List<CategoryEntity> list, CategoryEntity t) {
        List<CategoryEntity> tlist = new ArrayList<>();
        for (CategoryEntity n : list) {
            if (StringUtils.isNotNull(n.getParentId()) && n.getParentId().longValue() == t.getId().longValue()) {
                tlist.add(n);
            }
        }
        return tlist;
    }

    /**
     * 判断是否有子节点
     */
    private boolean hasChild(List<CategoryEntity> list, CategoryEntity t) {
        return !getChildList(list, t).isEmpty();
    }

}