package com.alinesno.infra.data.scheduler.trigger.entity;

import com.alinesno.infra.common.facade.mapper.entity.InfraBaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gitee.sunchenbin.mybatis.actable.annotation.Column;
import com.gitee.sunchenbin.mybatis.actable.annotation.Table;
import com.gitee.sunchenbin.mybatis.actable.annotation.Unique;
import com.gitee.sunchenbin.mybatis.actable.constants.MySqlTypeConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Job 实体：使用 Lombok、MyBatis-Plus 的 ActiveRecord，并用 mybatis-actable 注解以自动建表
 */
@EqualsAndHashCode(callSuper = true)
@Data
@TableName("job")
@Table(name = "job") // mybatis-actable 注解，自动建表时会读取
public class JobEntity extends InfraBaseEntity {

    @TableField
    @Column(name = "process_id", type = MySqlTypeConstant.BIGINT, isNull = false , comment = "进程ID")
    @Unique
    private Long processId ;

    @TableField
    @Column(name = "name", type = MySqlTypeConstant.VARCHAR, length = 255, isNull = false , comment = "名称")
    private String name;

    // 描述
    @TableField("remark")
    @Column(name = "remark", type = MySqlTypeConstant.VARCHAR, length = 255, isNull = true , comment = "描述")
    private String remark;

}