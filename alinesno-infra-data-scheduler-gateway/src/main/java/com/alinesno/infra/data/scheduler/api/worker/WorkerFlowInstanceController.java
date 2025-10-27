package com.alinesno.infra.data.scheduler.api.worker;

import cn.dev33.satoken.context.SaTokenContext;
import cn.dev33.satoken.stp.StpUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.common.core.constants.SpringInstanceScope;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionScope;
import com.alinesno.infra.common.facade.pageable.ConditionDto;
import com.alinesno.infra.common.facade.pageable.DatatablesPageBean;
import com.alinesno.infra.common.facade.pageable.TableDataInfo;
import com.alinesno.infra.common.facade.response.R;
import com.alinesno.infra.data.scheduler.adapter.worker.WorkerFlowExecutionConsumer;
import com.alinesno.infra.data.scheduler.api.utils.LocalConditionUtils;
import com.alinesno.infra.data.scheduler.api.utils.TokenUtils;
import com.alinesno.infra.data.scheduler.entity.ProcessDefinitionEntity;
import com.alinesno.infra.data.scheduler.entity.worker.FlowExecutionEntity;
import com.alinesno.infra.data.scheduler.enums.worker.FlowExecutionStatus;
import com.alinesno.infra.data.scheduler.service.IProcessDefinitionService;
import io.swagger.annotations.Api;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
public class WorkerFlowInstanceController {

    @Autowired
    private WorkerFlowExecutionConsumer service;

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

        List<ConditionDto> condition = Optional.ofNullable(page.getConditionList()).orElseGet(ArrayList::new);
        condition.add(LocalConditionUtils.newCondOrderDesc("runTimes"));

        Optional.ofNullable(request.getParameter("processDefinitionId"))
                .filter(StringUtils::isNotBlank)
                .ifPresent(v -> condition.add(LocalConditionUtils.newCondEq("processDefinitionId", v)));

        page.setConditionList(condition);

        TableDataInfo tableDataInfo =  service.datatables(page) ;

        if(tableDataInfo.getRows() != null){

            List<FlowExecutionDto> dtoRows = new ArrayList<>() ;

            for(Object obj : tableDataInfo.getRows()){
                JSONObject jsonObject = JSON.parseObject(JSON.toJSONString(obj));
                FlowExecutionEntity entity = jsonObject.toJavaObject(FlowExecutionEntity.class);

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


        return tableDataInfo ;
    }

}
