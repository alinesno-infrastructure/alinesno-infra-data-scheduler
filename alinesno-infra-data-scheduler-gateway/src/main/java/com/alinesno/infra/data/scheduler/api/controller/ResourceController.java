package com.alinesno.infra.data.scheduler.api.controller;

import cn.hutool.core.util.IdUtil;
import com.alinesno.infra.common.core.constants.SpringInstanceScope;
import com.alinesno.infra.common.facade.pageable.DatatablesPageBean;
import com.alinesno.infra.common.facade.pageable.TableDataInfo;
import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.common.facade.response.R;
import com.alinesno.infra.common.web.adapter.rest.BaseController;
import com.alinesno.infra.data.scheduler.adapter.CloudStorageConsumer;
import com.alinesno.infra.data.scheduler.entity.ResourceEntity;
import com.alinesno.infra.data.scheduler.enums.ResourceTypeEnums;
import com.alinesno.infra.data.scheduler.service.ICategoryService;
import com.alinesno.infra.data.scheduler.service.IResourceService;
import io.swagger.annotations.Api;
import jakarta.servlet.http.HttpServletRequest;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 处理与TransEntity相关的请求的Controller。
 * 继承自BaseController类并实现ITransService接口。
 *
 * @version 1.0.0
 * @author  luoxiaodong
 */
@Slf4j
@Api(tags = "Trans")
@RestController
@Scope(SpringInstanceScope.PROTOTYPE)
@RequestMapping("/api/infra/data/scheduler/resource")
public class ResourceController extends BaseController<ResourceEntity, IResourceService> {

    @Autowired
    private IResourceService service;

    @Autowired
    private ICategoryService catalogService ;

    @Autowired
    private CloudStorageConsumer storageConsumer ;

    @Value("${alinesno.file.local.path:${java.io.tmpdir}}")
    private String localPath  ;

    /**
     * 获取TransEntity的DataTables数据。
     *
     * @param request HttpServletRequest对象。
     * @param model Model对象。
     * @param page DatatablesPageBean对象。
     * @return 包含DataTables数据的TableDataInfo对象。
     */
    @ResponseBody
    @PostMapping("/datatables")
    public TableDataInfo datatables(HttpServletRequest request, Model model, DatatablesPageBean page) {
        log.debug("page = {}", ToStringBuilder.reflectionToString(page));
        return this.toPage(model, this.getFeign(), page);
    }

    /**
     * 文件上传
     * @return
     */
    @SneakyThrows
    @PostMapping("/importData")
    public AjaxResult importData(@RequestPart("file") MultipartFile file){

        ResourceEntity resourceEntity = new ResourceEntity() ;

        // 新生成的文件名称
        String fileSuffix = Objects.requireNonNull(file.getOriginalFilename()).substring(file.getOriginalFilename().lastIndexOf(".")+1);
        String fileName = file.getOriginalFilename();
        String aliasName = IdUtil.getSnowflakeNextIdStr() ;
        String newFileName = aliasName + "." + fileSuffix;

        resourceEntity.setFileName(fileName);
        resourceEntity.setAlias(aliasName);
        resourceEntity.setDirectory(false);

        // 复制文件
        File targetFile = new File(localPath , newFileName);
        FileUtils.writeByteArrayToFile(targetFile, file.getBytes());

        log.debug("newFileName = {} , targetFile = {}" , newFileName , targetFile.getAbsoluteFile());

        R<String> ajaxResult = storageConsumer.upload(targetFile , "qiniu-kodo" , progress -> {
            System.out.println("total bytes: " + progress.getTotalBytes());
            System.out.println("current bytes: " + progress.getCurrentBytes());
            System.out.println("progress: " + Math.round(progress.getRate() * 100) + "%");
        }) ;

        log.debug("ajaxResult= {}" , ajaxResult);

        resourceEntity.setType(ResourceTypeEnums.FILE);
        resourceEntity.setPid(0);
        resourceEntity.setSize(targetFile.length());
        resourceEntity.setStorageId(Long.parseLong(ajaxResult.getData()));

        service.save(resourceEntity) ;

        return AjaxResult.success("上传成功." , resourceEntity.getId()) ;
    }

    /**
     * 获取到所有的资源列表，并返回如下格式:
     * [{key:xxx,value:xxx}]
     */
    @GetMapping("/getAllResource")
    public AjaxResult getAllResource(){
        List<ResourceEntity> list = service.list() ;

        List<Map<String, Object>> result = list.stream().map(item -> {

            Map<String , Object> map = new java.util.HashMap<>();
            map.put("value", item.getId()) ;
            map.put("label" , item.getFileName()) ;

            return map ;
        }).toList();

        return AjaxResult.success("success" , result) ;
    }

    @GetMapping("/catalogTreeSelect")
    public AjaxResult catalogTreeSelect(){
        return AjaxResult.success("success" , catalogService.selectCatalogTreeList()) ;
    }

    @Override
    public IResourceService getFeign() {
        return this.service;
    }
}
