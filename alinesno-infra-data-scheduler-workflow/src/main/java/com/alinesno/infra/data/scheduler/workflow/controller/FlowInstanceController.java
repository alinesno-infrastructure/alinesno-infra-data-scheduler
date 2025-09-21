package com.alinesno.infra.data.scheduler.workflow.controller;

import com.alinesno.infra.common.core.constants.SpringInstanceScope;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionScope;
import com.alinesno.infra.common.facade.pageable.ConditionDto;
import com.alinesno.infra.common.facade.pageable.DatatablesPageBean;
import com.alinesno.infra.common.facade.pageable.TableDataInfo;
import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.common.web.adapter.rest.BaseController;
import com.alinesno.infra.data.scheduler.entity.ProcessDefinitionEntity;
import com.alinesno.infra.data.scheduler.entity.ProcessInstanceEntity;
import com.alinesno.infra.data.scheduler.service.IProcessDefinitionService;
import com.alinesno.infra.data.scheduler.service.IProcessInstanceService;
import com.alinesno.infra.data.scheduler.workflow.dto.FlowExecutionDto;
import com.alinesno.infra.data.scheduler.workflow.entity.FlowExecutionEntity;
import com.alinesno.infra.data.scheduler.workflow.enums.FlowExecutionStatus;
import com.alinesno.infra.data.scheduler.workflow.service.IFlowExecutionService;
import io.swagger.annotations.Api;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
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
public class FlowInstanceController extends BaseController<FlowExecutionEntity, IFlowExecutionService> {

    @Autowired
    private IFlowExecutionService service;

    @Autowired
    private IProcessDefinitionService processDefinitionService ;

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

        TableDataInfo tableDataInfo = this.toPage(model, this.getFeign(), page);

        if(tableDataInfo.getRows() != null){

            List<FlowExecutionDto> dtoRows = new ArrayList<>() ;

            for(Object obj : tableDataInfo.getRows()){
                FlowExecutionEntity entity = (FlowExecutionEntity) obj ;

                FlowExecutionDto dto = new FlowExecutionDto() ;
                BeanUtils.copyProperties(entity,dto);

                Long processDefinitionId = entity.getProcessDefinitionId();
                ProcessDefinitionEntity processInstanceEntity = processDefinitionService.getById(processDefinitionId) ;

                if(processInstanceEntity != null){
                    dto.setName(processInstanceEntity.getName());
                }

                FlowExecutionStatus flowExecutionStatus = FlowExecutionStatus.getByCode(entity.getExecutionStatus()) ;
                dto.setExecutionStatusLabel(flowExecutionStatus.getLabel());

                dtoRows.add(dto) ;
            }

            tableDataInfo.setRows(dtoRows);
        }

        return tableDataInfo;
    }

    @Override
    public IFlowExecutionService getFeign() {
        return this.service;
    }
}
