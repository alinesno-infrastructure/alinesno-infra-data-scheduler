package com.alinesno.infra.data.scheduler.api.controller;

import cn.hutool.core.util.IdUtil;
import com.alinesno.infra.common.core.constants.SpringInstanceScope;
import com.alinesno.infra.common.facade.pageable.DatatablesPageBean;
import com.alinesno.infra.common.facade.pageable.TableDataInfo;
import com.alinesno.infra.common.facade.response.AjaxResult;
import com.alinesno.infra.common.web.adapter.rest.BaseController;
import com.alinesno.infra.data.scheduler.adapter.CloudStorageConsumer;
import com.alinesno.infra.data.scheduler.entity.ResourceEntity;
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
    public AjaxResult importData(@RequestPart("file") MultipartFile file, String type , String updateSupport){

        // 新生成的文件名称
        String fileSuffix = Objects.requireNonNull(file.getOriginalFilename()).substring(file.getOriginalFilename().lastIndexOf(".")+1);
        String newFileName = IdUtil.getSnowflakeNextId() + "." + fileSuffix;

        // 复制文件
        File targetFile = new File(localPath , newFileName);
        FileUtils.writeByteArrayToFile(targetFile, file.getBytes());

        log.debug("newFileName = {} , targetFile = {}" , newFileName , targetFile.getAbsoluteFile());

        if("img".equals(type)){  // 图片上传类型
            AjaxResult ajaxResult = storageConsumer.upload(targetFile , "qiniu-kodo" , progress -> {
                System.out.println("total bytes: " + progress.getTotalBytes());
                System.out.println("current bytes: " + progress.getCurrentBytes());
                System.out.println("progress: " + Math.round(progress.getRate() * 100) + "%");
            }) ;

            log.debug("ajaxResult= {}" , ajaxResult);
            return ajaxResult ;
        }

        return AjaxResult.success();
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
