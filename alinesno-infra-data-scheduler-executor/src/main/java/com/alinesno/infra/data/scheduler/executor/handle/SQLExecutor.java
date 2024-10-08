package com.alinesno.infra.data.scheduler.executor.handle;

import cn.hutool.db.sql.SqlFormatter;
import com.alibaba.druid.pool.DruidDataSource;
import com.alinesno.infra.data.scheduler.api.ParamsDto;
import com.alinesno.infra.data.scheduler.executor.AbstractExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;

import javax.lang.exception.RpcServiceRuntimeException;
import java.sql.Connection;
import java.sql.SQLException;

@Slf4j
@Service("sqlExecutor")
public class SQLExecutor extends AbstractExecutorService {

    @Override
    public void execute(TaskInfoBean task) {

        ParamsDto paramsDto = getParamsDto() ;
        String rawScript = paramsDto.getRawScript();

        writeLog(SqlFormatter.format(rawScript));

        // 创建一个数据源
        DruidDataSource dataSource = getDataSource();
        Connection connection = null;
        try {
            connection = dataSource.getConnection();
            // 执行SQL脚本
            ScriptUtils.executeSqlScript(connection, new ByteArrayResource(rawScript.getBytes()));
            writeLog("SQL 脚本执行成功.");

        } catch (SQLException e) {
            throw new RpcServiceRuntimeException("获取数据源异常:" + e.getMessage());
        }finally {
            DataSourceUtils.releaseConnection(connection, dataSource);
            dataSource.close();
        }

    }

}
