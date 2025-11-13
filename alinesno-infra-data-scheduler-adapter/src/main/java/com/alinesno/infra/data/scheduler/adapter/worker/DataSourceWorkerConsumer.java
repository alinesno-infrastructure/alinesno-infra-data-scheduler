package com.alinesno.infra.data.scheduler.adapter.worker;

import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.facade.enums.HasStatusEnums;
import com.alinesno.infra.common.facade.pageable.DatatablesPageBean;
import com.alinesno.infra.common.facade.pageable.TableDataInfo;
import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.common.facade.response.R;
import com.alinesno.infra.data.scheduler.api.CheckDbConnectResult;
import com.alinesno.infra.data.scheduler.api.DataSourceDto;
import com.alinesno.infra.data.scheduler.entity.DataSourceEntity;
import com.alinesno.infra.data.scheduler.enums.SinkReaderEnums;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.dtflys.forest.annotation.*;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.beans.BeanUtils;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import javax.lang.exception.RpcServiceRuntimeException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 数据源消费者
 */
@BaseRequest(baseURL = "#{alinesno.data.scheduler.worker-node}/api/infra/data/scheduler/dataSource" , readTimeout= 3600_000)
public interface DataSourceWorkerConsumer {

    /**
     * 获取BusinessLogEntity的DataTables数据。
     *
     * @param page DatatablesPageBean对象。
     * @return 包含DataTables数据的TableDataInfo对象。
     */
    @Post("/datatables")
    R<TableDataInfo> datatables(@JSONBody DatatablesPageBean page)  ;

    /**
     * 通过对象验证是否连接正常 checkConnectionByObj
     * @param dto
     * @return
     */
    @Post("/checkConnectionByObj")
    R<CheckDbConnectResult> checkDBConnect(@JSONBody DataSourceDto dto ) ;

    /**
     *  通过id验证是否连接正常 checkConnection
     * @param sourceId
     * @return
     */
    @Get("/checkConnection")
    R<CheckDbConnectResult> checkDBConnect(@Query("sourceId") String sourceId)  ;

    @Put("/modifyDb")
    R<Void> modifyDb(@JSONBody DataSourceDto dto) ;

    /**
     * 获取到所有数据库源
     * @return
     */
    @Get("/listAllDataSource")
    R<List<Map<String , Object>>> listAllDataSource(@JSONBody PermissionQuery query) ;

    /**
     * 保存数据库源
     * @param dto
     * @return
     */
    @Post("/saveDb")
    R<Void> saveDb(@JSONBody DataSourceDto dto ) ;

    /**
     * 获取到所有数据库源
     * @return
     */
    @Post("/allDataSource")
    R<List<Map<String, Object>>> allDataSource(@JSONBody PermissionQuery query) ;

    /**
     * 列出状态正常的数据源 listAvailableDataSources
     * @return
     */
    @Post("/listAvailableDataSources")
    R<List<DataSourceEntity>> listAvailableDataSources(@JSONBody PermissionQuery query) ;

    /**
     * 详情况查询
     * @param id
     * @return
     */
    @Get("/detail/{id}")
    R<DataSourceEntity> detail(@Var("id") String id);
}
