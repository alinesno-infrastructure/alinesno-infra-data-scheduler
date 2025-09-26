package com.alinesno.infra.data.scheduler.spark.utils;

import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * OSS 上传/签名/删除工具（用于上传 spark-sql 文件并生成短期可访问 URL）
 *
 * 注意：不在本类中持有 OSS 客户端单例（每次创建并关闭），若高频调用可改为单例管理并在应用关闭时释放。
 */
@Slf4j
public class OssSqlStorage {

    private final String endpoint;
    private final String bucket;
    private final String accessKeyId;
    private final String accessKeySecret;
    private final String basePath ;

    public OssSqlStorage(String endpoint, String bucket, String accessKeyId, String accessKeySecret, String basePath) {
        this.endpoint = endpoint;
        this.bucket = bucket;
        this.accessKeyId = accessKeyId;
        this.accessKeySecret = accessKeySecret;
        this.basePath = basePath == null ? "" : basePath;
    }

    @Data
    @AllArgsConstructor
    public static class UploadResult {
        private URL presignedUrl;
        private String objectKey;
    }

    /**
     * 上传本地文件到 OSS，并生成一个预签名 URL（expiry 指定有效期）。
     */
    public UploadResult uploadFileGeneratePresignedUrl(Path localFile, Duration expiry) throws Exception {
        if (localFile == null || !Files.exists(localFile)) {
            throw new IllegalArgumentException("localFile not exists: " + localFile);
        }
        String fileName = localFile.getFileName().toString();
        String objectKey = buildObjectKey(fileName);

        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            ossClient.putObject(bucket, objectKey, localFile.toFile());

            Date expiration = Date.from(Instant.now().plusMillis(expiry.toMillis()));
            URL url = ossClient.generatePresignedUrl(bucket, objectKey, expiration);
            return new UploadResult(url, objectKey);
        } finally {
            if (ossClient != null) {
                try { ossClient.shutdown(); } catch (Exception ignored) {}
            }
        }
    }

    /**
     * 便捷：把字符串内容写到临时文件上传并生成预签名 URL，函数内部会删除临时文件。
     */
    public UploadResult uploadStringAsTempFile(String content, Duration expiry) throws Exception {
        Path tmp = null;
        try {
            tmp = Files.createTempFile("spark-sql-", ".sql");
            Files.write(tmp, content.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return uploadFileGeneratePresignedUrl(tmp, expiry);
        } finally {
            if (tmp != null) {
                try { Files.deleteIfExists(tmp); } catch (IOException ignored) {}
            }
        }
    }

    /**
     * 删除 OSS 对象（若存在）
     */
    public void deleteObject(String objectKey) {
        if (objectKey == null || objectKey.isEmpty()) return;
        OSS ossClient = null;
        try {
            ossClient = new OSSClientBuilder().build(endpoint, accessKeyId, accessKeySecret);
            ossClient.deleteObject(bucket, objectKey);
            log.info("Deleted OSS object: {}/{}", bucket, objectKey);
        } catch (Exception ex) {
            log.warn("Failed to delete OSS object {}/{}: {}", bucket, objectKey, ex.getMessage());
        } finally {
            if (ossClient != null) {
                try { ossClient.shutdown(); } catch (Exception ignored) {}
            }
        }
    }

    private String buildObjectKey(String fileName) {
        String prefix = basePath == null ? "" : basePath;
        if (!prefix.isEmpty() && !prefix.endsWith("/")) prefix = prefix + "/";
        // 加 uuid 或时间戳避免冲突
        String unique = Instant.now().toEpochMilli() + "-" + UUID.randomUUID().toString();
        return prefix + unique + "-" + fileName;
    }
}