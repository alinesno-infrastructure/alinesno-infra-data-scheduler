package com.alinesno.infra.data.scheduler.executor.handle;

import cn.hutool.db.sql.SqlFormatter;
import com.alibaba.druid.pool.DruidDataSource;
import com.alinesno.infra.data.scheduler.api.ParamsDto;
import com.alinesno.infra.data.scheduler.executor.BaseExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@Service("sqlExecutor")
public class SQLExecutor extends BaseExecutorService {

    @Override
    public void execute(TaskInfoBean task) {

        ParamsDto paramsDto = getParamsDto(task) ;
        String rawScript = paramsDto.getRawScript();
        String rawScriptFormat = SqlFormatter.format(rawScript) ;

        writeLog(task , rawScriptFormat);

        // 创建一个数据源
        DruidDataSource dataSource = getDataSource(paramsDto.getDataSourceId()) ;

        try (Connection connection = dataSource.getConnection()) {
            // 执行SQL脚本
            ScriptUtils.executeSqlScript(connection, new ByteArrayResource(rawScript.getBytes()));
            writeLog(task , "SQL 脚本执行成功.");
            DataSourceUtils.releaseConnection(connection, dataSource);
        } catch (SQLException e) {
            writeLog(task , "SQL 脚本执行失败: " + e.getMessage());
        }
    }

}
