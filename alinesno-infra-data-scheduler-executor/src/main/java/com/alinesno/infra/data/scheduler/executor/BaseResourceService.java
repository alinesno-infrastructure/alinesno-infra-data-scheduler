package com.alinesno.infra.data.scheduler.executor;

import com.alinesno.infra.data.scheduler.adapter.CloudStorageConsumer;
import com.alinesno.infra.data.scheduler.entity.ResourceEntity;
import com.alinesno.infra.data.scheduler.service.IResourceService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class BaseResourceService {

    @Autowired
    protected CloudStorageConsumer storageConsumer;

    @Value("${alinesno.file.local.path:${java.io.tmpdir}}")
    protected String localPath;

    @Autowired
    private IResourceService resourceService;

    /**
     * 下载文件资源并返回文件名称
     *
     * @param resourceIds
     * @return
     */
    @SneakyThrows
    protected List<String> downloadResource(List<String> resourceIds, String workspace) {

        List<String> fileNameList = new ArrayList<>();

        List<ResourceEntity> resourceEntities = resourceService.listByIds(resourceIds);

        for (ResourceEntity resource : resourceEntities) {
            byte[] bytes = storageConsumer.download(String.valueOf(resource.getStorageId()), progress -> log.debug("下载进度：" + progress.getRate()));

            File targetFile = new File(workspace, resource.getFileName());
            FileUtils.writeByteArrayToFile(targetFile, bytes);

            fileNameList.add(targetFile.getAbsolutePath());
        }

        return fileNameList;
    }

}
