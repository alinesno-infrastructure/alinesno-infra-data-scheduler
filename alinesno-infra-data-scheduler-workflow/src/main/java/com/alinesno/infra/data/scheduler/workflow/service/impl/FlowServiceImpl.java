package com.alinesno.infra.data.scheduler.workflow.service.impl;

import cn.hutool.core.util.IdUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.alinesno.infra.common.core.service.impl.IBaseServiceImpl;
import com.alinesno.infra.common.web.log.utils.SpringUtils;
import com.alinesno.infra.data.scheduler.entity.ProcessDefinitionEntity;
import com.alinesno.infra.data.scheduler.service.IProcessDefinitionService;
import com.alinesno.infra.data.scheduler.service.ISecretsService;
import com.alinesno.infra.data.scheduler.workflow.dto.*;
import com.alinesno.infra.data.scheduler.workflow.entity.FlowEntity;
import com.alinesno.infra.data.scheduler.workflow.entity.FlowExecutionEntity;
import com.alinesno.infra.data.scheduler.workflow.entity.FlowNodeEntity;
import com.alinesno.infra.data.scheduler.workflow.entity.FlowNodeExecutionEntity;
import com.alinesno.infra.data.scheduler.workflow.enums.ExecutionStrategyEnums;
import com.alinesno.infra.data.scheduler.workflow.enums.FlowExecutionStatus;
import com.alinesno.infra.data.scheduler.workflow.enums.PublishStatus;
import com.alinesno.infra.data.scheduler.workflow.mapper.FlowMapper;
import com.alinesno.infra.data.scheduler.workflow.nodes.AbstractFlowNode;
import com.alinesno.infra.data.scheduler.workflow.parse.LogicFlowParser;
import com.alinesno.infra.data.scheduler.workflow.parse.NodePrinter;
import com.alinesno.infra.data.scheduler.workflow.service.IFlowExecutionService;
import com.alinesno.infra.data.scheduler.workflow.service.IFlowNodeExecutionService;
import com.alinesno.infra.data.scheduler.workflow.service.IFlowNodeService;
import com.alinesno.infra.data.scheduler.workflow.service.IFlowService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;

import static com.alinesno.infra.data.scheduler.workflow.constants.FlowConst.FLOW_STEP_NODE;

/**
 * 工作流服务接口的实现类，负责处理工作流基础信息和元数据信息的具体业务逻辑。
 * 继承自 IBaseServiceImpl 类，借助该类提供的通用方法，可完成对工作流数据的基本操作。
 *
 * @author luoxiaodong
 * @version 1.0.0
 */
@Slf4j
@Service
public class FlowServiceImpl extends IBaseServiceImpl<FlowEntity, FlowMapper> implements IFlowService {

    @Autowired
    private IProcessDefinitionService processDefinitionService;

    @Autowired
    private IFlowNodeService flowNodeService;  // 流程定义

    @Autowired
    private ISecretsService secretsService ;

    @Autowired
    private IFlowExecutionService flowExecutionService ;  // 流程实例

    @Autowired
    @Qualifier("chatThreadPool")
    private ThreadPoolTaskExecutor chatThreadPool;

    @Autowired
    private IFlowNodeExecutionService flowNodeExecutionService ; // 流程节点实例

    /**
     * 运行指定角色的流程 <br/>
     * 1. 获取到角色当前最新的已经发布的流程 <br/>
     * 2. 建立流程实例并启动流程实例，流程实例在运行中 <br/>
     * 3. 获取到流程当前的所有节点信息 <br/>
     * 4. 按链表顺序遍历节点信息，依次执行节点的逻辑 <br/>
     * 5. 每个流程节点起一个节点实例，节点实例运行中 <br/>
     * 6. 所有节点实例执行完毕后，流程实例结束 <br/>
     */
    @Override
    public CompletableFuture<String> runRoleFlow(Long processDefinitionId) {

        ProcessDefinitionEntity processDefinitionEntity = processDefinitionService.getById(processDefinitionId) ;
        processDefinitionEntity.setRunCount(processDefinitionEntity.getRunCount() + 1) ;
        processDefinitionService.updateById(processDefinitionEntity) ;

        Map<String , String> orgSecret = secretsService.getSecretMapByOrgId(processDefinitionEntity.getOrgId()) ;

        ExecutionStrategyEnums errorStrategy = ExecutionStrategyEnums.fromCode(processDefinitionEntity.getErrorStrategy()) ;

        FlowEntity flowEntity = getLatestPublishedFlowByProcessDefinitionId(processDefinitionId);
        Assert.notNull(flowEntity, "未发布角色流程");

        // 这里输出chatThreadPool活动线程，是否线程已经满
        logThreadPoolStatus();
        detailedThreadPoolCheck();

        // 更新运行版本号
        flowEntity.setRunTimes(flowEntity.getRunTimes() == null ? 1 : flowEntity.getRunTimes() + 1);
        flowEntity.setRunUniqueNumber(IdUtil.nanoId(10));  // 记录最后执行的流程号

        update(flowEntity);

        FlowExecutionEntity flowExecutionEntity = new FlowExecutionEntity();
        flowExecutionEntity.setFlowId(flowEntity.getId()) ;
        flowExecutionEntity.setProcessDefinitionId(processDefinitionId);

        flowExecutionEntity.setRunUniqueNumber(flowEntity.getRunUniqueNumber()) ;

        flowExecutionEntity.setExecutionStatus(FlowExecutionStatus.EXECUTING.getCode());
        flowExecutionEntity.setRunTimes(processDefinitionEntity.getRunCount());
        flowExecutionEntity.setExecuteTime(new Date());

        // 创建流程实例
        flowExecutionEntity.setOrgId(processDefinitionEntity.getOrgId());
        flowExecutionEntity.setOperatorId(processDefinitionEntity.getOperatorId());
        flowExecutionEntity.setDepartmentId(processDefinitionEntity.getDepartmentId()) ;

        flowExecutionService.save(flowExecutionEntity);

        return CompletableFuture.supplyAsync(() -> {
            try {

                // 执行节点实例
                WorkflowRequestDto dto = JSONObject.parseObject(flowEntity.getFlowGraphJson() , WorkflowRequestDto.class) ;

                Map<String, Object> dataMap = new HashMap<>();
                dataMap.put("nodes", dto.getNodes());
                dataMap.put("edges", dto.getEdges());
                List<FlowNodeDto> parseNodes = LogicFlowParser.parseNodes(dataMap) ;

                // 跟数据库中的节点信息进行匹配，构建节点之间的关系
                List<FlowNodeEntity> flowNodes = flowNodeService.list(Wrappers.lambdaQuery(FlowNodeEntity.class).eq(FlowNodeEntity::getFlowId , flowEntity.getId())) ;
                for (FlowNodeDto flowNode : parseNodes) {
                    FlowNodeEntity flowNodeEntity = flowNodes.stream().filter(e -> e.getStepId().equals(flowNode.getId())).findFirst().orElse(null);
                    assert flowNodeEntity != null;
                    flowNode.setNodeId(flowNodeEntity.getId());
                }

                Map<String , Object> output = new HashMap<>();  // 流程全局参数
                StringBuilder outputContent = new StringBuilder() ; // 流程输出内容

                log.debug("执行任务.");
                CompletableFuture<Void> future = executeNodesSimplified(
                        parseNodes ,
                        flowExecutionEntity ,
                        output ,
                        outputContent ,
                        errorStrategy.getCode(),
                        orgSecret) ;

                future.whenComplete((v, e) -> {

                    // 判断之前的节点是否有异常，有则标记流程为失败
                    List<FlowNodeExecutionEntity> nodeExecutions = flowNodeExecutionService
                            .list(Wrappers.lambdaQuery(FlowNodeExecutionEntity.class)
                                    .eq(FlowNodeExecutionEntity::getFlowExecutionId, flowExecutionEntity.getId()));

                    // 默认设置为完成状态
                    flowExecutionEntity.setExecutionStatus(FlowExecutionStatus.COMPLETED.getCode());

                    // 检查是否有节点执行失败
                    for (FlowNodeExecutionEntity nodeExecution : nodeExecutions) {
                        if (nodeExecution.getExecutionStatus().equals(FlowExecutionStatus.ERROR.getCode())) {
                            // 发现错误节点，覆盖为错误状态
                            flowExecutionEntity.setExecutionStatus(FlowExecutionStatus.ERROR.getCode());
                            flowExecutionEntity.setExceptionNodeId(nodeExecution.getNodeId());

                            break;
                        }
                    }

                    // 判断是否暂停任务
                    if(flowExecutionEntity.getExecutionStatus().equals(FlowExecutionStatus.ERROR.getCode()) &&
                            errorStrategy.equals(ExecutionStrategyEnums.PAUSE_TASK)) {
                        IProcessDefinitionService processDefinitionService = SpringUtils.getBean(IProcessDefinitionService.class);
                        processDefinitionService.pauseJob(processDefinitionId);
                    }

                    // 设置完成时间并更新
                    flowExecutionEntity.setFinishTime(new Date());
                    flowExecutionService.update(flowExecutionEntity);

                });

                future.get() ;

                // 更新流程定义成功次数
                processDefinitionEntity.setSuccessCount(processDefinitionEntity.getSuccessCount() + 1) ;
                processDefinitionService.updateById(processDefinitionEntity) ;

                return null ; // taskInfo.getFullContent() ;
            } catch (Exception e) {
                log.error("流程执行失败", e);
                throw new RuntimeException("流程执行失败: " + e.getMessage(), e);
            }
        } , chatThreadPool);

    }

    /**
     * 打印线程池状态信息
     */
    private void logThreadPoolStatus() {
        try {
            int activeCount = chatThreadPool.getThreadPoolExecutor().getActiveCount();
            int poolSize = chatThreadPool.getThreadPoolExecutor().getPoolSize();
            long taskCount = chatThreadPool.getThreadPoolExecutor().getTaskCount();
            long completedTaskCount = chatThreadPool.getThreadPoolExecutor().getCompletedTaskCount();
            int queueSize = chatThreadPool.getThreadPoolExecutor().getQueue().size();
            int corePoolSize = chatThreadPool.getThreadPoolExecutor().getCorePoolSize();
            int maxPoolSize = chatThreadPool.getThreadPoolExecutor().getMaximumPoolSize();

            // 计算线程池使用率
            double poolUsage = (double) activeCount / maxPoolSize * 100;

            log.info("线程池状态监控 - 活动线程: {}/{}, 池大小: {}, 队列任务: {}, 总任务: {}, 已完成: {}, 使用率: {:.2f}%",
                    activeCount, maxPoolSize, poolSize, queueSize, taskCount, completedTaskCount, poolUsage);

            // 如果线程池接近满载，记录警告日志
            if (poolUsage > 80) {
                log.warn("线程池使用率过高，当前使用率: {:.2f}%", poolUsage);
            }

            // 如果队列积压严重，记录警告
            if (queueSize > 100) {
                log.warn("线程池队列积压严重，当前队列大小: {}", queueSize);
            }

        } catch (Exception e) {
            log.error("获取线程池状态失败", e);
        }
    }

    /**
     * 详细的线程池状态检查
     */
    private void detailedThreadPoolCheck() {
        try {
            ThreadPoolTaskExecutor executor = chatThreadPool;

            log.info("=== 线程池详细状态 ===");
            log.info("核心线程数: {}", executor.getCorePoolSize());
            log.info("最大线程数: {}", executor.getMaxPoolSize());
            log.info("当前池大小: {}", executor.getPoolSize());
            log.info("活动线程数: {}", executor.getActiveCount());
            log.info("队列容量: {}", executor.getThreadPoolExecutor().getQueue().remainingCapacity());
            log.info("队列大小: {}", executor.getThreadPoolExecutor().getQueue().size());
            log.info("总任务数: {}", executor.getThreadPoolExecutor().getTaskCount());
            log.info("已完成任务: {}", executor.getThreadPoolExecutor().getCompletedTaskCount());
            log.info("====================");

        } catch (Exception e) {
            log.error("详细线程池状态检查失败", e);
        }
    }

    @SneakyThrows
    public CompletableFuture<Void> executeNodesSimplified(
            List<FlowNodeDto> nodes,
            FlowExecutionEntity flowExecutionEntity,
            Map<String, Object> output,
            StringBuilder outputContent,
            int errorStrategy,
            Map<String, String> orgSecret) {

        if (nodes == null || nodes.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        // 保证 output 线程安全（如果并发写入较多，建议在调用方传入 ConcurrentHashMap）
        Map<String, Object> safeOutput = (output instanceof java.util.concurrent.ConcurrentMap)
                ? output
                : new java.util.concurrent.ConcurrentHashMap<>(output);

        // id -> node
        Map<String, FlowNodeDto> nodeMap = new HashMap<>();
        for (FlowNodeDto n : nodes) {
            nodeMap.put(n.getId(), n);
        }

        final int totalNodes = nodes.size();
        // 每个节点剩余未完成的前驱数
        Map<String, AtomicInteger> remainingPrev = new ConcurrentHashMap<>();
        // 每个节点被“激活”的入边数（来自前驱选择该节点的次数）
        Map<String, AtomicInteger> activeCount = new ConcurrentHashMap<>();
        // 用于记录节点最终的 level（深度），取所有前驱的 max(parentLevel + 1)
        Map<String, AtomicInteger> levelMap = new ConcurrentHashMap<>();
        // 已创建的节点 future，占位（确保每个节点只创建一次 future）
        Map<String, CompletableFuture<Void>> nodeFutures = new ConcurrentHashMap<>();

        // 初始化计数
        for (FlowNodeDto n : nodes) {
            int prevCnt = (n.getPrevNodes() == null) ? 0 : n.getPrevNodes().size();
            remainingPrev.put(n.getId(), new AtomicInteger(prevCnt));
            activeCount.put(n.getId(), new AtomicInteger(0));
            levelMap.put(n.getId(), new AtomicInteger(0));
        }

        AtomicInteger completedCount = new AtomicInteger(0);
        CompletableFuture<Void> allDone = new CompletableFuture<>();
        AtomicInteger orderCounter = new AtomicInteger(1);

        // 跳过集合（已被标记为 skip 的节点 id）
        Set<String> skipped = ConcurrentHashMap.newKeySet();

        java.util.function.Consumer<Throwable> failAll = (err) -> {
            try {
                // 将流程执行状态设置为 ERROR 并写库，记录 finishTime
                flowExecutionEntity.setExecutionStatus(FlowExecutionStatus.ERROR.getCode());
                flowExecutionEntity.setFinishTime(new Date());
                // 你可以把 err 的信息也写到 flowExecutionEntity 的某个字段上（若有）
                flowExecutionService.update(flowExecutionEntity);
            } catch (Exception ex) {
                log.error("Failed to persist flowExecution ERROR state", ex);
            }
            if (!allDone.isDone()) allDone.completeExceptionally(err);
        };

        // 选择子节点（condition 节点有分支选择逻辑）
        BiFunction<FlowNodeDto, Map<String, FlowNodeDto>, List<FlowNodeDto>> chooseChildren =
                (node, nodeMapRef) -> {
                    List<FlowNodeDto.FlowEdgeDto> edges = node.getOutgoingEdges();
                    List<FlowNodeDto> chosen = new ArrayList<>();
                    if (edges == null || edges.isEmpty()) return chosen;

                    boolean isCondition = "condition".equalsIgnoreCase(node.getType());
                    if (isCondition) {
                        Object branchObj = safeOutput.get(node.getStepName() + ".branch_map");
                        String branchId = null;
                        if (branchObj instanceof Map) {
                            Object idObj = ((Map) branchObj).get("branch_id");
                            if (idObj != null) branchId = String.valueOf(idObj);
                        }
                        if (branchId != null) {
                            for (FlowNodeDto.FlowEdgeDto edge : edges) {
                                String srcAnchor = edge.getSourceAnchorId();
                                if (srcAnchor != null && srcAnchor.contains(branchId)) {
                                    chosen.add(edge.getTargetNode());
                                }
                            }
                        }
                        // 兜底：若没有匹配出边则走所有出边
                        if (chosen.isEmpty()) {
                            for (FlowNodeDto.FlowEdgeDto edge : edges) {
                                chosen.add(edge.getTargetNode());
                            }
                        }
                    } else {
                        for (FlowNodeDto.FlowEdgeDto edge : edges) {
                            chosen.add(edge.getTargetNode());
                        }
                    }
                    return chosen;
                };

        // 使用 AtomicReference 做 self-reference，避免在 lambda 中引用未初始化变量
        final AtomicReference<BiFunction<FlowNodeDto, Integer, CompletableFuture<Void>>> processNodeRef =
                new AtomicReference<>();

        // 处理一个节点（保证每个节点只会创建一次 future），实现放入 processNodeRef
        processNodeRef.set((node, parentLevel) -> {
            return nodeFutures.computeIfAbsent(node.getId(), id -> {
                // set a reasonable level (may be updated by multiple parents before scheduling)
                if (parentLevel != null) {
                    levelMap.get(id).updateAndGet(prev -> Math.max(prev, parentLevel));
                }

                int currentOrder = orderCounter.getAndIncrement();
                int nodeLevel = Math.max(1, levelMap.get(id).get());

                CompletableFuture<Void> execFuture = executeFlowNode(
                        node,
                        flowExecutionEntity,
                        safeOutput,
                        outputContent,
                        currentOrder,
                        nodeLevel,
                        nodes,
                        errorStrategy ,
                        orgSecret
                );

                execFuture.whenComplete((v, ex) -> {
                    if (ex != null) {
                        failAll.accept(ex);
                        return;
                    }

                    // 执行完成计数（真实执行完成）
                    int finished = completedCount.incrementAndGet();
                    if (finished == totalNodes) {
                        if (!allDone.isDone()) allDone.complete(null);
                    }

                    // 决定出边（condition 节点选择逻辑）
                    List<FlowNodeDto> chosenChildren = chooseChildren.apply(node, nodeMap);

                    List<FlowNodeDto.FlowEdgeDto> outgoing = node.getOutgoingEdges();
                    if (outgoing != null) {
                        // 对每个 outgoing 的目标做两件事：
                        // 1) 若被 chosen，则 activeCount++；
                        // 2) 不论是否 chosen，都对 remainingPrev--，若降到0则判断 activeCount 决定是否调度或 skip
                        for (FlowNodeDto.FlowEdgeDto edge : outgoing) {
                            FlowNodeDto child = edge.getTargetNode();
                            if (child == null || child.getId() == null) continue;
                            String childId = child.getId();

                            boolean isChosen = chosenChildren.stream().anyMatch(c -> Objects.equals(c.getId(), childId));

                            // 如果该 child 被 chosen，则增加 activeCount
                            if (isChosen) {
                                activeCount.get(childId).incrementAndGet();
                            }

                            // 减少 child 的 remaining prev（来自该父节点的报告）
                            AtomicInteger rem = remainingPrev.get(childId);
                            if (rem != null) {
                                int left = rem.decrementAndGet();
                                // 更新 child 的 level 值为当前 node 的 level + 1 的最大值
                                levelMap.get(childId).updateAndGet(prev -> Math.max(prev, nodeLevel + 1));

                                if (left == 0) {
                                    // 所有前驱都已完成（或被处理），只有当 activeCount > 0 或者 child 本身没有任何前驱被视为 start 时，才真正调度
                                    if (activeCount.get(childId).get() > 0 || (child.getPrevNodes() == null || child.getPrevNodes().isEmpty())) {
                                        // 使用最新 level 值调度
                                        int chosenLevel = Math.max(1, levelMap.get(childId).get());
                                        FlowNodeDto childNode = nodeMap.get(childId);
                                        if (childNode != null) {
                                            // 递归调用自身，通过引用拿到函数
                                            processNodeRef.get().apply(childNode, chosenLevel);
                                        }
                                    } else {
                                        // 没有前驱激活该 child -> 整个子树被跳过，需要把 child 及其下游按 skip 传播处理
                                        // 使用栈/队列避免递归
                                        Deque<String> stack = new ArrayDeque<>();
                                        stack.push(childId);
                                        while (!stack.isEmpty()) {
                                            String nid = stack.pop();
                                            if (nid == null) continue;
                                            // 如果已经被 skip 或 已有真实执行的 future 完成，则跳过
                                            if (!skipped.add(nid)) {
                                                continue;
                                            }
                                            // 如果该节点已经存在且处于执行中（future 存在但未完成），则不要强行 skip 它
                                            CompletableFuture<Void> existingFuture = nodeFutures.get(nid);
                                            if (existingFuture != null && !existingFuture.isDone()) {
                                                // 该节点正被执行中，不应跳过；把 skipped 标记撤回
                                                skipped.remove(nid);
                                                continue;
                                            }

                                            // 标记为已完成（跳过）
                                            int finishedSkip = completedCount.incrementAndGet();
                                            if (finishedSkip == totalNodes) {
                                                if (!allDone.isDone()) allDone.complete(null);
                                            }

                                            // 放一个已完成的 future 占位，避免后续重复调度
                                            nodeFutures.putIfAbsent(nid, CompletableFuture.completedFuture(null));

                                            // 对它的所有 outgoing child 做 remainingPrev--（因为这个节点不会真实执行其出边）
                                            FlowNodeDto nodeObj = nodeMap.get(nid);
                                            if (nodeObj == null) continue;
                                            List<FlowNodeDto.FlowEdgeDto> outs = nodeObj.getOutgoingEdges();
                                            if (outs == null) continue;
                                            for (FlowNodeDto.FlowEdgeDto e2 : outs) {
                                                FlowNodeDto child2 = e2.getTargetNode();
                                                if (child2 == null || child2.getId() == null) continue;
                                                String child2Id = child2.getId();
                                                AtomicInteger remChild = remainingPrev.get(child2Id);
                                                if (remChild == null) continue;
                                                int left2 = remChild.decrementAndGet();
                                                // update level 值为跳过路径的 parent level + 1（兜底处理）
                                                levelMap.get(child2Id).updateAndGet(prev -> Math.max(prev, levelMap.get(nid).get() + 1));
                                                if (left2 == 0) {
                                                    // 若 child2 有被激活过，调度；否则继续 skip
                                                    if (activeCount.get(child2Id).get() > 0 || (child2.getPrevNodes() == null || child2.getPrevNodes().isEmpty())) {
                                                        int chosenLevel2 = Math.max(1, levelMap.get(child2Id).get());
                                                        FlowNodeDto childNode2 = nodeMap.get(child2Id);
                                                        if (childNode2 != null) {
                                                            processNodeRef.get().apply(childNode2, chosenLevel2);
                                                        }
                                                    } else {
                                                        stack.push(child2Id);
                                                    }
                                                }
                                            }
                                        } // end skip propagation stack
                                    }
                                }
                            }
                        }
                    }
                });

                return execFuture;
            });
        });

        // 启动：对所有入度为 0 的节点进行处理（start nodes）
        for (FlowNodeDto n : nodes) {
            if (n.getPrevNodes() == null || n.getPrevNodes().isEmpty()) {
                // mark remainingPrev already 0, activeCount force 1 to ensure execution
                activeCount.get(n.getId()).incrementAndGet();
                levelMap.get(n.getId()).set(1);
                processNodeRef.get().apply(n, 1);
            }
        }

        // 边界：如果没有任何 start 节点（环或配置异常），返回异常
        boolean hasStart = nodes.stream().anyMatch(n -> n.getPrevNodes() == null || n.getPrevNodes().isEmpty());
        if (!hasStart) {
            allDone.completeExceptionally(new RuntimeException("流程没有起始节点（可能存在环或配置错误）"));
        }

        return allDone;
    }

    /**
     * 执行节点实例
     *
     * @return
     */
    private CompletableFuture<Void> executeFlowNode(
            FlowNodeDto node,
            FlowExecutionEntity flowExecutionEntity,
            Map<String,Object> output,
            StringBuilder outputContent,
            int count,
            int level,
            List<FlowNodeDto> nodes,
            int errorStrategy,
            Map<String, String> orgSecret) {

        return CompletableFuture.runAsync(() -> {
            FlowNodeExecutionEntity flowNodeExecutionEntity = new FlowNodeExecutionEntity();
            flowNodeExecutionEntity.setFlowExecutionId(flowExecutionEntity.getId());
            flowNodeExecutionEntity.setExecutionStatus(FlowExecutionStatus.EXECUTING.getCode());
            flowNodeExecutionEntity.setExecuteTime(new Date());
            flowNodeExecutionEntity.setNodeId(node.getNodeId());
            flowNodeExecutionEntity.setStepId(node.getId());
            flowNodeExecutionEntity.setExecutionOrder(count);
            flowNodeExecutionEntity.setExecutionDepth(level);
            flowNodeExecutionEntity.setNodeType(node.getType());
            ObjectMapper objectMapper = new ObjectMapper();
            try {

                // 立刻检查中断
                if (Thread.currentThread().isInterrupted()) {
                    throw new InterruptedException("节点执行前检测到中断");
                }

                flowNodeExecutionEntity.setProperties(objectMapper.writeValueAsString(node.getProperties()));
            } catch (JsonProcessingException | InterruptedException e) {
                log.error("executeFlowNode Error converting properties to JSON: {}" , e.getMessage());
                flowNodeExecutionEntity.setProperties(null);
            }

            // 初次保存为 EXECUTING
            flowNodeExecutionService.save(flowNodeExecutionEntity);

            // 执行节点
            outputContent.setLength(0);
            AbstractFlowNode abstractFlowNode = SpringUtil.getBean(FLOW_STEP_NODE + node.getType());
            abstractFlowNode.setFlowNodes(nodes);
            CompletableFuture<Void> future = abstractFlowNode.executeNode(
                    node,
                    flowExecutionEntity,
                    flowNodeExecutionEntity,
                    output,
                    outputContent ,
                    orgSecret);

            boolean b = errorStrategy == ExecutionStrategyEnums.STOP_FLOW.getCode() || errorStrategy == ExecutionStrategyEnums.PAUSE_TASK.getCode();

            try {
                future.get(); // 等待节点执行结束（成功或抛异常）
                // 成功路径
                flowNodeExecutionEntity.setExecuteInfo(outputContent.toString());
                flowNodeExecutionEntity.setExecutionStatus(FlowExecutionStatus.COMPLETED.getCode());
                flowNodeExecutionEntity.setFinishTime(new Date());
                flowNodeExecutionEntity.setProgress(100);
                flowNodeExecutionService.update(flowNodeExecutionEntity);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                String err = stringifyThrowable(ie);
                flowNodeExecutionEntity.setExecuteInfo(err);
                flowNodeExecutionEntity.setExecutionStatus(FlowExecutionStatus.ERROR.getCode());
                flowNodeExecutionEntity.setFinishTime(new Date());
                flowNodeExecutionEntity.setProgress(0);
                flowNodeExecutionService.update(flowNodeExecutionEntity);

                if(b){
                    throw new RuntimeException("流程执行异常：" + err);
                }

            } catch (ExecutionException ee) {
                Throwable cause = ee.getCause() != null ? ee.getCause() : ee;
                String err = stringifyThrowable(cause);
                flowNodeExecutionEntity.setExecuteInfo(err);
                flowNodeExecutionEntity.setExecutionStatus(FlowExecutionStatus.ERROR.getCode());
                flowNodeExecutionEntity.setFinishTime(new Date());
                flowNodeExecutionEntity.setProgress(0);
                flowNodeExecutionService.update(flowNodeExecutionEntity);

                if(b){
                    throw new RuntimeException("流程执行异常：" + err);
                }

            }
        }, chatThreadPool);
    }

    private String stringifyThrowable(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }

    @SneakyThrows
    public CompletableFuture<Void> executeNodesByTraversal(
            List<FlowNodeDto> nodes,
            FlowExecutionEntity flowExecutionEntity,
            Map<String, Object> output,
            StringBuilder outputContent,
            int errorStrategy,
            Map<String ,  String> orgSecret) {

        if (nodes == null || nodes.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        Queue<FlowNodeDto> queue = new LinkedList<>();
        Set<String> queued = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (FlowNodeDto n : nodes) {
            if (n.getPrevNodes() == null || n.getPrevNodes().isEmpty()) {
                queue.offer(n);
                queued.add(n.getId());
            }
        }

        AtomicInteger count = new AtomicInteger(1);
        AtomicInteger level = new AtomicInteger(1);

        // 递归异步处理队列
        return processQueueAsync(queue,
                queued,
                visited,
                nodes,
                flowExecutionEntity,
                output,
                outputContent,
                count,
                level ,
                errorStrategy,
                orgSecret);
    }

    private CompletableFuture<Void> processQueueAsync(
            Queue<FlowNodeDto> queue,
            Set<String> queued,
            Set<String> visited,
            List<FlowNodeDto> nodes,
            FlowExecutionEntity flowExecutionEntity,
            Map<String, Object> output,
            StringBuilder outputContent,
            AtomicInteger count,
            AtomicInteger level ,
            int errorStrategy ,
            Map<String , String> orgSecret) {

        if (queue.isEmpty()) {
            return CompletableFuture.completedFuture(null);
        }

        FlowNodeDto currentNode = queue.poll();
        queued.remove(currentNode.getId());

        if (visited.contains(currentNode.getId())) {
            // 已处理，继续下一个
            return processQueueAsync(queue,
                    queued,
                    visited,
                    nodes,
                    flowExecutionEntity,
                    output,
                    outputContent,
                    count,
                    level ,
                    errorStrategy ,
                    orgSecret);
        }

        log.debug("node--{}--level--{}--->>>>>>>>start", count.get(), level.get());
        log.debug("Processing node stepName = {} , type = {} , id = {}", currentNode.getStepName(), currentNode.getType(), currentNode.getId());
        log.debug("node--{}--level--{}--->>>>>>>>end", count.get(), level.get());
        currentNode.setLastNode(false);

        // 只把明显的终端节点标记为 isLastNode
        List<FlowNodeDto.FlowEdgeDto> outgoing = currentNode.getOutgoingEdges();
        boolean isLast = true;
        if (outgoing != null && !outgoing.isEmpty()) {
            boolean anyUnvisitedTarget = outgoing.stream()
                    .map(FlowNodeDto.FlowEdgeDto::getTargetNode)
                    .anyMatch(t -> t != null && !visited.contains(t.getId()));
            isLast = !anyUnvisitedTarget;
        }
        currentNode.setLastNode(isLast);

        // executeFlowNode 返回 CompletableFuture<Void>（已经由它自己在 chatThreadPool 异步执行）
        CompletableFuture<Void> execFuture = executeFlowNode(
                currentNode,
                flowExecutionEntity,
                output,
                outputContent,
                count.getAndIncrement(),
                level.get(),
                nodes ,
                errorStrategy,
                orgSecret);

        return execFuture.thenCompose(v -> {
            // 标记已访问
            visited.add(currentNode.getId());

            // 计算下游候选节点（保持原有 condition 分支逻辑）
            List<FlowNodeDto.FlowEdgeDto> edges = currentNode.getOutgoingEdges();
            if (edges != null && !edges.isEmpty()) {
                List<FlowNodeDto> possibleTargets = new ArrayList<>();
                boolean isCondition = "condition".equalsIgnoreCase(currentNode.getType());
                if (isCondition) {
                    Object branchObj = output.get(currentNode.getStepName() + ".branch_map");
                    String branchId = null;
                    if (branchObj instanceof Map) {
                        Object idObj = ((Map) branchObj).get("branch_id");
                        if (idObj != null) branchId = String.valueOf(idObj);
                    }
                    if (branchId != null) {
                        for (FlowNodeDto.FlowEdgeDto edge : edges) {
                            String srcAnchor = edge.getSourceAnchorId();
                            if (srcAnchor != null && srcAnchor.contains(branchId)) {
                                possibleTargets.add(edge.getTargetNode());
                            }
                        }
                        if (possibleTargets.isEmpty()) {
                            log.warn("Condition node {} produced branchId={} but no outgoing edge matched it. Falling back to all outgoing edges.", currentNode.getId(), branchId);
                            for (FlowNodeDto.FlowEdgeDto edge : edges) {
                                possibleTargets.add(edge.getTargetNode());
                            }
                        }
                    } else {
                        log.warn("Condition node {} did not produce branch_map in output. Falling back to all outgoing edges.", currentNode.getId());
                        for (FlowNodeDto.FlowEdgeDto edge : edges) {
                            possibleTargets.add(edge.getTargetNode());
                        }
                    }
                } else {
                    for (FlowNodeDto.FlowEdgeDto edge : edges) {
                        possibleTargets.add(edge.getTargetNode());
                    }
                }

                // 对每个目标节点，只有当它的所有前驱都已 visited 时才加入队列（从而实现 join 行为）
                for (FlowNodeDto next : possibleTargets) {
                    if (next == null) continue;
                    if (visited.contains(next.getId()) || queued.contains(next.getId())) continue;
                    if (allPredecessorsVisited(next, visited)) {
                        queue.offer(next);
                        queued.add(next.getId());
                    } else {
                        // 若尚有前驱未完成，不立即入队，等待其它前驱完成时会再次检查并入队
                    }
                }
            }

            // 层级递增策略：这里保守处理，每处理一个节点 increment level（你也可以按层级批量处理）
            level.incrementAndGet();

            // 继续处理队列
            return processQueueAsync(queue,
                    queued,
                    visited,
                    nodes,
                    flowExecutionEntity,
                    output,
                    outputContent,
                    count,
                    level ,
                    errorStrategy ,
                    orgSecret);
        });
    }

    /**
     * 判断目标节点的所有前驱是否都已被 visited。
     * 兼容 prevNodes 中元素为 FlowNodeDto、String 或其他可转换为 id 的类型（尽量稳健）。
     */
    private boolean allPredecessorsVisited(FlowNodeDto node, Set<String> visited) {
        List<?> prevs = node.getPrevNodes();
        if (prevs == null || prevs.isEmpty()) return true;
        for (Object p : prevs) {
            if (p == null) continue;
            String id = null;
            if (p instanceof FlowNodeDto) {
                id = ((FlowNodeDto) p).getId();
            } else if (p instanceof Map) {
                // 若 prevNodes 存放的是 map（极端情况），尝试取 id 字段
                Object idObj = ((Map) p).get("id");
                if (idObj != null) id = String.valueOf(idObj);
            } else {
                id = String.valueOf(p);
            }
            if (id == null) continue;
            if (!visited.contains(id)) return false;
        }
        return true;
    }

    /**
     * 每次保存之后，版本号加1
     *
     * @param processDefinitionId
     * @param flowDto
     * @return
     */
    @Override
    @Transactional
    public FlowEntity saveRoleFlow(Long processDefinitionId, WorkflowRequestDto flowDto) {
        // 查询角色的流程
        LambdaQueryWrapper<FlowEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FlowEntity::getProcessDefinitionId, processDefinitionId);
        FlowEntity flowEntity = getOne(queryWrapper);

        if (flowEntity == null) {
            flowEntity = new FlowEntity();
            flowEntity.setProcessDefinitionId(processDefinitionId);
            flowEntity.setLockVersion(0);
            flowEntity.setPublishStatus(PublishStatus.UNPUBLISHED.getCode());
            flowEntity.setUpdateTime(new Date());
        }

        flowEntity.setFlowGraphJson(JSONObject.toJSONString(flowDto)); // 假设 WorkflowRequestDto 有 toJson 方法

        saveOrUpdate(flowEntity);

        // 保存节点信息
        Map<String, Object> data = new HashMap<>();
        data.put("nodes", flowDto.getNodes());
        data.put("edges", flowDto.getEdges());

        List<FlowNodeDto> nodes = LogicFlowParser.parseNodes(data);

        log.debug("链表打印.");
        NodePrinter.printNodesAsLinkedList(nodes);

        // 转换成Entity保存到数据库中
        Long flowId = flowEntity.getId();

        List<FlowNodeEntity> nodeEntities = nodes.stream()
                .map(nodeDto -> {
                    FlowNodeEntity nodeEntity = nodeDto.toEntity();
                    nodeEntity.setFlowId(flowId); // 设置所属工作流的 ID
                    return nodeEntity;
                })
                .toList();

        // 先删除该工作流下的所有旧节点信息
        LambdaQueryWrapper<FlowNodeEntity> deleteWrapper = Wrappers.lambdaQuery();
        deleteWrapper.eq(FlowNodeEntity::getFlowId, flowId);
        flowNodeService.remove(deleteWrapper);

        // 保存节点信息
        flowNodeService.saveBatch(nodeEntities);

        return flowEntity ;
    }

    /**
     * 发布工作流
     * @param flowId
     */
    @Override
    @Transactional
    public void publishFlow(Long flowId) {
        // 查询角色的流程
        LambdaQueryWrapper<FlowEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FlowEntity::getId, flowId);
        FlowEntity flowEntity = getOne(queryWrapper);

        if (Objects.nonNull(flowEntity)) {
            // 版本号加1
            flowEntity.setPublishStatus(PublishStatus.PUBLISHED.getCode());
            update(flowEntity);
        }
    }

    /**
     * 获取指定角色最新版本的已发布流程
     * @param processDefinitionId 角色ID
     * @return 最新版本的已发布流程实体，如果不存在则返回 null
     */
    @Override
    public FlowEntity getLatestPublishedFlowByProcessDefinitionId(Long processDefinitionId) {
        LambdaQueryWrapper<FlowEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FlowEntity::getProcessDefinitionId, processDefinitionId)
                .eq(FlowEntity::getPublishStatus, PublishStatus.PUBLISHED.getCode())
                .last("LIMIT 1");
        return getOne(queryWrapper);
    }

    /**
     * 获取指定角色的未发布流程
     * @param processDefinitionId 角色ID
     * @return 未发布流程实体，如果不存在则返回 null
     */
    @Override
    public FlowEntity getUnpublishedFlowByProcessDefinitionId(Long processDefinitionId) {
        LambdaQueryWrapper<FlowEntity> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(FlowEntity::getProcessDefinitionId, processDefinitionId)
                .eq(FlowEntity::getPublishStatus, PublishStatus.UNPUBLISHED.getCode());
        return getOne(queryWrapper);
    }

    /**
     * 获取指定角色最新版本的流程
     * @param processDefinitionId 角色ID
     * @return 最新版本的已发布流程实体，如果不存在则返回 null
     */
    @Override
    public FlowDto getLatestFlowByProcessDefinitionId(Long processDefinitionId) {
        LambdaQueryWrapper<FlowEntity> queryWrapper = new LambdaQueryWrapper<>();

        queryWrapper.eq(FlowEntity::getProcessDefinitionId, processDefinitionId)
                .last("LIMIT 1");
        FlowEntity flowEntity = getOne(queryWrapper);

        if(flowEntity == null){
            return FlowDto.empty();
        }

        WorkflowRequestDto dto = JSONObject.parseObject(flowEntity.getFlowGraphJson() , WorkflowRequestDto.class) ;

        FlowDto flowDto = new FlowDto() ;
        BeanUtils.copyProperties(flowEntity , flowDto);
        flowDto.setFlowGraphJson(dto);

        List<FlowNodeDto> flowNodeDtos = getFlowNodeDtosByFlowId(flowEntity.getId()) ;
        flowDto.setNodes(flowNodeDtos);

        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("nodes", dto.getNodes());
        dataMap.put("edges", dto.getEdges());
        List<FlowNodeDto> parseNodes = LogicFlowParser.parseNodes(dataMap) ;

        log.debug("链表打印:");
        NodePrinter.printNodesAsLinkedList(parseNodes);

        return flowDto ;
    }

    /**
     * 查询指定工作流 ID 下的所有节点实体，并将其转换为 DTO 列表
     * @param flowId 工作流 ID
     * @return 转换后的 FlowNodeDto 列表
     */
    public List<FlowNodeDto> getFlowNodeDtosByFlowId(Long flowId) {
        // 查询指定工作流 ID 下的所有节点实体
        LambdaQueryWrapper<FlowNodeEntity> queryWrapper = Wrappers.lambdaQuery();
        queryWrapper.eq(FlowNodeEntity::getFlowId, flowId);
        List<FlowNodeEntity> nodeEntities = flowNodeService.list(queryWrapper);

        // 将实体列表转换为 DTO 列表
        return nodeEntities.stream()
                .map(FlowNodeDto::fromEntity)
                .toList();
    }

    /**
     * 尝试运行工作流
     *
     * @param processDefinitionId
     * @return
     */
    @SneakyThrows
    @Override
    public CompletableFuture<String> tryRun(Long processDefinitionId) {
        log.debug("尝试运行工作流: {}", processDefinitionId);
        return runRoleFlow(processDefinitionId);
    }

    @Override
    public LastExecuteFlowDto getLastExecutedFlow(Long processDefinitionId, Long executeId) {

        LastExecuteFlowDto lastExecuteFlowDto = new LastExecuteFlowDto() ;

        FlowEntity lastExecutedFlow = null ;
        FlowExecutionEntity flowExecutionEntity = null ;
        if(executeId != null) {
            flowExecutionEntity = flowExecutionService.getById(executeId) ;
        }else{
            lastExecutedFlow = getLatestPublishedFlowByProcessDefinitionId(processDefinitionId) ;

            if(lastExecutedFlow == null){
                lastExecuteFlowDto.setStatus(FlowExecutionStatus.NOT_RUN.getCode());
                return lastExecuteFlowDto ;
            }

            String runUniqueNumber = lastExecutedFlow.getRunUniqueNumber() ;

            LambdaQueryWrapper<FlowExecutionEntity> queryWrapper = new LambdaQueryWrapper<>();
            queryWrapper.eq(FlowExecutionEntity::getRunUniqueNumber, runUniqueNumber) ;
            queryWrapper.eq(FlowExecutionEntity::getFlowId, lastExecutedFlow.getId()) ;

            // 获取到流程实例
            flowExecutionEntity = flowExecutionService.getOne(queryWrapper) ;
        }


        if(flowExecutionEntity == null){
            lastExecuteFlowDto.setStatus(FlowExecutionStatus.NOT_RUN.getCode());
            return lastExecuteFlowDto ;
        }

        lastExecuteFlowDto.setStatus(flowExecutionEntity.getExecutionStatus());
        lastExecuteFlowDto.setExecuteInstanceId(flowExecutionEntity.getId());

        // 获取到实例的节点
        LambdaQueryWrapper<FlowNodeExecutionEntity> queryNodeWrapper = new LambdaQueryWrapper<>();
        queryNodeWrapper.eq(FlowNodeExecutionEntity::getFlowExecutionId, flowExecutionEntity.getId()) ;
        List<FlowNodeExecutionEntity> flowNodeExecutionEntities = flowNodeExecutionService.list(queryNodeWrapper) ;

        List<FlowNodeExecutionDto> flowNodeExecutionDtos =  flowNodeExecutionEntities.stream().map(item -> {
            FlowNodeExecutionDto dto = new FlowNodeExecutionDto() ;
            BeanUtils.copyProperties(item , dto) ;

            JSONObject jsonNode = JSON.parseObject(item.getProperties()) ;
            dto.setNode(jsonNode) ;

            return dto ;
        }).toList();

        lastExecuteFlowDto.setFlowNode(flowNodeExecutionDtos);

        return lastExecuteFlowDto ;
    }

}