package com.alinesno.infra.data.scheduler.service.impl;

import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.data.scheduler.api.CheckDbConnectResult;
import com.alinesno.infra.data.scheduler.entity.DataSourceEntity;
import com.alinesno.infra.data.scheduler.mapper.DataSourceMapper;
import com.alinesno.infra.data.scheduler.service.IDataSourceService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringEscapeUtils;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;

@Slf4j
@Service
public class DataSourceServiceImpl extends IBaseServiceImpl<DataSourceEntity, DataSourceMapper> implements IDataSourceService {

    @Override
    public CheckDbConnectResult checkDbConnect(DataSourceEntity entity) {

//        if (entity.getReaderType().equals(DbType.MY_SQL)) {
//            return checkMysqlConnect(entity);
//        }

        return new CheckDbConnectResult("不支持的数据库类型", "401", false);
    }

    private CheckDbConnectResult checkMysqlConnect(DataSourceEntity entity) {
        String url = entity.getReaderUrl();

        try {
            Connection connection = DriverManager.getConnection(
                    StringEscapeUtils.escapeJava(StringEscapeUtils.unescapeHtml(url)),
                    StringEscapeUtils.escapeJava(StringEscapeUtils.unescapeHtml(entity.getReaderUsername())),
                    StringEscapeUtils.escapeJava(StringEscapeUtils.unescapeHtml(entity.getReaderPasswd())));
            connection.close();
        } catch (Exception e) {
            log.error(e.getMessage());
            return CheckDbConnectResult.reject("590");
        }
        return CheckDbConnectResult.accept("200");
    }


}
