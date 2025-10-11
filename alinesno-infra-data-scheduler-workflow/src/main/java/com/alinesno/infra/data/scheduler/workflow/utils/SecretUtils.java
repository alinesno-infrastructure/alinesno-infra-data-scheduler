package com.alinesno.infra.data.scheduler.workflow.utils;

import java.util.*;
import java.util.regex.*;

import com.alinesno.infra.common.web.log.utils.SpringUtils;
import com.alinesno.infra.data.scheduler.workflow.dto.FlowNodeDto;
import com.alinesno.infra.data.scheduler.workflow.entity.FlowExecutionEntity;
import com.alinesno.infra.data.scheduler.workflow.logger.NodeLog;
import com.alinesno.infra.data.scheduler.workflow.logger.NodeLogService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class SecretUtils {

    /**
     * 检测替换结果中是否仍然存在未解析的占位符，并记录到本地日志与 NodeLogService。
     *
     * @param replacedResult 替换后的字符串
     * @param node        FlowNodeDto，上下文信息（用于构造 NodeLog），请确保其有下述访问方法或按项目实际方法名调整
     *                       - getFlowExecutionId()
     *                       - getNodeId()
     *                       - getStepName()
     *                       - getRunUniqueNumber()
     * @return 已发现的未解析 secret key 集合（按发现顺序，无重复），若无则返回空集合
     */
    public static Set<String> checkAndLogUnresolvedSecrets(String replacedResult, FlowNodeDto node ,  FlowExecutionEntity flowExecution  ,NodeLogService nodeLogService) {
        if (replacedResult == null) {
            return Collections.emptySet();
        }

        // 匹配形式：${{secrets.KEY}} 或 ${{ KEY }} 或 ${{ KEY }}（允许任意空白）
        // 捕获 group(1) = key，不管是否带 "secrets." 前缀
        Pattern pattern = Pattern.compile("\\$\\{\\{\\s*(?:secrets\\.)?([^\\s\\}]+)\\s*}}");
        Matcher matcher = pattern.matcher(replacedResult);

        Set<String> unresolvedKeys = new LinkedHashSet<>();
        Set<String> unresolvedPlaceholders = new LinkedHashSet<>(); // 保存原始占位符文本以便日志记录
        while (matcher.find()) {
            String key = matcher.group(1);
            unresolvedKeys.add(key);

            // 获取完整原始占位符（matcher.group() 返回整个匹配文本）
            unresolvedPlaceholders.add(matcher.group());
        }

        if (unresolvedKeys.isEmpty()) {
            return Collections.emptySet();
        }

        String unresolvedKeysStr = String.join(", ", unresolvedKeys);

        // 1) 写本地日志
        log.warn("Found unresolved secret placeholders for node [{} / {}]: keys={}, placeholders={}",
                node != null ? node.getNodeId() : "unknown",
                node != null ? node.getStepName() : "unknown",
                unresolvedKeysStr, unresolvedPlaceholders);

        // 2) 写入系统/节点日志（通过 NodeLogService）
        try {

            // 构造额外信息 map（按需扩展）
            Map<String, Object> extra = new HashMap<>();
            extra.put("unresolvedSecrets", unresolvedKeysStr);
            extra.put("unresolvedPlaceholders", unresolvedPlaceholders);
            if (node != null) {
                extra.put("flowExecutionId", flowExecution.getId());
                extra.put("runUniqueNumber", flowExecution.getRunUniqueNumber());
            }

            // 注意：下面 NodeLog.of(...) 的参数顺序/类型可能与你项目中不同，请按你项目实际的 NodeLog.of/NodeLog 构造器调整
            assert node != null;
            nodeLogService.append(NodeLog.of(
                    flowExecution.getId().toString(),
                    node.getId(),
                    node.getStepName(),
                    "WARN",
                    "存在未解析的密文占位符: " + unresolvedKeysStr,
                    extra
            ));
        } catch (Exception ex) {
            // 写日志失败时不要抛异常，记录错误便于排查
            log.error("Failed to write unresolved secrets to NodeLogService. keys={}, placeholders={}, error={}",
                    unresolvedKeysStr, unresolvedPlaceholders, ex.toString());
        }

        return Collections.unmodifiableSet(unresolvedKeys);
    }
}