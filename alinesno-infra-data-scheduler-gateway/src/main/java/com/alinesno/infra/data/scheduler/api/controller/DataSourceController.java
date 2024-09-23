package com.alinesno.infra.data.scheduler.api.controller;

import com.alinesno.infra.common.core.constants.SpringInstanceScope;
import com.alinesno.infra.common.facade.pageable.DatatablesPageBean;
import com.alinesno.infra.common.facade.pageable.TableDataInfo;
import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.common.web.adapter.rest.BaseController;
import com.alinesno.infra.data.scheduler.api.CheckDbConnectResult;
import com.alinesno.infra.data.scheduler.api.DataSourceDto;
import com.alinesno.infra.data.scheduler.entity.DataSourceEntity;
import com.alinesno.infra.data.scheduler.service.IDataSourceService;
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
    public TableDataInfo datatables(HttpServletRequest request, Model model, DatatablesPageBean page) {
        log.debug("page = {}", ToStringBuilder.reflectionToString(page));
        return this.toPage(model, this.getFeign(), page);
    }

    @PostMapping("/checkDB")
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

    @PutMapping("/modifyDb")
    public AjaxResult modifyDb(@Validated @RequestBody DataSourceDto dto ) {

        DataSourceEntity dbEntity = new DataSourceEntity() ;
        BeanUtils.copyProperties(dto, dbEntity) ;

        dbEntity.setReaderName(dto.getReaderType().toUpperCase());

//        DbParserUtils.parserJdbcUrl(dbEntity , dto.getJdbcUrl()) ;

        try {
            return super.update(null, dbEntity) ;
        } catch (Exception e) {
            throw new RpcServiceRuntimeException(e.getMessage()) ;
        }
    }

//    /**
//     * 获取到所有数据库源
//     * @return
//     */
//    @GetMapping("/allDataSource")
//    public AjaxResult allDataSource(){
//        return AjaxResult.success(service.list()) ;
//    }
//
//    /**
//     * 获取到所有数据库源
//     * @return
//     */
//    @GetMapping("/allDataSource")
//    public AjaxResult allDataSource(){
//
//        SinkReaderEnums[] sinkArr = SinkReaderEnums.values() ;
//        SourceReaderEnums[] sourceArr = SourceReaderEnums.values() ;
//
//        List<Map<String, Object>> map = new ArrayList<>() ;
//        for(SinkReaderEnums sink : sinkArr){
//            Map<String, Object> mapItem = new HashMap<>() ;
//
//            mapItem.put("code", sink.getCode()) ;
//            mapItem.put("icon", sink.getIcon()) ;
//            mapItem.put("readerDesc",sink.getDesc()) ;
//            mapItem.put("sourceType", "sink") ;
//
//            map.add(mapItem) ;
//        }
//
//        for(SourceReaderEnums source : sourceArr){
//            Map<String, Object> mapItem = new HashMap<>() ;
//
//            mapItem.put("code", source.getCode()) ;
//            mapItem.put("icon", source.getIcon()) ;
//            mapItem.put("readerDesc",source.getDesc()) ;
//            mapItem.put("sourceType", "source") ;
//
//            map.add(mapItem) ;
//        }
//
//        return AjaxResult.success(map) ;
//    }
//
//    @PostMapping("/saveDb")
//    public AjaxResult saveDb(@Validated @RequestBody DataSourceDto dto ) {
//
//        DataSourceEntity dbEntity = new DataSourceEntity() ;
//
//        BeanUtils.copyProperties(dto, dbEntity) ;
//        dbEntity.setReaderName(dto.getReaderType().toUpperCase());
//
////        DbParserUtils.parserJdbcUrl(dbListEntity , dto.get()) ;
//
//        try {
//            return super.save(null, dbEntity) ;
//        } catch (Exception e) {
//            throw new RpcServiceRuntimeException(e.getMessage()) ;
//        }
//    }
//
//    /**
//     * 获取到数据库源ID
//     * @param dto
//     * @return
//     */
//    @PostMapping("/fetchData")
//    public AjaxResult fetchData(@Validated @RequestBody FetchDataDto dto) {
//        log.debug("dto = {}", dto) ;
//
//        DataSourceEntity resourceSource = service.getById(dto.getSourceId()) ;
//        String readerType = resourceSource.getReaderType() ;
//        IDataSourceReader dataSourceReader = SpringUtils.getBean(readerType.toLowerCase() + PipeConstants.READER_SUFFIX) ;
//
//        SourceReader reader = DataSourceMapping.getDataSource(resourceSource , dto) ;
//        List<Map<String, Object>> rows = dataSourceReader.fetchData(reader) ;
//
//        AjaxResult ajax =  AjaxResult.success(rows);
//        ajax.put("fieldMeta" , dataSourceReader.analyseSourceField(reader));
//
//        return ajax ;
//    }
//
//    /**
//     * 获取到数据库源ID的表结构
//     * @param sourceId
//     * @return
//     */
//    @GetMapping("/fetchTableData")
//    public AjaxResult fetchTableData(String sourceId) {
//        log.debug("sourceId = {}", sourceId) ;
//
//        DataSourceEntity resourceSource = service.getById(sourceId) ;
//        String readerType = resourceSource.getReaderType() ;
//        IDataSourceReader dataSourceReader = SpringUtils.getBean(readerType.toLowerCase() + PipeConstants.READER_SUFFIX) ;
//
//        SourceReader reader = DataSourceMapping.getDataSource(resourceSource , null) ;
//        List<TableMetaBean> rows = dataSourceReader.fetchTableData(reader) ;
//
//        return AjaxResult.success(rows);
//    }
//
//    /**
//     * 获取到数据库源ID的表结构
//     * @param sourceId
//     * @return
//     */
//    @GetMapping("/fetchTableMetaData")
//    public AjaxResult fetchTableMetaData(String sourceId , String tableName) {
//        log.debug("sourceId = {}", sourceId) ;
//
//        DataSourceEntity resourceSource = service.getById(sourceId) ;
//        String readerType = resourceSource.getReaderType() ;
//        IDataSourceReader dataSourceReader = SpringUtils.getBean(readerType.toLowerCase() + PipeConstants.READER_SUFFIX) ;
//
//        SourceReader reader = DataSourceMapping.getDataSource(resourceSource , null) ;
//        reader.setQuerySql("select * from " + tableName);
//
//        List<FieldMetaBean> rows = dataSourceReader.analyseSourceField(reader) ;
//
//        return AjaxResult.success(rows);
//    }
//
//    /**
//     * 检查数据库连接
//     * @return
//     */
//    @GetMapping("/checkConnection")
//    public AjaxResult checkConnection(String sourceId){
//        DataSourceEntity resourceSource = service.getById(sourceId) ;
//        String readerType = resourceSource.getReaderType() ;
//        IDataSourceReader dataSourceReader = SpringUtils.getBean(readerType.toLowerCase() + PipeConstants.READER_SUFFIX) ;
//
//        SourceReader reader = DataSourceMapping.getDataSource(resourceSource , null) ;
//        dataSourceReader.checkConnection(reader) ;
//
//        return AjaxResult.success();
//    }
//
//    /**
//     * 检查数据库连接
//     * @return
//     */
//    @PostMapping("/checkConnectionByObj")
//    public AjaxResult checkConnectionByObj(@Validated @RequestBody DataSourceDto dto){
//
//        String readerType = dto.getReaderType() ;
//        IDataSourceReader dataSourceReader = SpringUtils.getBean(readerType.toLowerCase() + PipeConstants.READER_SUFFIX) ;
//
//        SourceReader reader = DataSourceMapping.getDataSourceByDto(dto) ;
//        dataSourceReader.checkConnection(reader) ;
//
//        return AjaxResult.success();
//    }


    @Override
    public IDataSourceService getFeign() {
        return this.service;
    }
}