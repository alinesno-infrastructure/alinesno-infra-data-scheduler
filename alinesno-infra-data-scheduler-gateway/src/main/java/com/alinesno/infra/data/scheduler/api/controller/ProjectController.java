package com.alinesno.infra.data.scheduler.api.controller;

import com.alinesno.infra.common.core.constants.SpringInstanceScope;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionSave;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionScope;
import com.alinesno.infra.common.facade.pageable.DatatablesPageBean;
import com.alinesno.infra.common.facade.pageable.TableDataInfo;
import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.common.web.adapter.login.account.CurrentAccountBean;
import com.alinesno.infra.common.web.adapter.login.account.CurrentAccountJwt;
import com.alinesno.infra.common.web.adapter.login.annotation.CurrentAccount;
import com.alinesno.infra.common.web.adapter.rest.BaseController;
import com.alinesno.infra.data.scheduler.api.ProjectDto;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 处理与ApplicationEntity相关的请求的Controller。
 * 继承自BaseController类并实现IApplicationService接口。
 *
 * @author luoxiaodong
 * @version 1.0.0
 */
@Slf4j
@Api(tags = "Application")
@RestController
@Scope(SpringInstanceScope.PROTOTYPE)
@RequestMapping("/api/infra/data/scheduler/project")
public class ProjectController /* extends BaseController<ProjectEntity, IProjectService> */ {

//    @Autowired
//    private IProjectService service;

//    @Autowired
//    private CurrentProjectSession currentProjectSession ;

//    /**
//     * 获取ApplicationEntity的DataTables数据。
//     *
//     * @param request HttpServletRequest对象。
//     * @param model   Model对象。
//     * @param page    DatatablesPageBean对象。
//     * @return 包含DataTables数据的TableDataInfo对象。
//     */
//    @DataPermissionScope
//    @ResponseBody
//    @PostMapping("/datatables")
//    public TableDataInfo datatables(HttpServletRequest request,
//                                    @CurrentAccount CurrentAccountBean currentAccount ,
//                                    Model model,
//                                    DatatablesPageBean page) {
//        log.debug("page = {}", ToStringBuilder.reflectionToString(page));
//
//        long userId = currentAccount.getId() ;
//        long count = service.count(new LambdaQueryWrapper<ProjectEntity>()
//                        .eq(ProjectEntity::getOrgId, currentAccount.getOrgId()));
//
//        // 初始化默认应用
//        if (count == 0) {
//            service.initDefaultApp(userId, currentAccount.getOrgId()) ;
//        }
//
//        return this.toPage(model, this.getFeign(), page);
//    }


    /**
     * 获取当前应用
     * @return
     */
    @GetMapping("/currentProject")
    public AjaxResult currentApplication(@CurrentAccount CurrentAccountBean currentAccount) {

//        service.initDefaultApp(currentAccount.getId(), currentAccount.getOrgId()) ;
//
//        ProjectEntity e =  CurrentProjectSession.get() ;
//
//        String defaultIcon = "fa-solid fa-file-shield" ;
//        if(e.getProjectIcons() == null){
//            e.setProjectIcons(defaultIcon);
//        }

        return AjaxResult.success();
    }

    /**
     * 保存项目 projectDesc
     */
    @DataPermissionSave
    @PostMapping("/saveProject")
    public AjaxResult saveProject(@RequestBody @Validated ProjectDto dto){
        log.info("saveProject projectEntity = {}" ,  dto) ;
//        service.saveProject(dto) ;
        return AjaxResult.success("操作成功.") ;
    }

//    /**
//     * 选择应用
//     * @return
//     */
//    @GetMapping("/choiceProject")
//    public AjaxResult choiceProject(Long projectId) {
//        CurrentProjectSession.set(projectId);
//        return ok() ;
//    }
//
//    /**
//     * 获取默认应用地址
//     * @return
//     */
//    @GetMapping("/defaultProject")
//    public AjaxResult defaultProject(){
//        long userId = CurrentAccountJwt.getUserId();
//        ProjectEntity e = service.getDefaultProject(userId) ;
//        return AjaxResult.success("操作成功." , e.getProjectCode()) ;
//    }
//
//    @Override
//    public IProjectService getFeign() {
//        return this.service;
//    }
}
