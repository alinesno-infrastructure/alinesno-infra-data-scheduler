package com.alinesno.infra.data.scheduler.entity;

import com.gitee.sunchenbin.mybatis.actable.annotation.ColumnComment;
import com.gitee.sunchenbin.mybatis.actable.annotation.ColumnType;
import com.gitee.sunchenbin.mybatis.actable.annotation.Table;
import lombok.Data;
import com.alinesno.infra.common.facade.mapper.entity.InfraBaseEntity;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.EqualsAndHashCode;

/**
 * <p>
 * 存储应用实体类
 * </p>
 * <p>
 * 该类继承自InfraBaseEntity，表示存储应用的基本信息。
 * </p>
 *
 * @author luoxiaodong
 * @version 1.0.0
 */
@EqualsAndHashCode(callSuper = true)
@TableName("project")
@Data
@Table(comment = "项目应用")
public class ProjectEntity extends InfraBaseEntity {

    /**
     * 项目名称
     */
    @TableField("project_name")
    @ColumnType(length=32)
    @ColumnComment("项目名称")
    private String projectName;

    /**
     * 项目描述
     */
    @TableField("project_desc")
    @ColumnType(length=256)
    @ColumnComment("项目描述")
    private String projectDesc;

    /**
     * 项目代码
     */
    @TableField("project_code")
    @ColumnType(length=50)
    @ColumnComment("项目代码")
    private String projectCode;

    /**
     * 所开通渠道
     */
    @TableField("document_type")
    @ColumnType(length=255)
    @ColumnComment("所开通渠道")
    private String documentType ;

}
