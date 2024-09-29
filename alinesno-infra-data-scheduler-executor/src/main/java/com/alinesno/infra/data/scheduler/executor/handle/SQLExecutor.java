package com.alinesno.infra.data.scheduler.executor.handle;

import cn.hutool.db.sql.SqlExecutor;
import cn.hutool.db.sql.SqlFormatter;
import com.alinesno.infra.data.scheduler.api.ParamsDto;
import com.alinesno.infra.data.scheduler.executor.BaseExecutorService;
import com.alinesno.infra.data.scheduler.executor.bean.TaskInfoBean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;

@Slf4j
@Service("sqlExecutor")
public class SQLExecutor extends BaseExecutorService {

    @Override
    public void execute(TaskInfoBean task) {

        ParamsDto paramsDto = getParamsDto(task) ;
        String rawScript = paramsDto.getRawScript();
        String rawScriptFormat = SqlFormatter.format(rawScript) ;

        log.debug("SQL Executor rawScript: {}", rawScriptFormat) ;

        writeLog(task , rawScriptFormat);

        // 创建一个数据源
        DataSource dataSource = getDataSource(paramsDto.getDataSourceId()) ;

        try (Connection connection = dataSource.getConnection()) {
            ResultSet resultSet = SqlExecutor.callQuery(connection, rawScript) ;

            writeLog(task , "SQL 脚本执行结果: ");
            printResultSet(task ,resultSet) ;

            writeLog(task , "SQL 脚本执行成功.");
            DataSourceUtils.releaseConnection(connection, dataSource);
        } catch (SQLException e) {
            writeLog(task , "SQL 脚本执行失败: " + e.getMessage());
        }
    }

    /**
     * 打印结果集
     * @param task
     * @param resultSet
     * @throws SQLException
     */
    public void printResultSet(TaskInfoBean task, ResultSet resultSet) throws SQLException {
        // 获取结果集的元数据
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();

        // 打印表头
        StringBuilder tableHeader = new StringBuilder();
        for (int i = 1; i <= columnCount; i++) {
            if (i > 1){
                tableHeader.append(", ");
            }
            tableHeader.append(metaData.getColumnLabel(i));
        }
        log.debug(tableHeader.toString());
        writeLog(task , tableHeader.toString());

        // 遍历结果集并打印每一行
        while (resultSet.next()) {
            StringBuilder row = new StringBuilder();
            for (int i = 1; i <= columnCount; i++) {
                if (i > 1) {
                    row.append(", ");
                }
                // 获取当前列的值
                Object value = resultSet.getObject(i);
                row.append(value != null ? value : "NULL");
            }

            // 打印当前列的值
            log.debug(row.toString());
            writeLog(task , row.toString());
        }
    }
}
