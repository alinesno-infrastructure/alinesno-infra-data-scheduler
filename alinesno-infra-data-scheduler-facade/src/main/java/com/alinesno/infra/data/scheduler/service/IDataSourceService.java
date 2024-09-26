package com.alinesno.infra.data.scheduler.service;

import com.alinesno.infra.common.facade.services.IBaseService;
import com.alinesno.infra.data.scheduler.api.CheckDbConnectResult;
import com.alinesno.infra.data.scheduler.entity.DataSourceEntity;

import javax.sql.DataSource;

public interface IDataSourceService extends IBaseService<DataSourceEntity> {

    /**
     * 数据库连接校验
     * @param dbListEntity
     * @return
     */
    CheckDbConnectResult checkDbConnect(DataSourceEntity dbListEntity);

    /**
     * 通过数据库ID获取数据源
     * @param dataSourceId
     * @return
     */
    DataSource getDataSource(long dataSourceId);

}