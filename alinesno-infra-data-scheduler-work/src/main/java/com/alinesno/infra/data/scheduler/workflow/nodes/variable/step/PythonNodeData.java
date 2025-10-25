package com.alinesno.infra.data.scheduler.workflow.nodes.variable.step;

import com.alinesno.infra.data.scheduler.workflow.nodes.variable.NodeData;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文本转语音（TextToSpeech）的节点数据类
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class PythonNodeData extends NodeData {

    private String pythonScript ;

}