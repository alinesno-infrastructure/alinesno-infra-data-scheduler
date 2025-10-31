package com.alinesno.infra.data.scheduler.spark.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.net.URL;

/**
 * 上传结果封装类
 */
@Data
@AllArgsConstructor
public class UploadResult {
    private URL presignedUrl;
    private String objectKey;
}