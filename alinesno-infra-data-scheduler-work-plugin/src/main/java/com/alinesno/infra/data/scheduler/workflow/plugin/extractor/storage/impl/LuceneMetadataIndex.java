//// LuceneMetadataIndex.java
//package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.storage.impl;
//
//import com.alinesno.infra.data.scheduler.workflow.plugin.dto.DocumentChunk;
//import org.apache.lucene.analysis.standard.StandardAnalyzer;
//import org.apache.lucene.document.*;
//import org.apache.lucene.index.*;
//import org.apache.lucene.search.*;
//import org.apache.lucene.store.ByteBuffersDirectory;
//import org.apache.lucene.queryparser.classic.ParseException;
//import org.apache.lucene.queryparser.classic.QueryParser;
//import org.springframework.stereotype.Component;
//
//import java.io.IOException;
//import java.util.*;
//import java.util.concurrent.locks.ReentrantReadWriteLock;
//import java.util.stream.Collectors;
//
///**
// * 基于 Apache Lucene 的元数据索引实现（线程安全）
// * <p>
// * 使用内存目录（ByteBuffersDirectory）构建轻量级倒排索引，
// * 支持关键词 OR 查询、文件类型过滤。
// * 所有写操作加写锁，读操作加读锁，保证并发安全。
// * </p>
// */
//@Component
//public class LuceneMetadataIndex implements MetadataIndex {
//
//    private final Directory directory = new ByteBuffersDirectory();
//    private final Analyzer analyzer = new StandardAnalyzer();
//    private volatile IndexSearcher searcher;
//    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
//
//    public LuceneMetadataIndex() throws IOException {
//        // 初始化空索引
//        try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
//            writer.commit();
//        }
//        refreshSearcher();
//    }
//
//    @Override
//    public void index(DocumentChunk chunk) {
//        lock.writeLock().lock();
//        try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
//            Document doc = new Document();
//            doc.add(new TextField("content", chunk.getContent(), Field.Store.NO));
//            doc.add(new StringField("sourceFile", chunk.getSourceFile(), Field.Store.YES));
//            doc.add(new StringField("fileType", chunk.getFileType(), Field.Store.YES));
//            writer.addDocument(doc);
//            writer.commit();
//            refreshSearcher();
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to index document chunk", e);
//        } finally {
//            lock.writeLock().unlock();
//        }
//    }
//
//    @Override
//    public void rebuildIndex(Iterable<DocumentChunk> allChunks) {
//        lock.writeLock().lock();
//        try (IndexWriter writer = new IndexWriter(directory, new IndexWriterConfig(analyzer))) {
//            writer.deleteAll(); // 清空旧索引
//            for (DocumentChunk chunk : allChunks) {
//                Document doc = new Document();
//                doc.add(new TextField("content", chunk.getContent(), Field.Store.NO));
//                doc.add(new StringField("sourceFile", chunk.getSourceFile(), Field.Store.YES));
//                doc.add(new StringField("fileType", chunk.getFileType(), Field.Store.YES));
//                writer.addDocument(doc);
//            }
//            writer.commit();
//            refreshSearcher();
//        } catch (IOException e) {
//            throw new RuntimeException("Failed to rebuild index", e);
//        } finally {
//            lock.writeLock().unlock();
//        }
//    }
//
//    @Override
//    public List<DocumentChunk> searchBulk(List<String> keywords, Set<String> fileTypes, String sectionPattern, List<String> sourceFiles) {
//        if (keywords.isEmpty()) return Collections.emptyList();
//
//        lock.readLock().lock();
//        try {
//            // 构造关键词 OR 查询
//            BooleanQuery.Builder keywordQuery = new BooleanQuery.Builder();
//            for (String kw : keywords) {
//                keywordQuery.add(new TermQuery(new Term("content", kw.toLowerCase())), BooleanClause.Occur.SHOULD);
//            }
//
//            // 构造文件类型 AND 查询
//            BooleanQuery.Builder fileTypeQuery = new BooleanQuery.Builder();
//            for (String type : fileTypes) {
//                fileTypeQuery.add(new TermQuery(new Term("fileType", type)), BooleanClause.Occur.SHOULD);
//            }
//
//            // 合并查询
//            BooleanQuery.Builder finalQuery = new BooleanQuery.Builder();
//            finalQuery.add(keywordQuery.build(), BooleanClause.Occur.MUST);
//            finalQuery.add(fileTypeQuery.build(), BooleanClause.Occur.MUST);
//
//            // 执行搜索
//            TopDocs topDocs = searcher.search(finalQuery.build(), 10000); // 假设最多返回1万条
//
//            // 过滤 sourceFiles（Lucene 不擅长复杂过滤，用 Java 过滤更简单）
//            Set<String> allowedFiles = (sourceFiles == null || sourceFiles.isEmpty()) ? null : new HashSet<>(sourceFiles);
//            List<DocumentChunk> results = new ArrayList<>();
//            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
//                Document doc = searcher.doc(scoreDoc.doc);
//                String fileName = doc.get("sourceFile");
//                if (allowedFiles != null && !allowedFiles.contains(fileName)) {
//                    continue;
//                }
//                // 注意：此处无法还原完整 DocumentChunk（因 content 未存储）
//                // 实际使用中应结合 DocumentChunkRepository 获取完整对象
//                // 此处仅返回文件名，由上层去 repository 查完整 chunk
//                // 为简化，我们假设上层会重新加载 —— 生产环境建议存储更多字段
//                results.add(stubChunk(fileName, doc.get("fileType")));
//            }
//            return results;
//        } catch (IOException e) {
//            throw new RuntimeException("Search failed", e);
//        } finally {
//            lock.readLock().unlock();
//        }
//    }
//
//    private DocumentChunk stubChunk(String sourceFile, String fileType) {
//        DocumentChunk chunk = new DocumentChunk();
//        chunk.setSourceFile(sourceFile);
//        chunk.setFileType(fileType);
//        chunk.setContent(""); // 占位，实际内容由 repository 提供
//        return chunk;
//    }
//
//    private void refreshSearcher() throws IOException {
//        IndexReader newReader = DirectoryReader.open(directory);
//        this.searcher = new IndexSearcher(newReader);
//    }
//}