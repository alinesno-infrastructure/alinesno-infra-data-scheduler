// DocumentChunkRepository.java
package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.storage;

import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.DocumentChunk;

import java.util.List;

/**
 * 文档块存储仓库接口
 * <p>
 * 提供文档块的持久化与查询能力。实现需保证线程安全。
 * </p>
 */
public interface DocumentChunkRepository {
    /**
     * 保存指定文件的文档块列表
     * @param sourceFile 原始文件名
     * @param chunks 文档块列表
     */
    void saveChunks(String sourceFile, List<DocumentChunk> chunks);

    /**
     * 根据文件名查询文档块
     * @param filename 文件名
     * @return 文档块列表，若不存在则返回空列表
     */
    List<DocumentChunk> findBySource(String filename);

    /**
     * 判断文件是否已解析
     * @param filename 文件名
     * @return true 已存在，false 未解析
     */
    boolean exists(String filename);
}