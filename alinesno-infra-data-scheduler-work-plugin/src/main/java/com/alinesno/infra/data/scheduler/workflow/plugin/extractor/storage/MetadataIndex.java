// MetadataIndex.java
package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.storage;

import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.DocumentChunk;

import java.util.List;
import java.util.Set;

/**
 * 元数据索引接口
 * <p>
 * 提供基于关键词的文档块检索能力，不依赖向量数据库。
 * 实现需支持高并发读写。
 * </p>
 */
public interface MetadataIndex {
    /**
     * 索引单个文档块
     * @param chunk 文档块
     */
    void index(DocumentChunk chunk);

    /**
     * 批量重建索引（用于初始化）
     * @param allChunks 所有文档块
     */
    void rebuildIndex(Iterable<DocumentChunk> allChunks);

    /**
     * 批量关键词检索
     * @param keywords 关键词列表（OR 关系）
     * @param fileTypes 文件类型集合
     * @param sectionPattern 章节路径正则（暂未实现，预留）
     * @param sourceFiles 限定文件列表（null 表示不限）
     * @return 匹配的文档块列表
     */
    List<DocumentChunk> searchBulk(List<String> keywords, Set<String> fileTypes, String sectionPattern, List<String> sourceFiles);
}