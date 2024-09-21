package com.alinesno.infra.data.scheduler.entity;

import com.alinesno.infra.common.facade.mapper.entity.InfraBaseEntity;
import com.alinesno.infra.data.scheduler.enums.DbTypeEnums;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.gitee.sunchenbin.mybatis.actable.annotation.ColumnComment;
import com.gitee.sunchenbin.mybatis.actable.annotation.ColumnType;
import com.gitee.sunchenbin.mybatis.actable.constants.MySqlTypeConstant;
import lombok.Data;
import lombok.EqualsAndHashCode;

@EqualsAndHashCode(callSuper = true)
@Data
@TableName("datasource")
public class DataSourceEntity extends InfraBaseEntity {

  @TableField(exist = false)
  private String userName;

  @TableField
  @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 128)
  @ColumnComment("数据库名称")
  private String name;

  @TableField
  @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 128)
  @ColumnComment("数据库描述")
  private String note;

  @TableField
  @ColumnType(value = MySqlTypeConstant.VARCHAR, length = 128)
  @ColumnComment("数据库类型")
  private DbTypeEnums type;

  @TableField
  @ColumnType(value = MySqlTypeConstant.LONGTEXT)
  @ColumnComment("数据库连接参数")
  private String connectionParams;

}