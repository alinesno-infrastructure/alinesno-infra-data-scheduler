package com.alinesno.infra.data.scheduler.workflow.utils.shell;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 抽象的 Shell 执行基类（带实时日志回调能力）
 * 注：主要修改点：
 *  - 增加 ShellLogListener，用于实时把子进程输出 fragment 推到外部（例如 nodeLogService）
 *  - writeLog 会同时把 fragment 写到文件并回调 listener
 *  - 支持可配置的流编码（默认 UTF-8）
 */
@Slf4j
public abstract class AbstractShell {

    /**
     * 时间超时时间（毫秒），为 0 表示不超时
     */
    protected long timeOutInterval = 0L;

    @Setter
    @Getter
    private String logPath ;

    /**
     * 如果脚本超时
     */
    private AtomicBoolean timedOut;

    /**
     * refresh interval in msec
     */
    private long interval;

    /**
     * last time the command was performed
     */
    private long lastTime;

    /**
     * env for the command execution
     */
    private Map<String, String> environment;
    private File dir;

    /**
     * 子进程对象（有 getter）
     */
    @Getter
    private Process process;
    private int exitCode;

    /**
     * 控制 completed 标志
     */
    private AtomicBoolean completed;

    public AbstractShell() {
        this(0L);
    }

    /**
     * @param interval 重复执行的最小间隔
     */
    public AbstractShell(long interval ) {
        this.interval = interval;
        this.lastTime = (interval<0) ? 0 : -interval;
    }

    /**
     * 设置环境变量
     */
    protected void setEnvironment(Map<String, String> env) {
        this.environment = env;
    }

    /**
     * 设置工作目录
     */
    protected void setWorkingDirectory(File dir) {
        this.dir = dir;
    }

    /**
     * 如果命令需要被重试或限制频率，调用 run() 会执行 runCommand
     */
    protected void run() throws IOException {
        if (lastTime + interval > System.currentTimeMillis()) {
            return;
        }
        // reset for next run
        exitCode = 0;
        runCommand();
    }

    /**
     * 流编码（用于 InputStreamReader 和写日志文件）
     * 默认 UTF-8；在中文 Windows 环境如果需要可以设置为 Charset.forName("GBK")
     */
    private Charset streamCharset = StandardCharsets.UTF_8;

    public void setStreamCharset(Charset cs) {
        if (cs != null) this.streamCharset = cs;
    }

    public Charset getStreamCharset() {
        return this.streamCharset;
    }

    /**
     * 日志回调监听器接口：外部可实现该接口来接收子进程输出的 fragment（实时）
     */
    public interface ShellLogListener {
        /**
         * 接收一段日志片段（可能是一行，也可能是一个 chunk）
         * @param fragment 原始字符串片段（不保证以换行结尾）
         */
        void onLogFragment(String fragment);
    }

    // 日志监听器实例
    private ShellLogListener logListener;

    /**
     * 设置日志监听器（可为 null）
     */
    public void setLogListener(ShellLogListener listener) {
        this.logListener = listener;
    }

    /**
     * 执行命令的实际实现（内部实现）
     */
    private void runCommand() throws IOException {
        ProcessBuilder builder = new ProcessBuilder(getExecString());
        Timer timeOutTimer = null;
        ShellTimeoutTimerTask timeoutTimerTask = null;
        timedOut = new AtomicBoolean(false);
        completed = new AtomicBoolean(false);

        if (environment != null) {
            builder.environment().putAll(this.environment);
        }
        if (dir != null) {
            builder.directory(this.dir);
        }

        process = builder.start();
        ProcessContainer.putProcess(process);

        if (timeOutInterval > 0) {
            timeOutTimer = new Timer();
            timeoutTimerTask = new ShellTimeoutTimerTask(this);
            // 一次性调度
            timeOutTimer.schedule(timeoutTimerTask, timeOutInterval);
        }

        // 使用可配置的流编码读取 stdout/stderr
        final BufferedReader errReader =
                new BufferedReader(new InputStreamReader(process.getErrorStream(), streamCharset));
        BufferedReader inReader =
                new BufferedReader(new InputStreamReader(process.getInputStream(), streamCharset));
        final StringBuilder errMsg = new StringBuilder();

        // 读取 error 流的线程：将每行写入 errMsg 并调用 writeLog（writeLog 会触发 listener）
        Thread errThread = new Thread() {
            @Override
            public void run() {
                try {
                    String line = errReader.readLine();
                    while((line != null) && !isInterrupted()) {
                        errMsg.append(line);
                        // 写入日志文件并触发 listener
                        writeLog(line + System.lineSeparator());
                        errMsg.append(System.getProperty("line.separator"));
                        line = errReader.readLine();
                    }
                } catch(IOException ioe) {
                    log.warn("读取 error 流时出错", ioe);
                }
            }
        };

        // 读取 stdout 的线程：交给子类 parseExecResult 处理（子类会调用 writeLog）
        Thread inThread = new Thread() {
            @Override
            public void run() {
                try {
                    parseExecResult(inReader);
                } catch (IOException ioe) {
                    log.warn("读取 input 流时出错", ioe);
                }
                super.run();
            }
        };

        try {
            errThread.start();
            inThread.start();
        } catch (IllegalStateException ise) { }

        try {
            // 等待进程结束
            exitCode = process.waitFor();
            try {
                // 确保两个读取线程结束
                errThread.join();
                inThread.join();
            } catch (InterruptedException ie) {
                log.warn("等待读取线程结束被中断", ie);
            }
            completed.compareAndSet(false,true);
            if (exitCode != 0 || errMsg.length()>0) {
                // 抛出包含 stderr 的异常消息（使用配置的编码转换）
                throw new ExitCodeException(exitCode, errMsg.toString());
            }
        } catch (InterruptedException ie) {
            throw new IOException(ie.toString());
        } finally {
            if ((timeOutTimer!=null) && !timedOut.get()) {
                timeOutTimer.cancel();
            }
            // 关闭流
            try {
                inReader.close();
            } catch (IOException ioe) {
                log.warn("关闭 input 流出错", ioe);
            }
            if (!completed.get()) {
                errThread.interrupt();
            }
            try {
                errReader.close();
            } catch (IOException ioe) {
                log.warn("关闭 error 流出错", ioe);
            }
            ProcessContainer.removeProcess(process);
            process.destroy();
            lastTime = System.currentTimeMillis();
        }
    }

    /**
     * 子类需要实现：返回要执行的命令数组（如 {"sh","-c","..."} 或 {"cmd.exe","/C","..."}）
     */
    protected abstract String[] getExecString();

    /**
     * 子类需要实现：解析执行输出，并可通过 writeLog 将输出写到文件并触发 listener
     */
    protected abstract void parseExecResult(BufferedReader lines) throws IOException;

    /**
     * 将日志片段写到文件，并调用 listener（如果有）
     * 说明：lines 的编码由 streamCharset 决定，写文件也使用该编码
     */
    @SneakyThrows
    protected void writeLog(String logText){
        // 写入 debug 日志（原始文本）
        try {
            log.debug(logText);
        } catch (Exception ignore){}

        // 写文件（追加）
        if(this.getLogPath() != null){
            try {
                FileUtils.writeStringToFile(new File(this.getLogPath()), logText, this.streamCharset, true);
            } catch (IOException ioe) {
                log.warn("写日志文件失败: {}", ioe.getMessage());
            }
        }

        // 调用 listener（实时上报）
        try {
            if (this.logListener != null && logText != null && !logText.isEmpty()) {
                this.logListener.onLogFragment(logText);
            }
        } catch (Throwable t) {
            // listener 异常不影响主流程
            log.warn("日志回调 listener 抛出异常: {}", t.getMessage());
        }
    }

    /**
     * 设置脚本为超时，用于 TimerTask
     */
    private void setTimedOut() {
        this.timedOut.set(true);
    }

    /**
     * 超时 TimerTask：负责在超时时销毁子进程
     */
    private static class ShellTimeoutTimerTask extends TimerTask {

        private AbstractShell shell;

        public ShellTimeoutTimerTask(AbstractShell shell) {
            this.shell = shell;
        }

        @Override
        public void run() {
            Process p = shell.getProcess();
            try {
                p.exitValue();
            } catch (Exception e) {
                // Process 未终止，则销毁
                if (p != null && !shell.completed.get()) {
                    shell.setTimedOut();
                    p.destroy();
                }
            }
        }
    }

    /**
     * 含 exit code 的 IOException 类型
     */
    public static class ExitCodeException extends IOException {
        int exitCode;

        public ExitCodeException(int exitCode, String message) {
            super(message);
            this.exitCode = exitCode;
        }

        public int getExitCode() {
            return exitCode;
        }
    }

    /**
     * 进程管理容器
     */
    public static class ProcessContainer extends ConcurrentHashMap<Integer, Process>{
        private static final ProcessContainer container = new ProcessContainer();
        private ProcessContainer(){
            super();
        }
        public static final ProcessContainer getInstance(){
            return container;
        }

        public static void putProcess(Process process){
            getInstance().put(process.hashCode(), process);
        }
        public static int processSize(){
            return getInstance().size();
        }

        public static void removeProcess(Process process){
            getInstance().remove(process.hashCode());
        }

        public static void destroyAllProcess(){
            Set<Entry<Integer, Process>> set = getInstance().entrySet();
            for (Entry<Integer, Process> entry : set) {
                try{
                    entry.getValue().destroy();
                } catch (Exception e) {
                    log.error("Destroy All Processes error", e);
                }
            }

            log.info("close " + set.size() + " executing process tasks");
        }
    }
}