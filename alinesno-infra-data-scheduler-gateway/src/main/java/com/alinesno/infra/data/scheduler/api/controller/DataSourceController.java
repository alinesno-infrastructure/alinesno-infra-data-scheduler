package com.alinesno.infra.data.scheduler.api.controller;

import com.alinesno.infra.common.core.constants.SpringInstanceScope;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionQuery;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionSave;
import com.alinesno.infra.common.extend.datasource.annotation.DataPermissionScope;
import com.alinesno.infra.common.facade.datascope.PermissionQuery;
import com.alinesno.infra.common.facade.pageable.DatatablesPageBean;
import com.alinesno.infra.common.facade.pageable.TableDataInfo;
import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.common.web.adapter.rest.BaseController;
import com.alinesno.infra.data.scheduler.api.CheckDbConnectResult;
import com.alinesno.infra.data.scheduler.api.DataSourceDto;
import com.alinesno.infra.data.scheduler.entity.DataSourceEntity;
import com.alinesno.infra.data.scheduler.enums.SinkReaderEnums;
import com.alinesno.infra.data.scheduler.service.IDataSourceService;
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
    @DataPermissionScope
    @PostMapping("/datatables")
    public TableDataInfo datatables(HttpServletRequest request, Model model, DatatablesPageBean page) {
        log.debug("page = {}", ToStringBuilder.reflectionToString(page));

//        CurrentProjectSession.filterProject(page);

        return this.toPage(model, this.getFeign(), page);
    }

    @PostMapping("/checkConnectionByObj")
    public AjaxResult checkDBConnect(@Validated @RequestBody DataSourceDto dto ) {

        DataSourceEntity dbListEntity = new DataSourceEntity() ;
        BeanUtils.copyProperties(dto, dbListEntity) ;

//        DbParserUtils.parserJdbcUrl(dbListEntity , dto.getJdbcUrl()) ;

        CheckDbConnectResult result = service.checkDbConnect(dbListEntity);
        if (result.isAccepted()) {
            return AjaxResult.success("操作成功", result);
        } else {
            return AjaxResult.error("数据库检验失败", result);
        }
    }

    @DataPermissionSave
    @PutMapping("/modifyDb")
    public AjaxResult modifyDb(@Validated @RequestBody DataSourceDto dto) {

        DataSourceEntity dbEntity = new DataSourceEntity() ;
        BeanUtils.copyProperties(dto, dbEntity) ;

        dbEntity.setReaderName(dto.getReaderType().toUpperCase());
//        dbEntity.setProjectId(CurrentProjectSession.get().getId());

        try {
            return super.update(null, dbEntity) ;
        } catch (Exception e) {
            throw new RpcServiceRuntimeException(e.getMessage()) ;
        }
    }

    /**
     * 获取到所有数据库源
     * @return
     */
    @DataPermissionQuery
    @GetMapping("/listAllDataSource")
    public AjaxResult listAllDataSource(PermissionQuery query){

        LambdaQueryWrapper<DataSourceEntity> wrapper = new LambdaQueryWrapper<>() ;
        wrapper.setEntityClass(DataSourceEntity.class) ;
        query.toWrapper(wrapper);
//        wrapper.eq(DataSourceEntity::getProjectId , CurrentProjectSession.get().getId()) ;

        List<DataSourceEntity> list = service.list(wrapper) ;

        List<Map<String, Object>> result = list.stream().map(item -> {

            Map<String , Object> map = new java.util.HashMap<>();
            map.put("value", item.getId()) ;
            map.put("label" , item.getReaderName() + ":" + item.getReaderDesc()) ;

            return map ;
        }).toList();

        return AjaxResult.success("success" , result) ;
    }

    @DataPermissionSave
    @PostMapping("/saveDb")
    public AjaxResult saveDb(@Validated @RequestBody DataSourceDto dto ) {

        DataSourceEntity dbEntity = new DataSourceEntity() ;

        BeanUtils.copyProperties(dto, dbEntity) ;
        dbEntity.setReaderName(dto.getReaderType().toUpperCase());
        dbEntity.setOperationType("source");

//        dbEntity.setProjectId(CurrentProjectSession.get().getId());

        try {
            return super.save(null, dbEntity) ;
        } catch (Exception e) {
            throw new RpcServiceRuntimeException(e.getMessage()) ;
        }
    }

    /**
     * 获取到所有数据库源
     * @return
     */
    @GetMapping("/allDataSource")
    public AjaxResult allDataSource(PermissionQuery query){

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

        return AjaxResult.success(map) ;
    }


    @Override
    public IDataSourceService getFeign() {
        return this.service;
    }
}