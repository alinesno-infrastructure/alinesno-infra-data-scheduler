package com.alinesno.infra.data.scheduler.adapter;

import com.alinesno.infra.common.facade.response.R;
import com.dtflys.forest.annotation.*;
import com.dtflys.forest.callback.OnProgress;

import java.io.File;

/**
 * 云文件存储上传
 */
@BaseRequest(baseURL = "#{alinesno.infra.gateway.host}/base-storage" , connectTimeout = 30*1000)
public interface CloudStorageConsumer {

    /**
     * 上传文件到存储
     *
     * @param file       要上传的文件，使用@DataFile注解表明这是一个文件参数
     * @param platform   平台信息，用于区分上传文件的来源或用途
     * @param onProgress 上传进度回调接口，用于实时获取上传进度
     * @return 返回一个R类型对象，包含上传结果信息
     */
    @Post(url = "/api/base/storage/upload", contentType = "multipart/form-data")
    R<String> upload(@DataFile("file") File file, @Body("platform") String platform, OnProgress onProgress);

    /**
     * 从存储下载文件
     *
     * @param storageId  存储文件的ID，用于定位特定的文件
     * @param onProgress 下载进度回调接口，用于实时获取下载进度
     * @return 返回字节数组，包含文件的二进制内容
     */
    @Get(url = "/api/base/storage/download")
    byte[] download(@Query("storageId") String storageId, OnProgress onProgress);

}