package com.alinesno.infra.data.scheduler.executor.handle;

import com.alinesno.infra.data.scheduler.api.ParamsDto;
import com.alinesno.infra.data.scheduler.executor.BaseExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@Service("sqlExecutor")
public class SQLExecutor extends BaseExecutorService {

    @Override
    public void execute(TaskInfoBean task) {

        ParamsDto paramsDto = getParamsDto(task) ;
        String rawScript = paramsDto.getRawScript();

        log.debug("SQL Executor rawScript: {}", rawScript) ;

        // 创建一个数据源
        DataSource dataSource = getDataSource(paramsDto.getDataSourceId()) ;

        try (Connection connection = dataSource.getConnection()) {
            // 执行SQL脚本
            ScriptUtils.executeSqlScript(connection, new ByteArrayResource(rawScript.getBytes()));
            log.debug("SQL script executed successfully.");
        } catch (SQLException e) {
            log.error("Failed to execute SQL script: " + e.getMessage());
        }
    }

}
