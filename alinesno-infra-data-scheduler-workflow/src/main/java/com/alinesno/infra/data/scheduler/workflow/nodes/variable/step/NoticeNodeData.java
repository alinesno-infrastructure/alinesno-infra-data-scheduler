package com.alinesno.infra.data.scheduler.workflow.nodes.variable.step;

import com.alinesno.infra.data.scheduler.workflow.nodes.variable.NodeData;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 *  通知节点
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class NoticeNodeData extends NodeData {

    private String imId;

    private int retryCount ;

    private String noticeContent;
}
