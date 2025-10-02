package com.alinesno.infra.data.scheduler.im.controller;

import com.alinesno.infra.common.core.constants.SpringInstanceScope;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionQuery;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionSave;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionScope;
import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.facade.pageable.DatatablesPageBean;
import com.alinesno.infra.common.facade.pageable.TableDataInfo;
import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.common.web.adapter.rest.BaseController;
import com.alinesno.infra.data.scheduler.entity.NotificationConfigEntity;
import com.alinesno.infra.data.scheduler.im.dto.NotificationConfigSelectDto;
import com.alinesno.infra.data.scheduler.im.service.INotificationConfigService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.swagger.annotations.Api;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * 通知配置 Controller（返回 AjaxResult，列表脱敏，编辑获取解密值）
 */
@Slf4j
@Api(tags = "NotificationConfig")
@RestController
@Scope(SpringInstanceScope.PROTOTYPE)
@RequestMapping("/api/infra/data/scheduler/notificationConfig")
public class NotificationConfigController extends BaseController<NotificationConfigEntity, INotificationConfigService> {

    @Autowired
    private INotificationConfigService service;

    @DataPermissionScope
    @ResponseBody
    @PostMapping("/datatables")
    public TableDataInfo datatables(HttpServletRequest request, Model model, DatatablesPageBean page) {
        log.debug("page = {}", ToStringBuilder.reflectionToString(page));
        // 如果 datatables 需要脱敏，确保 service 的分页查询也调用 maskSensitive 逻辑。
        return this.toPage(model, this.getFeign(), page);
    }

    @Override
    public INotificationConfigService getFeign() {
        return this.service;
    }

    /**
     * 获取单条配置（用于编辑，返回解密后的副本）
     */
    @GetMapping("/getConfig/{id}")
    public AjaxResult getConfigById(@PathVariable Serializable id) {
        NotificationConfigEntity cfg = service.getDecryptedById(id);
        return AjaxResult.success("ok", cfg);
    }

    /**
     * 保存或更新配置；前端可以用同一接口做新增和编辑（根据 id 判定）
     */
    @DataPermissionSave
    @PostMapping("/saveConfig")
    public AjaxResult saveConfig(@RequestBody NotificationConfigEntity entity) {
        boolean ok = service.saveOrUpdate(entity);
        return AjaxResult.success("保存成功");
    }

    /**
     * 删除配置
     */
    @DeleteMapping("/removeConfig/{id}")
    public AjaxResult removeConfig(@PathVariable Serializable id) {
        boolean ok = service.removeById(id);
        return AjaxResult.success("删除成功");
    }

    /**
     * 获取所有IM列表
     */
    @DataPermissionQuery
    @GetMapping("/listAllIM")
    public AjaxResult listAllIM(PermissionQuery  query){
        LambdaQueryWrapper<NotificationConfigEntity> wrapper = new LambdaQueryWrapper<>() ;
        wrapper.setEntityClass(NotificationConfigEntity.class) ;
        query.toWrapper(wrapper);

        List<NotificationConfigEntity> list = service.list(wrapper) ;
        List<NotificationConfigSelectDto> listDto = NotificationConfigSelectDto.convert(list) ;

        return AjaxResult.success("操作成功", listDto) ;
    }

    /**
     * 简易列表（无 datatables），用于前端下拉或选择器，返回脱敏结果
     */
    @DataPermissionQuery
    @PostMapping("/list")
    public AjaxResult list(@RequestBody(required = false) Map<String, Object> params , PermissionQuery query) {
        try {
            String provider = params == null ? null : (String) params.get("provider");
            List<NotificationConfigEntity> list = service.listByProvider(provider , query);
            return AjaxResult.success("ok", list);
        } catch (Exception ex) {
            log.error("list notification config error", ex);
            return AjaxResult.error("查询失败: " + ex.getMessage());
        }
    }
}