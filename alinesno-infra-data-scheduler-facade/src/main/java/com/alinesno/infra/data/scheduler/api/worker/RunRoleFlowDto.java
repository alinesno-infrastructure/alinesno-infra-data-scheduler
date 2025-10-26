package com.alinesno.infra.data.scheduler.api.worker;

import com.alinesno.infra.data.scheduler.entity.ProcessDefinitionEntity;
import com.alinesno.infra.data.scheduler.enums.ExecutionStrategyEnums;
import lombok.Data;

import java.util.Map;

/**
 *  运行角色流程数据传输对象
 */
@Data
public class RunRoleFlowDto {
    private Long processDefinitionId  ;
    private ProcessDefinitionEntity processDefinitionEntity ;
    private ExecutionStrategyEnums errorStrategy ;
    private Map<String , String> orgSecrets ;

    /**
     * 创建运行角色流程数据传输对象
     * @param processDefinitionId
     * @param process
     * @param errorStrategy
     * @param orgSecrets
     * @return
     */
    public static RunRoleFlowDto form(Long processDefinitionId,
                                      ProcessDefinitionEntity process,
                                      ExecutionStrategyEnums errorStrategy,
                                      Map<String, String> orgSecrets) {
        RunRoleFlowDto dto = new RunRoleFlowDto();

        dto.setProcessDefinitionId(processDefinitionId);
        dto.setProcessDefinitionEntity(process);
        dto.setErrorStrategy(errorStrategy);
        dto.setOrgSecrets(orgSecrets);

        return dto ;
    }
}
