package com.alinesno.infra.data.scheduler.workflow.plugin.reader;

import com.alinesno.infra.data.scheduler.adapter.dto.FileAttachmentDto;
import com.alinesno.infra.data.scheduler.workflow.plugin.extractor.dto.UploadData;

/**
 * 附件内容解析和读取
 */
public interface IAttachmentReaderService {

    /**
     * 读取附件内容
     *
     * @param attachmentDto
     * @param uploadData
     * @return
     */
    String readAttachment(FileAttachmentDto attachmentDto, UploadData uploadData);

}
