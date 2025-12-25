// InMemoryDocumentChunkRepository.java
package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.storage.impl;

import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.DocumentChunk;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.storage.DocumentChunkRepository;
import org.springframework.stereotype.Component;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * 基于内存的文档块存储实现（线程安全）
 * <p>
 * 使用 ConcurrentHashMap + 读写锁保证并发安全，
 * 适用于中小规模文档集（<1000 文件）。
 * 生产环境可替换为数据库实现。
 * </p>
 */
@Component
public class InMemoryDocumentChunkRepository implements DocumentChunkRepository {

    private final Map<String, List<DocumentChunk>> storage = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public void saveChunks(String sourceFile, List<DocumentChunk> chunks) {
        storage.put(sourceFile, new ArrayList<>(chunks)); // defensive copy
    }

    @Override
    public List<DocumentChunk> findBySource(String filename) {
        return new ArrayList<>(storage.getOrDefault(filename, Collections.emptyList()));
    }

    @Override
    public boolean exists(String filename) {
        return storage.containsKey(filename);
    }
}