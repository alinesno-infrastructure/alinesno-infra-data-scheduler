package com.alinesno.infra.data.scheduler.trigger.service;

import com.alinesno.infra.data.scheduler.trigger.bean.BuildTask;
import com.alinesno.infra.data.scheduler.trigger.bean.TaskState;
import com.alinesno.infra.data.scheduler.trigger.entity.BuildRecordEntity;
import com.alinesno.infra.data.scheduler.trigger.entity.JobEntity;
import com.alinesno.infra.data.scheduler.trigger.resolver.BuildNumberAllocator;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * BuildQueueService: 支持任务状态查询与取消（兼容现有 IBuildRecordService 接口）
 */
@Slf4j
@Service
public class BuildQueueService {

    private static final int DEFAULT_OUT_TIME = 60; // minutes
    private final ScheduledExecutorService timeoutScheduler = Executors.newSingleThreadScheduledExecutor();

    // queue stores BuildTask
    private final BlockingQueue<BuildTask> queue = new LinkedBlockingQueue<>();

    private final ThreadPoolExecutor workers;
    private final ExecutorService consumer = Executors.newSingleThreadExecutor();
    private final Future<?> consumerFuture;
    private final ProcessExecutionService processExecutionService;

    private final IBuildRecordService buildRecordService;
    private final BuildNumberAllocator buildNumberAllocator;

    // track running/completed tasks
    private final ConcurrentHashMap<String, Future<?>> runningFutures = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BuildTask> runningTasks = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, BuildTask> allTasks = new ConcurrentHashMap<>(); // history incl queued/running/completed

    public BuildQueueService(@Value("${alinesno.trigger.default.concurrent:3}") int defaultConcurrency,
                             ProcessExecutionService processExecutionService,
                             IBuildRecordService buildRecordService,
                             BuildNumberAllocator buildNumberAllocator) {

        this.processExecutionService = processExecutionService;

        this.buildRecordService = buildRecordService;
        this.buildNumberAllocator = buildNumberAllocator;

        if (defaultConcurrency < 1) {
            throw new IllegalArgumentException("任务并发数需要 >= 1");
        }

        this.workers = new ThreadPoolExecutor(
                defaultConcurrency,      // corePoolSize
                defaultConcurrency,      // maximumPoolSize
                60L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(1000),  // 队列长度
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()  // 任务满时拒绝并抛出异常
        );
        this.workers.allowCoreThreadTimeOut(false);

        consumerFuture = consumer.submit(this::consumerLoop);
    }

    /**
     * 服务启动时从数据库恢复未完成的任务
     */
    @PostConstruct
    public void recoverUnfinishedTasks() {
        try {
            log.info("开始恢复未完成任务...");

            // 1. 从数据库查询未完成的任务（QUEUED/RUNNING）
            List<BuildRecordEntity> unfinishedRecords = buildRecordService.listUnfinishedRecords();
            if (unfinishedRecords == null || unfinishedRecords.isEmpty()) {
                log.info("未发现未完成任务，恢复结束");
                return;
            }

            log.info("发现 {} 条未完成任务，开始处理...", unfinishedRecords.size());

            // 2. 遍历记录，处理恢复逻辑
            for (BuildRecordEntity record : unfinishedRecords) {
                String status = record.getStatus();
                Long recordId = record.getId();

                // 2.1 处理 RUNNING 状态：重启后无法继续执行，标记为失败
                if (TaskState.RUNNING.name().equals(status)) {
                    try {
                        buildRecordService.markFailed(recordId, "服务重启导致任务中断");
                        log.warn("任务 [recordId={}, jobId={}] 原状态为 RUNNING，已标记为失败",
                                recordId, record.getJobId());
                    } catch (Exception e) {
                        log.error("标记 RUNNING 任务为失败失败", e);
                    }
                    continue;
                }

                // 2.2 处理 QUEUED 状态：恢复到内存队列
                if (TaskState.QUEUED.name().equals(status)) {
                    // 创建 BuildTask 并设置必要字段
                    BuildTask task = new BuildTask();
                    task.setId(recordId.toString()); // 使用数据库记录ID作为任务ID
                    task.setJobId(record.getJobId());
                    task.setJobName(record.getDisplayName()); // 或从 Job 表查询
                    task.setBuildNumber(record.getBuildNumber());
                    task.setRecordId(recordId);
                    task.setState(TaskState.QUEUED);
                    task.setEnqueuedAt(record.getEnqueuedAt() != null ?
                            record.getEnqueuedAt() : LocalDateTime.now());

                    // 添加到内存队列和任务集合
                    allTasks.put(task.getId(), task);
                    boolean isEnqueued = queue.offer(task);

                    if (isEnqueued) {
                        log.info("恢复任务成功：recordId={}, jobId={}, buildNumber={}",
                                recordId, task.getJobId(), task.getBuildNumber());
                    } else {
                        // 队列已满，标记为失败
                        task.setState(TaskState.FAILED);
                        task.setMessage("恢复失败：内存队列已满");
                        allTasks.put(task.getId(), task);
                        buildRecordService.markFailed(recordId, "恢复失败：内存队列已满");
                        log.error("恢复任务失败（队列已满）：recordId={}", recordId);
                    }
                }
            }

            log.info("未完成任务恢复完成");

        } catch (Exception e) {
            log.error("恢复未完成任务时发生异常", e);
        }
    }

    // 直接入队任务实例（用于测试或手动入队）
    public void enqueue(BuildTask task) {
        if (task == null) throw new IllegalArgumentException("task null");

        task.setState(TaskState.QUEUED);
        task.setEnqueuedAt(LocalDateTime.now());
        allTasks.put(task.getId(), task);

        boolean offered = queue.offer(task);
        if (!offered) {
            task.setState(TaskState.FAILED);
            task.setMessage("任务入队失败：队列已满");
            allTasks.put(task.getId(), task);
            if (task.getRecordId() != null) {
                try {
                    buildRecordService.markFailed(task.getRecordId(), "任务入队失败：队列已满");
                } catch (Exception ex) {
                    log.warn("markFailed failed", ex);
                }
            }
        }
    }

    // enqueue(JobEntity job) 修改：创建数据库记录后入队
    public BuildTask enqueue(JobEntity job, Long triggerId) {
        if (job == null) {
            throw new IllegalArgumentException("job null");
        }

        BuildTask t = new BuildTask(job.getId(), job.getName() , job.getProcessId());

        // allocate build number
        int buildNumber = buildNumberAllocator.nextBuildNumber(job.getId());
        t.setBuildNumber(buildNumber);

        // create record
        BuildRecordEntity rec = null;
        try {
            rec = buildRecordService.createQueuedRecord(job.getId(), triggerId, job.getName(), buildNumber , job.getProcessId());
            if (rec != null) {
                t.setRecordId(rec.getId());
            }
        } catch (Exception ex) {
            log.warn("createQueuedRecord failed", ex);
            // proceed: still enqueue but recordId may be null
        }

        enqueue(t);
        return t;
    }

    private void consumerLoop() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                BuildTask task = queue.take(); // blocks
                if (task == null) {
                    continue;
                }

                // canceller 占位，供 runnable finally 中取消
                final AtomicReference<ScheduledFuture<?>> cancellerRef = new AtomicReference<>();

                Runnable runnable = () -> {
                    // 在真正开始时设置 startedAt 并加入 runningTasks
                    task.setState(TaskState.RUNNING);
                    task.setStartedAt(LocalDateTime.now());
                    runningTasks.put(task.getId(), task);

                    try {
                        if (task.getRecordId() != null) {
                            try {
                                buildRecordService.markRunning(task.getRecordId());
                            } catch (Exception e) {
                                log.warn("markRunning failed for recordId={}", task.getRecordId(), e);
                            }
                        }

                        log.debug("开始构建任务 {}:{} 任务ID={} 时间: {}", task.getJobId(), task.getJobName(), task.getId(), Instant.now());

//                        // 模拟耗时(10秒+随机数)
//                        Thread.sleep(5000);

                        // 真正的业务执行：把 jobId 当成 processId（如不是请改为 task.getProcessId() 等）
                        Long processId = task.getProcessId() ; // 或 task.getProcessId()
                        processExecutionService.runProcess(processId);

                        task.setState(TaskState.COMPLETED);
                        task.setMessage("构建成功");

                        if (task.getRecordId() != null) {
                            try {
                                buildRecordService.markCompleted(task.getRecordId(), task.getMessage());
                            } catch (Exception e) {
                                log.warn("markCompleted failed for recordId={}", task.getRecordId(), e);
                            }
                        }

                        log.debug("任务完成 {}:{} 任务ID={} 时间: {}", task.getJobId(), task.getJobName(), task.getId(), Instant.now());

                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        if (task.getState() != TaskState.CANCELLED) {
                            task.setState(TaskState.FAILED);
                            task.setMessage("任务被取消");
                            if (task.getRecordId() != null) {
                                try {
                                    buildRecordService.markCancelled(task.getRecordId(), "Interrupted");
                                } catch (Exception ex) {
                                    log.warn("markCancelled failed for recordId={}", task.getRecordId(), ex);
                                }
                            }
                        }
                    } catch (Throwable ex) {
                        task.setState(TaskState.FAILED);
                        task.setMessage("Error: " + ex.getMessage());
                        if (task.getRecordId() != null) {
                            try {
                                buildRecordService.markFailed(task.getRecordId(), ex.getMessage());
                            } catch (Exception e) {
                                log.warn("markFailed failed for recordId={}", task.getRecordId(), e);
                            }
                        }
                    } finally {
                        // 取消定时器（如果存在）
                        ScheduledFuture<?> canceller = cancellerRef.get();
                        if (canceller != null) {
                            try { canceller.cancel(false); } catch (Throwable ignored) {}
                        }

                        task.setFinishedAt(LocalDateTime.now());
                        runningFutures.remove(task.getId());
                        runningTasks.remove(task.getId());
                        allTasks.put(task.getId(), task);
                    }
                };

                // 提交任务并立即记录 future（不要阻塞）
                Future<?> f;
                try {
                    f = workers.submit(runnable);
                } catch (RejectedExecutionException rex) {
                    // 若提交被拒绝（线程池满），标记任务失败并更新 DB
                    task.setState(TaskState.FAILED);
                    task.setFinishedAt(LocalDateTime.now());
                    task.setMessage("提交执行失败：线程池拒绝");
                    allTasks.put(task.getId(), task);
                    if (task.getRecordId() != null) {
                        try { buildRecordService.markFailed(task.getRecordId(), "提交执行失败：线程池拒绝"); } catch (Exception ex) { log.warn("markFailed failed", ex); }
                    }
                    continue;
                }

                runningFutures.put(task.getId(), f);
                allTasks.put(task.getId(), task);

                // 安排超时取消（DEFAULT_OUT_TIME 分钟后）
                ScheduledFuture<?> canceller = timeoutScheduler.schedule(() -> {
                    if (!f.isDone()) {
                        boolean cancelled = f.cancel(true);
                        if (cancelled) {
                            task.setState(TaskState.FAILED);
                            task.setMessage("任务超时被取消");
                            if (task.getRecordId() != null) {
                                try { buildRecordService.markFailed(task.getRecordId(), "任务超时"); } catch (Exception ex) { log.warn("markFailed failed", ex); }
                            }
                            task.setFinishedAt(LocalDateTime.now());
                            runningFutures.remove(task.getId());
                            runningTasks.remove(task.getId());
                            allTasks.put(task.getId(), task);
                        }
                    }
                }, DEFAULT_OUT_TIME, TimeUnit.MINUTES);

                cancellerRef.set(canceller);
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (Throwable t) {
            log.error("consumerLoop unexpected error", t);
        }
    }

    // 取消任务：如果在队列中则移除并标记 CANCELLED；若正在运行则尝试取消 future
    public boolean cancel(String taskId) {
        BuildTask task = allTasks.get(taskId);
        if (task == null) return false;

        if (task.getState() == TaskState.QUEUED) {
            boolean removed = queue.remove(task);
            task.setState(TaskState.CANCELLED);
            task.setFinishedAt(LocalDateTime.now());
            task.setMessage("Cancelled (removed from queue)");
            allTasks.put(taskId, task);
            if (task.getRecordId() != null) {
                try { buildRecordService.markCancelled(task.getRecordId(), "Cancelled (removed from queue)"); } catch (Exception ex) { log.warn("markCancelled failed", ex); }
            }
            return true;
        }

        if (task.getState() == TaskState.RUNNING) {
            Future<?> f = runningFutures.get(taskId);
            if (f != null) {
                boolean cancelled = f.cancel(true);
                if (cancelled) {
                    task.setState(TaskState.CANCELLED);
                    task.setFinishedAt(LocalDateTime.now());
                    task.setMessage("Cancelled (interrupted)");
                    allTasks.put(taskId, task);
                    if (task.getRecordId() != null) {
                        try { buildRecordService.markCancelled(task.getRecordId(), "Cancelled (interrupted)"); } catch (Exception ex) { log.warn("markCancelled failed", ex); }
                    }
                }
                return cancelled;
            }
        }

        return false;
    }

    // 在 BuildQueueService 类中新增此方法
    /**
     * 直接立即执行一次构建（不放入内存队列），并返回 BuildTask（状态会变为 RUNNING）
     *
     * @param job 构建任务对应的 JobEntity，不能为空
     * @param triggerId 可为空，表示触发来源
     * @return 立即执行的 BuildTask
     */
    public BuildTask runNow(JobEntity job, Long triggerId) {
        if (job == null) throw new IllegalArgumentException("job null");

        // 创建 BuildTask 并分配 buildNumber / record
        BuildTask task = new BuildTask(job.getId(), job.getName(), job.getProcessId());
        int buildNumber = buildNumberAllocator.nextBuildNumber(job.getId());
        task.setBuildNumber(buildNumber);

        BuildRecordEntity rec = null;
        try {
            rec = buildRecordService.createQueuedRecord(job.getId(), triggerId, job.getName(), buildNumber, job.getProcessId());
            if (rec != null) {
                task.setRecordId(rec.getId());
            }
        } catch (Exception ex) {
            log.warn("createQueuedRecord failed", ex);
            // 继续执行：recordId 可能为 null
        }

        // 标记为 RUNNING 并加入跟踪集合
        task.setState(TaskState.RUNNING);
        task.setStartedAt(LocalDateTime.now());
        allTasks.put(task.getId(), task);
        runningTasks.put(task.getId(), task);

        if (task.getRecordId() != null) {
            try {
                buildRecordService.markRunning(task.getRecordId());
            } catch (Exception e) {
                log.warn("markRunning failed for recordId={}", task.getRecordId(), e);
            }
        }

        // 同 consumer 的 runnable 实现（处理 InterruptedException / Throwable / finally）
        final AtomicReference<ScheduledFuture<?>> cancellerRef = new AtomicReference<>();

        Runnable runnable = () -> {
            try {
                log.debug("runNow start {}:{} 任务ID={} 时间: {}", task.getJobId(), task.getJobName(), task.getId(), Instant.now());

                Long processId = task.getProcessId();
                processExecutionService.runProcess(processId);

                task.setState(TaskState.COMPLETED);
                task.setMessage("构建成功");
                if (task.getRecordId() != null) {
                    try {
                        buildRecordService.markCompleted(task.getRecordId(), task.getMessage());
                    } catch (Exception e) {
                        log.warn("markCompleted failed for recordId={}", task.getRecordId(), e);
                    }
                }

                log.debug("runNow completed {}:{} 任务ID={} 时间: {}", task.getJobId(), task.getJobName(), task.getId(), Instant.now());

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (task.getState() != TaskState.CANCELLED) {
                    task.setState(TaskState.FAILED);
                    task.setMessage("任务被取消");
                    if (task.getRecordId() != null) {
                        try {
                            buildRecordService.markCancelled(task.getRecordId(), "Interrupted");
                        } catch (Exception ex) {
                            log.warn("markCancelled failed for recordId={}", task.getRecordId(), ex);
                        }
                    }
                }
            } catch (Throwable ex) {
                task.setState(TaskState.FAILED);
                task.setMessage("Error: " + ex.getMessage());
                if (task.getRecordId() != null) {
                    try {
                        buildRecordService.markFailed(task.getRecordId(), ex.getMessage());
                    } catch (Exception e) {
                        log.warn("markFailed failed for recordId={}", task.getRecordId(), e);
                    }
                }
            } finally {
                ScheduledFuture<?> canceller = cancellerRef.get();
                if (canceller != null) {
                    try { canceller.cancel(false); } catch (Throwable ignored) {}
                }

                task.setFinishedAt(LocalDateTime.now());
                runningFutures.remove(task.getId());
                runningTasks.remove(task.getId());
                allTasks.put(task.getId(), task);
            }
        };

        // 提交到线程池
        Future<?> f;
        try {
            f = workers.submit(runnable);
        } catch (RejectedExecutionException rex) {
            // 提交失败，标记失败并返回
            task.setState(TaskState.FAILED);
            task.setFinishedAt(LocalDateTime.now());
            task.setMessage("提交执行失败：线程池拒绝");
            allTasks.put(task.getId(), task);
            if (task.getRecordId() != null) {
                try { buildRecordService.markFailed(task.getRecordId(), "提交执行失败：线程池拒绝"); } catch (Exception ex) { log.warn("markFailed failed", ex); }
            }
            runningTasks.remove(task.getId());
            return task;
        }

        runningFutures.put(task.getId(), f);
        allTasks.put(task.getId(), task);

        // 安排超时取消，与 consumer 保持一致
        ScheduledFuture<?> canceller = timeoutScheduler.schedule(() -> {
            if (!f.isDone()) {
                boolean cancelled = f.cancel(true);
                if (cancelled) {
                    task.setState(TaskState.FAILED);
                    task.setMessage("任务超时被取消");
                    if (task.getRecordId() != null) {
                        try { buildRecordService.markFailed(task.getRecordId(), "任务超时"); } catch (Exception ex) { log.warn("markFailed failed", ex); }
                    }
                    task.setFinishedAt(LocalDateTime.now());
                    runningFutures.remove(task.getId());
                    runningTasks.remove(task.getId());
                    allTasks.put(task.getId(), task);
                }
            }
        }, DEFAULT_OUT_TIME, TimeUnit.MINUTES);

        cancellerRef.set(canceller);

        return task;
    }

    // 查询接口
    public int getQueueSize() {
        return queue.size();
    }

    public boolean delete(String taskId) {
        BuildTask task = allTasks.get(taskId);
        if (task == null) return false;

        if (task.getState() == TaskState.QUEUED) {
            queue.remove(task);
            allTasks.remove(taskId);
            if (task.getRecordId() != null) {
                try { buildRecordService.markFailed(task.getRecordId(), "任务被删除（队列中）"); } catch (Exception ex) { log.warn("markFailed failed", ex); }
            }
            return true;
        }

        if (task.getState() == TaskState.RUNNING) {
            Future<?> f = runningFutures.get(taskId);
            if (f != null) {
                boolean cancelled = f.cancel(true);
                runningFutures.remove(taskId);
                runningTasks.remove(taskId);
                allTasks.remove(taskId);
                if (task.getRecordId() != null) {
                    try { buildRecordService.markFailed(task.getRecordId(), "任务被删除（运行中）"); } catch (Exception ex) { log.warn("markFailed failed", ex); }
                }
                return true;
            } else {
                runningTasks.remove(taskId);
                allTasks.remove(taskId);
                return true;
            }
        }

        allTasks.remove(taskId);
        runningFutures.remove(taskId);
        runningTasks.remove(taskId);
        return true;
    }

    public int getRunningCount() {
        return runningTasks.size();
    }

    public List<BuildTask> listQueued() {
        return new ArrayList<>(queue);
    }

    public List<BuildTask> listRunning() {
        return new ArrayList<>(runningTasks.values());
    }

    public List<BuildTask> listAll() {
        return new ArrayList<>(allTasks.values());
    }

    public BuildTask getById(String taskId) {
        return allTasks.get(taskId);
    }

    public synchronized int getConcurrency() {
        return workers.getCorePoolSize();
    }

    public synchronized void setConcurrency(int newSize) {
        if (newSize < 1) throw new IllegalArgumentException("concurrency must be >= 1");
        int currCore = workers.getCorePoolSize();
        int currMax = workers.getMaximumPoolSize();

        if (newSize > currMax) {
            workers.setMaximumPoolSize(newSize);
            workers.setCorePoolSize(newSize);
        } else {
            workers.setCorePoolSize(newSize);
            workers.setMaximumPoolSize(newSize);
        }
    }

    @Scheduled(fixedRate = 60000)
    public void cleanZombieTasks() {
        LocalDateTime now = LocalDateTime.now();
        for (BuildTask task : runningTasks.values()) {
            LocalDateTime started = task.getStartedAt();
            if (started == null) {
                // 任务尚未真正开始，跳过检查
                continue;
            }
            if (Duration.between(started, now).toMinutes() > DEFAULT_OUT_TIME) {
                cancel(task.getId());
                try {
                    if (task.getRecordId() != null) {
                        buildRecordService.markFailed(task.getRecordId(), "任务超时未响应");
                    }
                } catch (Exception ex) {
                    log.warn("markFailed failed", ex);
                }
            }
        }
    }

    @PreDestroy
    public void shutdown() {
        try { consumerFuture.cancel(true); } catch (Throwable ignored) {}
        try { consumer.shutdownNow(); } catch (Throwable ignored) {}
        try { workers.shutdownNow(); } catch (Throwable ignored) {}
        try { timeoutScheduler.shutdownNow(); } catch (Throwable ignored) {}
    }
}