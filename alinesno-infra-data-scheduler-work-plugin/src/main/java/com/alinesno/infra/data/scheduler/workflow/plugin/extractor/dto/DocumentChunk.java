// DocumentChunk.java
package com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto;

import lombok.Data;
import java.util.Map;

/**
 * 文档块统一中间表示
 * <p>
 * 将异构文件（PDF/Word/Excel等）解析为标准化文本块，
 * 保留来源、位置、结构等元信息，供后续抽取使用。
 * </p>
 */
@Data
public class DocumentChunk {
    /** 块内文本内容 */
    private String content;

    /** 原始文件名 */
    private String sourceFile;

    /** 文件类型，如 "PDF", "DOCX" */
    private String fileType;

    /** 页码（适用于 PDF/Word），从1开始 */
    private Integer pageNumber;

    /** 逻辑章节路径，如 "年报/财务摘要/利润表" */
    private String sectionPath;

    /** 是否来自表格区域 */
    private boolean isTable;

    /** 扩展元数据（如表格行列坐标） */
    private Map<String, Object> metadata;
}