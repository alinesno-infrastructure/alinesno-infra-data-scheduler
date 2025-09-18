package com.alinesno.infra.data.scheduler.api.controller;

import com.alinesno.infra.common.core.constants.SpringInstanceScope;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionQuery;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionSave;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionScope;
import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.facade.pageable.DatatablesPageBean;
import com.alinesno.infra.common.facade.pageable.TableDataInfo;
import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.common.web.adapter.rest.BaseController;
import com.alinesno.infra.data.scheduler.entity.CategoryEntity;
import com.alinesno.infra.data.scheduler.service.ICategoryService;
import io.swagger.annotations.Api;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 处理与CategoryEntity相关的请求的Controller。
 * 继承自BaseController类并实现ICategoryService接口。
 *
 * @version 1.0.0
 * @author  luoxiaodong
 */
@Slf4j
@Api(tags = "Category")
@RestController
@Scope(SpringInstanceScope.PROTOTYPE)
@RequestMapping("/api/infra/data/scheduler/category")
public class CategoryController extends BaseController<CategoryEntity, ICategoryService> {

    @Autowired
    private ICategoryService service;

    /**
     * 获取CategoryEntity的DataTables数据。
     *
     * @param request HttpServletRequest对象。
     * @param model Model对象。
     * @param page DatatablesPageBean对象。
     * @return 包含DataTables数据的TableDataInfo对象。
     */
    @DataPermissionScope
    @ResponseBody
    @PostMapping("/datatables")
    public TableDataInfo datatables(HttpServletRequest request, Model model, DatatablesPageBean page) {
        log.debug("page = {}", ToStringBuilder.reflectionToString(page));
        return this.toPage(model, this.getFeign() , page) ;
    }

    @DataPermissionQuery
    @GetMapping("/list")
    public AjaxResult list(CategoryEntity promptCatalog , PermissionQuery query) {
        List<CategoryEntity> promptCatalogEntities = service.selectCatalogList(promptCatalog , query);

        return AjaxResult.success("操作成功." , promptCatalogEntities) ;
    }

    /**
     * 保存角色类型
     * @return
     */
    @DataPermissionSave
    @PostMapping("/insertCatalog")
    public AjaxResult insertCatalog(@RequestBody CategoryEntity entity){

//        long currentProject = CurrentProjectSession.get().getId() ;
//        entity.setProjectId(currentProject);

        service.insertCatalog(entity) ;

        return AjaxResult.success("操作成功.") ;
    }

    /**
     * 获取到子类
     * @param deptId
     * @return
     */
    @DataPermissionQuery
    @GetMapping("/excludeChild/{id}")
    public AjaxResult excludeChild(@PathVariable(value = "id", required = false) Long deptId , PermissionQuery query) {

        List<CategoryEntity> depts = service.selectCatalogList(new CategoryEntity(), query);
        depts.removeIf(d -> d.getId().longValue() == deptId || ArrayUtils.contains(StringUtils.split(d.getAncestors(), ","), deptId + ""));

        return AjaxResult.success("操作成功." , depts);
    }


    @Override
    public ICategoryService getFeign() {
        return this.service;
    }
}
