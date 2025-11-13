// ConditionNode.java
package com.alinesno.infra.data.scheduler.workflow.nodes.step;

import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSON;
import com.alinesno.infra.data.scheduler.api.logger.NodeLog;
import com.alinesno.infra.data.scheduler.workflow.constants.FlowConst;
import com.alinesno.infra.data.scheduler.workflow.nodes.AbstractFlowNode;
import com.alinesno.infra.data.scheduler.workflow.nodes.step.compare.*;
import com.alinesno.infra.data.scheduler.workflow.nodes.variable.step.ConditionNodeData;
import com.alinesno.infra.data.scheduler.workflow.utils.StackTraceUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * 判断器节点，增加 nodeLogService 日志
 */
@Slf4j
@Data
@Scope("prototype")
@Service(FlowConst.FLOW_STEP_NODE + "condition")
@EqualsAndHashCode(callSuper = true)
public class ConditionNode extends AbstractFlowNode {

    static List<ConditionVerifier> conditionVerifiers = new ArrayList<>();

    static {
        conditionVerifiers.add(new GEConditionVerifier());
        conditionVerifiers.add(new GTConditionVerifier());
        conditionVerifiers.add(new ContainConditionVerifier());
        conditionVerifiers.add(new EqualConditionVerifier());
        conditionVerifiers.add(new LTConditionVerifier());
        conditionVerifiers.add(new LEConditionVerifier());
        conditionVerifiers.add(new LengthLEConditionVerifier());
        conditionVerifiers.add(new LengthLTConditionVerifier());
        conditionVerifiers.add(new LengthEqualConditionVerifier());
        conditionVerifiers.add(new LengthGEConditionVerifier());
        conditionVerifiers.add(new LengthGTConditionVerifier());
        conditionVerifiers.add(new IsNullConditionVerifier());
        conditionVerifiers.add(new IsNotNullConditionVerifier());
        conditionVerifiers.add(new NotContainConditionVerifier());
    }

    public ConditionNode() {
        setType("condition");
    }

    @Override
    protected CompletableFuture<Void> handleNode() {
        log.debug("ConditionNode:{}" , node.toString());
        ConditionNodeData nodeData = JSON.parseObject(JSON.toJSONString(node.getProperties()), ConditionNodeData.class);
        log.debug("node type = {} output = {}" , node.getType() , JSONUtil.toJsonPrettyStr(nodeData));

        // 开始日志
        try {
            nodeLogService.append(NodeLog.of(
                    String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown"),
                    node.getId(),
                    node.getStepName(),
                    "INFO",
                    "Condition节点开始执行",
                    Map.of(
                            "nodeType", "condition",
                            "branchCount", nodeData.getBranchConditionList() == null ? 0 : nodeData.getBranchConditionList().size()
                    )
            ));
        } catch (Exception ignore) {
            log.warn("记录 Condition 开始日志失败: {}", ignore.getMessage());
        }

        Map<String , String> branchMap = new HashMap<>();
        boolean isHasBranch = false;

        try {
            List<ConditionNodeData.BranchCondition> branchList = nodeData.getBranchConditionList();
            if (branchList == null || branchList.isEmpty()) {
                try {
                    nodeLogService.append(NodeLog.of(
                            String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown"),
                            node.getId(),
                            node.getStepName(),
                            "WARN",
                            "Condition节点没有配置分支",
                            Map.of("phase", "branch.empty")
                    ));
                } catch (Exception ignore) {
                    log.warn("记录 branch.empty 日志失败: {}", ignore.getMessage());
                }
            } else {
                for (ConditionNodeData.BranchCondition branchCondition : branchList) {
                    // 记录正在评估的分支
                    try {
                        nodeLogService.append(NodeLog.of(
                                String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown"),
                                node.getId(),
                                node.getStepName(),
                                "DEBUG",
                                "评估分支条件",
                                Map.of(
                                        "branchId", branchCondition.getId(),
                                        "branchType", branchCondition.getType(),
                                        "conditions", branchCondition.getConditions() == null ? "[]" : branchCondition.getConditions().toString()
                                )
                        ));
                    } catch (Exception ignore) {
                        log.debug("记录评估分支日志失败: {}", ignore.getMessage());
                    }

                    List<ConditionNodeData.Condition> conditions = branchCondition.getConditions();

                    if (!CollectionUtils.isEmpty(conditions)) {
                        ConditionNodeData.Condition conditionField = conditions.get(0);
                        List<Object> field = conditionField.getField();
                        String nodeId = String.valueOf(field.get(0));
                        String keyName = String.valueOf(field.get(1));

                        Object fieldValue = workflowManage.getReferenceField(nodeId, keyName);
                        log.debug("fieldValue = {}" , fieldValue);

                        for (ConditionVerifier conditionVerifier : conditionVerifiers) {
                            if (conditionVerifier.isSupported(conditionField.getCompare())) {
                                boolean passed = false;
                                try {
                                    passed = conditionVerifier.verify(fieldValue, conditionField.getValue());
                                } catch (Exception verEx) {
                                    // 验证器内部异常记录
                                    try {
                                        nodeLogService.append(NodeLog.of(
                                                String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown"),
                                                node.getId(),
                                                node.getStepName(),
                                                "ERROR",
                                                "条件验证器执行异常",
                                                Map.of(
                                                        "branchId", branchCondition.getId(),
                                                        "verifier", conditionVerifier.getClass().getSimpleName(),
                                                        "exception", verEx.getMessage(),
                                                        "stackTrace", StackTraceUtils.getStackTrace(verEx)
                                                )
                                        ));
                                    } catch (Exception ignore) {
                                        log.warn("记录验证器异常日志失败: {}", ignore.getMessage());
                                    }
                                }

                                if (passed) {
                                    branchMap.put("branch_id", branchCondition.getId());
                                    branchMap.put("branch_name", branchCondition.getType());
                                    isHasBranch = true;
                                    try {
                                        nodeLogService.append(NodeLog.of(
                                                String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown"),
                                                node.getId(),
                                                node.getStepName(),
                                                "INFO",
                                                "分支匹配成功",
                                                Map.of(
                                                        "branchId", branchCondition.getId(),
                                                        "branchName", branchCondition.getType(),
                                                        "matchedBy", conditionVerifier.getClass().getSimpleName(),
                                                        "fieldNodeId", nodeId,
                                                        "fieldKey", keyName,
                                                        "fieldValue", String.valueOf(fieldValue)
                                                )
                                        ));
                                    } catch (Exception ignore) {
                                        log.warn("记录 branch.match 日志失败: {}", ignore.getMessage());
                                    }
                                    break;
                                }
                            }
                        }
                    } else {
                        // 分支无条件，视为 ELSE 或默认分支（记录并继续）
                        try {
                            nodeLogService.append(NodeLog.of(
                                    String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown"),
                                    node.getId(),
                                    node.getStepName(),
                                    "DEBUG",
                                    "分支配置为空条件，作为默认候选",
                                    Map.of("branchId", branchCondition.getId())
                            ));
                        } catch (Exception ignore) {
                            log.debug("记录空条件分支日志失败: {}", ignore.getMessage());
                        }
                    }

                    if (isHasBranch) break;
                }

                if (!isHasBranch) {
                    // 未匹配到任何分支，标记为 ELSE（采用最后一个分支或明确 ELSE）
                    // 这里保留行为：把当前循环最后一个分支当作 ELSE
                    ConditionNodeData.BranchCondition last = branchList.get(branchList.size() - 1);
                    branchMap.put("branch_id", last.getId());
                    branchMap.put("branch_name", "ELSE");
                    try {
                        nodeLogService.append(NodeLog.of(
                                String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown"),
                                node.getId(),
                                node.getStepName(),
                                "INFO",
                                "未匹配到分支，使用 ELSE",
                                Map.of("selectedBranchId", last.getId(), "selectedBranchName", "ELSE")
                        ));
                    } catch (Exception ignore) {
                        log.warn("记录 else 选择日志失败: {}", ignore.getMessage());
                    }
                }
            }

            output.put(node.getStepName() + ".branch_map", branchMap);
            output.put(node.getStepName() + ".branch_name", branchMap.get("branch_name"));

            // 结束日志
            try {
                nodeLogService.append(NodeLog.of(
                        String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown"),
                        node.getId(),
                        node.getStepName(),
                        "INFO",
                        "Condition节点执行完成",
                        Map.of(
                                "selectedBranch", branchMap,
                                "isHasBranch", isHasBranch
                        )
                ));
            } catch (Exception ignore) {
                log.warn("记录 Condition 完成日志失败: {}", ignore.getMessage());
            }

        } catch (Throwable t) {
            // 异常处理与日志记录
            try {
                nodeLogService.append(NodeLog.of(
                        String.valueOf(flowExecution != null ? flowExecution.getId() : "unknown"),
                        node.getId(),
                        node.getStepName(),
                        "ERROR",
                        "Condition节点执行异常: " + t.getMessage(),
                        Map.of(
                                "phase", "node.execute",
                                "exception", t.getMessage(),
                                "stackTrace", StackTraceUtils.getStackTrace(t)
                        )
                ));
            } catch (Exception ignore) {
                log.warn("记录 Condition 异常日志失败: {}", ignore.getMessage());
            }
            log.error("ConditionNode 执行异常: {}", t.getMessage(), t);
        }

        return CompletableFuture.completedFuture(null);
    }
}