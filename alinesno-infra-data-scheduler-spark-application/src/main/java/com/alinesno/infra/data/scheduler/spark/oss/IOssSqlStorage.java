package com.alinesno.infra.data.scheduler.spark.oss;

import com.alinesno.infra.data.scheduler.spark.model.UploadResult;

import java.nio.file.Path;
import java.time.Duration;
import java.net.URL;

/**
 * OSS 存储服务接口，定义 SQL 脚本上传、签名 URL 生成和对象删除的标准方法
 * 统一阿里云 OSS 和 MinIO 的实现规范
 */
public interface IOssSqlStorage {

    /**
     * 上传本地文件到 OSS，并生成指定有效期的预签名 URL
     *
     * @param localFile 本地文件路径
     * @param expiry    预签名 URL 的有效期
     * @return 包含预签名 URL 和对象存储键的上传结果
     * @throws Exception 上传过程中发生的异常（IO 异常、权限异常等）
     */
    UploadResult uploadFileGeneratePresignedUrl(Path localFile, Duration expiry) throws Exception;

    /**
     * 将字符串内容写入临时文件并上传到 OSS，生成预签名 URL
     * 临时文件会在上传完成后自动删除
     *
     * @param content 待上传的字符串内容（通常为 SQL 脚本）
     * @param expiry  预签名 URL 的有效期
     * @return 包含预签名 URL 和对象存储键的上传结果
     * @throws Exception 上传过程中发生的异常
     */
    UploadResult uploadStringAsTempFile(String content, Duration expiry) throws Exception;

    /**
     * 删除 OSS 中指定的对象
     *
     * @param objectKey 对象在 OSS 中的存储键（由上传方法返回）
     */
    void deleteObject(String objectKey);

}