package com.alinesno.infra.data.scheduler.service.impl;

import com.alibaba.druid.pool.DruidDataSource;
import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.facade.enums.HasStatusEnums;
import com.alinesno.infra.data.scheduler.api.CheckDbConnectResult;
import com.alinesno.infra.data.scheduler.entity.DataSourceEntity;
import com.alinesno.infra.data.scheduler.enums.DbTypeEnums;
import com.alinesno.infra.data.scheduler.mapper.DataSourceMapper;
import com.alinesno.infra.data.scheduler.service.IDataSourceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;

@Slf4j
@Service
public class DataSourceServiceImpl extends IBaseServiceImpl<DataSourceEntity, DataSourceMapper> implements IDataSourceService {

    @Override
    public CheckDbConnectResult checkDbConnect(DataSourceEntity entity) {
        try (DruidDataSource dataSource = createDataSource(entity)) {
            try (Connection connection = dataSource.getConnection()) {
                // 执行一个简单的查询来验证连接
                connection.createStatement().execute("SELECT 1");
                return new CheckDbConnectResult("连接成功", "200", true);
            } catch (SQLException e) {
                log.error("数据库连接失败: {}", e.getMessage(), e);
                return new CheckDbConnectResult("连接失败: " + e.getMessage(), "500", false);
            }
        } catch (Exception e) {
            log.error("创建数据源失败: {}", e.getMessage(), e);
            return new CheckDbConnectResult("创建数据源失败: " + e.getMessage(), "500", false);
        }
    }

    @Override
    public DruidDataSource getDataSource(long dataSourceId) {
        // 根据ID获取数据源实体
        DataSourceEntity entity = baseMapper.selectById(dataSourceId);
        if (entity == null) {
            log.error("Data source with ID {} not found.", dataSourceId);
            throw new RuntimeException("Data source not found.");
        }

        return createDataSource(entity);
    }

    @Override
    public List<DataSourceEntity> listAvailableDataSources(PermissionQuery query) {
        LambdaQueryWrapper<DataSourceEntity> wrapper = new LambdaQueryWrapper<>() ;
        wrapper.setEntityClass(DataSourceEntity.class) ;
        query.toWrapper(wrapper);
        wrapper.eq(DataSourceEntity::getHasStatus, HasStatusEnums.LEGAL.value) ;
        return list(wrapper) ;
    }

    @SneakyThrows
    private DruidDataSource createDataSource(DataSourceEntity entity) {

        // 创建Druid数据源配置
        DruidDataSource dataSource = new DruidDataSource();
        dataSource.setUrl(entity.getReaderUrl());
        dataSource.setUsername(entity.getReaderUsername());
        dataSource.setPassword(entity.getReaderPasswd()); // 注意：这里的密码应该是解密后的

        // 设置通用的配置项
        dataSource.setInitialSize(5);
        dataSource.setMinIdle(5);
        dataSource.setMaxActive(20);
        dataSource.setMaxWait(45*1000);
        dataSource.setTimeBetweenEvictionRunsMillis(60000);
        dataSource.setMinEvictableIdleTimeMillis(300000);
        dataSource.setValidationQuery("SELECT 1");
        dataSource.setTestWhileIdle(true);
        dataSource.setTestOnBorrow(false);
        dataSource.setTestOnReturn(false);
        dataSource.setPoolPreparedStatements(true);
        dataSource.setMaxPoolPreparedStatementPerConnectionSize(20);

        dataSource.setFilters("stat");

        // 根据不同的数据库类型设置其他属性
        DbTypeEnums dbType = DbTypeEnums.of(entity.getReaderType());
        if (dbType == null) {
            log.warn("Unsupported database type: {}", entity.getReaderType());
            throw new IllegalArgumentException("Unsupported database type: " + entity.getReaderType());
        }

        switch (dbType) {
            case MYSQL:
                // MySQL特定配置
                break;
            case POSTGRESQL:
                // PostgreSQL特定配置
                dataSource.setDriverClassName("org.postgresql.Driver");
                dataSource.setValidationQuery("SELECT 1");
                break;
            case CLICKHOUSE:
                // ClickHouse特定配置
                dataSource.setDriverClassName("ru.yandex.clickhouse.ClickHouseDriver");
                dataSource.setValidationQuery("SELECT 1");
                break;
            case ORACLE:
                // Oracle特定配置
                dataSource.setDriverClassName("oracle.jdbc.driver.OracleDriver");
                dataSource.setValidationQuery("SELECT 1 FROM DUAL");
                break;
            default:
                log.warn("Unsupported database type: {}", entity.getReaderType());
                throw new IllegalArgumentException("Unsupported database type: " + entity.getReaderType());
        }

        return dataSource;
    }
}