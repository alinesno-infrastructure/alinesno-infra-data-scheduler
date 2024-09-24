package com.alinesno.infra.data.scheduler.entity;

import com.alinesno.infra.common.facade.mapper.entity.InfraBaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gitee.sunchenbin.mybatis.actable.annotation.ColumnComment;
import com.gitee.sunchenbin.mybatis.actable.annotation.ColumnType;
import com.gitee.sunchenbin.mybatis.actable.annotation.TableComment;
import com.gitee.sunchenbin.mybatis.actable.constants.MySqlTypeConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
@Data
@TableName("process_definition")
@TableComment(value = "流程定义表")
public class ProcessDefinitionEntity extends InfraBaseEntity {

    @TableField
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 128)
    @ColumnComment("流程名称")
    private String name ;

    @TableField
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 128)
    @ColumnComment("流程描述")
    private String description ;

    @TableField
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 128)
    @ColumnComment("项目编码")
    private String projectCode ;

    @TableField(exist = false)
    private String projectName;  // 项目名称

    @TableField
    @ColumnType(value = MySqlTypeConstant.LONGTEXT)
    @ColumnComment("全局参数")
    private String globalParams;

    @TableField(exist = false)
    private Map<String, String> globalParamMap;

    @TableField
    @ColumnType(value = MySqlTypeConstant.INT)
    @ColumnComment("超时时间")
    private int timeout;

    @TableField
    @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 128)
    @ColumnComment("调度策略")
    private String scheduleCron ;

}