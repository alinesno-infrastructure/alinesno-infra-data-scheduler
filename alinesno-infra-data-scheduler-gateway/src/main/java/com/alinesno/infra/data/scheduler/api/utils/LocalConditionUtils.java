package com.alinesno.infra.data.scheduler.api.utils;

import com.alinesno.infra.common.facade.pageable.ConditionDto;
import com.alinesno.infra.common.facade.wrapper.Wrapper;
import lombok.NoArgsConstructor;

/**
 * 本地条件工具类
 */
@NoArgsConstructor
public class LocalConditionUtils extends ConditionDto {

    /**
     * 创建条件
     * @param type
     * @param column
     * @param value
     * @return
     */
    public static ConditionDto newCond(String type, String column, String value) {
        ConditionDto d = new ConditionDto();
        d.setType(type);
        d.setColumn(column);
        d.setValue(value);
        return d;
    }

    /**
     * 创建条件
     * @param processDefinitionId
     * @param v
     * @return
     */
    public static ConditionDto newCondEq(String processDefinitionId, String v) {
        return newCond(Wrapper.EQ, processDefinitionId, v);
    }

    /**
     * 创建排序条件
     * @param runTimes
     * @return
     */
    public static ConditionDto newCondOrderDesc(String runTimes) {
        return newCond(Wrapper.ORDER_BY, runTimes, "desc");
    }
}
