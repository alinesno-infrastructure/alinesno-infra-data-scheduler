package com.alinesno.infra.data.scheduler.api.controller;

import com.alinesno.infra.common.core.constants.SpringInstanceScope;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionScope;
import com.alinesno.infra.common.facade.pageable.ConditionDto;
import com.alinesno.infra.common.facade.pageable.DatatablesPageBean;
import com.alinesno.infra.common.facade.pageable.TableDataInfo;
import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.common.web.adapter.rest.BaseController;
import com.alinesno.infra.data.scheduler.api.session.CurrentProjectSession;
import com.alinesno.infra.data.scheduler.entity.ProcessInstanceEntity;
import com.alinesno.infra.data.scheduler.service.IProcessInstanceService;
import io.swagger.annotations.Api;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 处理与TransEntity相关的请求的Controller。
 * 继承自BaseController类并实现ITransService接口。
 *
 * @version 1.0.0
 * @author  luoxiaodong
 */
@Slf4j
@Api(tags = "Trans")
@RestController
@Scope(SpringInstanceScope.PROTOTYPE)
@RequestMapping("/api/infra/data/scheduler/processInstance")
public class ProcessInstanceController extends BaseController<ProcessInstanceEntity, IProcessInstanceService> {

    @Autowired
    private IProcessInstanceService service;

    /**
     * 获取TransEntity的DataTables数据。
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
        List<ConditionDto> condition = page.getConditionList() ;

        ConditionDto conditionDto = new ConditionDto()  ;
        conditionDto.setType("orderBy");
        conditionDto.setValue("desc") ;
        conditionDto.setColumn("runTimes");

        condition.add(conditionDto) ;

        page.setConditionList(condition);

        CurrentProjectSession.filterProject(page);

        return this.toPage(model, this.getFeign(), page);
    }

    /**
     * 读取日志文件
     * @return
     */
    @GetMapping("/readLog")
    public AjaxResult readLog(long processInstanceId , String start) {
        return AjaxResult.success("读取日志成功", service.readLog(processInstanceId , start));
    }

    @Override
    public IProcessInstanceService getFeign() {
        return this.service;
    }
}
