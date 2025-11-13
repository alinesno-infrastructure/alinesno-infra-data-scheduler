package com.alinesno.infra.data.scheduler.api.worker;

import com.alinesno.infra.common.core.constants.SpringInstanceScope;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionQuery;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionSave;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionScope;
import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.facade.pageable.DatatablesPageBean;
import com.alinesno.infra.common.facade.pageable.TableDataInfo;
import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.common.facade.response.R;
import com.alinesno.infra.data.scheduler.adapter.worker.DataSourceWorkerConsumer;
import com.alinesno.infra.data.scheduler.api.CheckDbConnectResult;
import com.alinesno.infra.data.scheduler.api.DataSourceDto;
import com.alinesno.infra.data.scheduler.entity.DataSourceEntity;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 处理与BusinessLogEntity相关的请求的Controller。
 * 继承自BaseController类并实现IBusinessLogService接口。
 *
 * @author luoxiaodong
 * @version 1.0.0
 */
@Slf4j
@RestController
@Scope(SpringInstanceScope.PROTOTYPE)
@RequestMapping("/api/infra/data/scheduler/dataSource")
public class WorkerDataSourceController {

    @Autowired
    private DataSourceWorkerConsumer dataSourceWorkerConsumer;

    /**
     * 获取BusinessLogEntity的DataTables数据。
     *
     * @param request HttpServletRequest对象。
     * @param model Model对象。
     * @param page DatatablesPageBean对象。
     * @return 包含DataTables数据的TableDataInfo对象。
     */
    @DataPermissionScope
    @PostMapping("/datatables")
    public TableDataInfo datatables(HttpServletRequest request, Model model, DatatablesPageBean page) {
        log.debug("page = {}", ToStringBuilder.reflectionToString(page));

        R<TableDataInfo> result = dataSourceWorkerConsumer.datatables(page) ;
        return result.getData() ;
    }

    @PostMapping("/checkConnectionByObj")
    public AjaxResult checkDBConnect(@Validated @RequestBody DataSourceDto dto ) {

        R<CheckDbConnectResult> result = dataSourceWorkerConsumer.checkDBConnect(dto);
        return R.isSuccess(result) ? AjaxResult.success("操作成功", result.getData()) : AjaxResult.error("数据库检验失败", result.getData());
    }

    /**
     *  通过id验证是否连接正常 checkConnection
     * @param sourceId
     * @return
     */
    @GetMapping("/checkConnection")
    public AjaxResult checkDBConnect(String sourceId) {
        R<CheckDbConnectResult> result = dataSourceWorkerConsumer.checkDBConnect(sourceId);
        return R.isSuccess(result) ? AjaxResult.success("操作成功", result.getData()) : AjaxResult.error("数据库检验失败", result.getData());
    }

    @DataPermissionSave
    @PutMapping("/modifyDb")
    public AjaxResult modifyDb(@Validated @RequestBody DataSourceDto dto) {

        R<Void> result =  dataSourceWorkerConsumer.modifyDb(dto) ;
        return R.isSuccess(result) ? AjaxResult.success("操作成功") : AjaxResult.error("数据库检验失败") ;
    }

    /**
     * 获取到所有数据库源
     * @return
     */
    @DataPermissionQuery
    @GetMapping("/listAllDataSource")
    public AjaxResult listAllDataSource(PermissionQuery query){
        R<List<Map<String , Object>>> list = dataSourceWorkerConsumer.listAllDataSource(query) ;
        return R.isSuccess(list) ? AjaxResult.success("success" , list.getData()) : AjaxResult.error("数据库检验失败");
    }

    @DataPermissionSave
    @PostMapping("/saveDb")
    public AjaxResult saveDb(@Validated @RequestBody DataSourceDto dto ) {
        R<Void> result = dataSourceWorkerConsumer.saveDb(dto) ;
        return R.isSuccess(result) ? AjaxResult.success("操作成功") : AjaxResult.error("数据库检验失败");
    }

    /**
     * 获取到所有数据库源
     * @return
     */
    @DataPermissionQuery
    @GetMapping("/allDataSource")
    public AjaxResult allDataSource(PermissionQuery query){
        R<List<Map<String, Object>>> list = dataSourceWorkerConsumer.allDataSource(query) ;
        return R.isSuccess(list) ? AjaxResult.success("success" , list.getData()) : AjaxResult.error("数据库检验失败");
    }

    /**
     * 列出状态正常的数据源 listAvailableDataSources
     * @return
     */
    @DataPermissionQuery
    @GetMapping("/listAvailableDataSources")
    public AjaxResult listAvailableDataSources(PermissionQuery query){

        R<List<DataSourceEntity>> list = dataSourceWorkerConsumer.listAvailableDataSources(query) ;
        return R.isSuccess(list) ? AjaxResult.success("success" , list.getData()) : AjaxResult.error("数据库检验失败");
    }

    /**
     * 详情况查询
     */
    @GetMapping("/detail/{id}")
    public AjaxResult getDataSourceById(@PathVariable String id){
        R<DataSourceEntity> result = dataSourceWorkerConsumer.detail(id) ;
        return R.isSuccess(result) ? AjaxResult.success("操作成功" , result.getData()) : AjaxResult.error("数据库检验失败");
    }

}