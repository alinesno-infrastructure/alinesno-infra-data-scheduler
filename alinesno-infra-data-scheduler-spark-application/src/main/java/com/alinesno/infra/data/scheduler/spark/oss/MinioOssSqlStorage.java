package com.alinesno.infra.data.scheduler.spark.oss;

import com.alinesno.infra.data.scheduler.spark.model.UploadResult;
import io.minio.*;
import io.minio.http.Method;
import io.minio.errors.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MinIO 上传/签名/删除工具（用于上传 spark-sql 文件并生成短期可访问 URL）
 * 实现与阿里云 OSS 相同的功能接口，适配 MinIO 的 SDK 特性
 */
@Slf4j
@NoArgsConstructor
public class MinioOssSqlStorage implements IOssSqlStorage {

    private String endpoint;
    private String bucket;
    private String accessKeyId;
    private String accessKeySecret;
    private String basePath;
    private MinioClient minioClient;

    public MinioOssSqlStorage(String endpoint, String bucket, String accessKeyId, String accessKeySecret, String basePath) {
        this.endpoint = endpoint;
        this.bucket = bucket;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.basePath = basePath == null ? "" : normalizeBasePath(basePath);

        // 初始化 MinIO 客户端
        this.minioClient = MinioClient.builder()
                .endpoint(this.endpoint)
                .credentials(this.accessKeyId, this.accessKeySecret)
                .build();

        // 确保桶存在
        ensureBucketExists();
    }

    /**
     * 标准化基础路径（确保以斜杠结尾且不包含多余斜杠）
     */
    private String normalizeBasePath(String path) {
        if (path.isEmpty()) return "";
        String normalized = path.replaceAll("//+", "/");
        return normalized.endsWith("/") ? normalized : normalized + "/";
    }

    /**
     * 确保存储桶存在，不存在则创建
     */
    private void ensureBucketExists() {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("Created MinIO bucket: {}", bucket);
            }
        } catch (Exception e) {
            log.error("Failed to check or create bucket {}: {}", bucket, e.getMessage(), e);
        }
    }

    /**
     * 上传本地文件到 MinIO，并生成预签名 URL
     */
    public UploadResult uploadFileGeneratePresignedUrl(Path localFile, Duration expiry) throws Exception {
        if (localFile == null || !Files.exists(localFile)) {
            throw new IllegalArgumentException("Local file not exists: " + localFile);
        }

        String fileName = localFile.getFileName().toString();
        String objectKey = buildObjectKey(fileName);

        try (InputStream is = Files.newInputStream(localFile)) {
            // 上传文件到 MinIO
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(is, is.available(), -1)
                            .build()
            );

            // 生成预签名 URL
            String url = minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .method(Method.GET)
                            .bucket(bucket)
                            .object(objectKey)
                            .expiry(600, TimeUnit.SECONDS)
                            .build()
            );

            return new UploadResult(new URL(url), objectKey);
        }
    }

    /**
     * 将字符串内容写入临时文件并上传到 MinIO，返回预签名 URL
     */
    public UploadResult uploadStringAsTempFile(String content, Duration expiry) throws Exception {
        Path tmpFile = null;
        try {
            tmpFile = Files.createTempFile("spark-sql-", ".sql");
            Files.write(tmpFile, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return uploadFileGeneratePresignedUrl(tmpFile, expiry);
        } finally {
            if (tmpFile != null) {
                try {
                    Files.deleteIfExists(tmpFile);
                } catch (IOException e) {
                    log.warn("Failed to delete temporary file: {}", tmpFile, e);
                }
            }
        }
    }

    /**
     * 删除 MinIO 中的对象
     */
    public void deleteObject(String objectKey) {
        if (objectKey == null || objectKey.isEmpty()) {
            return;
        }

        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .build()
            );
            log.info("Deleted MinIO object: {}/{}", bucket, objectKey);
        } catch (Exception e) {
            log.warn("Failed to delete MinIO object {}/{}: {}", bucket, objectKey, e.getMessage(), e);
        }
    }

    /**
     * 构建对象存储键（包含基础路径和唯一标识）
     */
    private String buildObjectKey(String fileName) {
        String uniqueId = Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString().substring(0, 8);
        return basePath + uniqueId + "-" + fileName;
    }
}