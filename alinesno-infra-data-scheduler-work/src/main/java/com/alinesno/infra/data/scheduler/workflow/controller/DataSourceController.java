package com.alinesno.infra.data.scheduler.workflow.controller;

import com.alinesno.infra.common.core.constants.SpringInstanceScope;
import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.facade.enums.HasStatusEnums;
import com.alinesno.infra.common.facade.pageable.DatatablesPageBean;
import com.alinesno.infra.common.facade.pageable.TableDataInfo;
import com.alinesno.infra.common.facade.response.R;
import com.alinesno.infra.common.web.adapter.rest.BaseController;
import com.alinesno.infra.data.scheduler.api.CheckDbConnectResult;
import com.alinesno.infra.data.scheduler.api.DataSourceDto;
import com.alinesno.infra.data.scheduler.entity.DataSourceEntity;
import com.alinesno.infra.data.scheduler.enums.SinkReaderEnums;
import com.alinesno.infra.data.scheduler.workflow.service.IDataSourceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.ui.Model;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.lang.exception.RpcServiceRuntimeException;
import java.util.ArrayList;
import java.util.HashMap;
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
public class DataSourceController extends BaseController<DataSourceEntity, IDataSourceService> {

    @Autowired
    private IDataSourceService service;

    /**
     * 获取BusinessLogEntity的DataTables数据。
     *
     * @param request HttpServletRequest对象。
     * @param model Model对象。
     * @param page DatatablesPageBean对象。
     * @return 包含DataTables数据的TableDataInfo对象。
     */
    @PostMapping("/datatables")
    public R<TableDataInfo> datatables(HttpServletRequest request, Model model, @RequestBody DatatablesPageBean page) {
        log.debug("page = {}", ToStringBuilder.reflectionToString(page));
        return R.ok(this.toPage(model, this.getFeign(), page));
    }

    @PostMapping("/checkConnectionByObj")
    public R<CheckDbConnectResult> checkDBConnect(@Validated @RequestBody DataSourceDto dto ) {

        DataSourceEntity dbEntity = new DataSourceEntity() ;
        BeanUtils.copyProperties(dto, dbEntity) ;

        CheckDbConnectResult result = service.checkDbConnect(dbEntity);
        if (result.isAccepted()) {
            return R.ok(result);
        } else {
            return R.fail(result);
        }
    }

    /**
     *  通过id验证是否连接正常 checkConnection
     * @param sourceId
     * @return
     */
    @GetMapping("/checkConnection")
    public R<CheckDbConnectResult> checkDBConnect(String sourceId) {
        DataSourceEntity dbEntity = service.getById(sourceId) ;
        CheckDbConnectResult result = service.checkDbConnect(dbEntity);
        if (result.isAccepted()) {
            return R.ok(result);
        } else {
            // 更新数据源状态
            dbEntity.setHasStatus(HasStatusEnums.ILLEGAL.value);
            service.update(dbEntity);
            return R.fail(result);
        }
    }

    @PutMapping("/modifyDb")
    public R<Void> modifyDb(@Validated @RequestBody DataSourceDto dto) {

        DataSourceEntity dbEntity = new DataSourceEntity() ;
        BeanUtils.copyProperties(dto, dbEntity) ;

        dbEntity.setReaderName(dto.getReaderType().toUpperCase());

        try {
            super.update(null, dbEntity) ;
            return R.ok() ;
        } catch (Exception e) {
            throw new RpcServiceRuntimeException(e.getMessage()) ;
        }
    }

    /**
     * 获取到所有数据库源
     * @return
     */
    @GetMapping("/listAllDataSource")
    public R<List<Map<String , Object>>> listAllDataSource(@RequestBody PermissionQuery query){

        LambdaQueryWrapper<DataSourceEntity> wrapper = new LambdaQueryWrapper<>() ;
        wrapper.setEntityClass(DataSourceEntity.class) ;
        query.toWrapper(wrapper);

        List<DataSourceEntity> list = service.list(wrapper) ;

        List<Map<String, Object>> result = list.stream().map(item -> {

            Map<String , Object> map = new HashMap<>();
            map.put("value", item.getId()) ;
            map.put("label" , item.getReaderName() + ":" + item.getReaderDesc()) ;

            return map ;
        }).toList();

        return R.ok(result) ;
    }

    @PostMapping("/saveDb")
    public R<Void> saveDb(@Validated @RequestBody DataSourceDto dto ) {

        DataSourceEntity dbEntity = new DataSourceEntity() ;

        BeanUtils.copyProperties(dto, dbEntity) ;
        dbEntity.setReaderName(dto.getReaderType().toUpperCase());
        dbEntity.setOperationType("source");

        try {
            super.save(null, dbEntity) ;
            return R.ok() ;
        } catch (Exception e) {
            throw new RpcServiceRuntimeException(e.getMessage()) ;
        }
    }

    /**
     * 获取到所有数据库源
     * @return
     */
    @PostMapping("/allDataSource")
    public R<List<Map<String, Object>>> allDataSource(@RequestBody PermissionQuery query){

        SinkReaderEnums[] sinkArr = SinkReaderEnums.values() ;

        List<Map<String, Object>> map = new ArrayList<>() ;
        for(SinkReaderEnums sink : sinkArr){
            Map<String, Object> mapItem = new HashMap<>() ;

            mapItem.put("code", sink.getCode()) ;
            mapItem.put("icon", sink.getIcon()) ;
            mapItem.put("readerDesc",sink.getDesc()) ;
            mapItem.put("sourceType", "sink") ;

            map.add(mapItem) ;
        }

        return R.ok(map) ;
    }

    /**
     * 列出状态正常的数据源 listAvailableDataSources
     * @return
     */
    @PostMapping("/listAvailableDataSources")
    public R<List<DataSourceEntity>> listAvailableDataSources(@RequestBody PermissionQuery query){
        List<DataSourceEntity> list = service.listAvailableDataSources(query) ;
        return R.ok(list);
    }

    @Override
    public IDataSourceService getFeign() {
        return this.service;
    }
}