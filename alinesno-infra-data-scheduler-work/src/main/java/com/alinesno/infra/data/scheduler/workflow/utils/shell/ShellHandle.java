package com.alinesno.infra.data.scheduler.workflow.utils.shell;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;

/**
 * 简单的 shell 执行器实现：把 stdout 收集到内存并通过 writeLog 写入文件（同时触发 listener）
 */
@Slf4j
public class ShellHandle extends AbstractShell {

    private String[] command;
    private StringBuffer output;

    public ShellHandle(String... execString) {
        this(execString, null);
    }

    public ShellHandle(String[] execString, File dir) {
        this(execString, dir, null);
    }

    public ShellHandle(String[] execString, File dir,
                       java.util.Map<String, String> env) {
        this(execString, dir, env , 0L);
    }

    public ShellHandle(String[] execString, File dir,
                       java.util.Map<String, String> env, long timeout) {
        command = execString.clone();
        if (dir != null) {
            setWorkingDirectory(dir);
        }
        if (env != null) {
            setEnvironment(env);
        }
        timeOutInterval = timeout;
    }

    public static String execCommand(String... cmd) throws IOException {
        return execCommand(null, cmd, 0L);
    }

    public static String execCommand(java.util.Map<String, String> env, String[] cmd,
                                     long timeout) throws IOException {
        ShellHandle exec = new ShellHandle(cmd, null, env, timeout);
        exec.execute();
        return exec.getOutput();
    }

    public static String execCommand(java.util.Map<String,String> env, String ... cmd)
            throws IOException {
        return execCommand(env, cmd, 0L);
    }

    /**
     * 执行命令（调用父类的 run）
     */
    public void execute() throws IOException {
        this.run();
    }

    @Override
    protected String[] getExecString() {
        return command;
    }

    /**
     * 解析执行结果：把 stdout 以 chunk 方式读入 output 并调用 writeLog（会触发 listener）
     * 该实现按块读取（避免逐行阻塞），并将每次读取到的 chunk 传给 writeLog
     */
    @Override
    protected void parseExecResult(BufferedReader lines) throws IOException {
        output = new StringBuffer();
        char[] buf = new char[1024];
        int nRead;
        String chunk;
        while ( (nRead = lines.read(buf, 0, buf.length)) > 0 ) {
            chunk = new String(buf,0,nRead);
            output.append(chunk);
            // 写日志并触发 listener（实时）
            writeLog(chunk);
        }
    }

    /**
     * 返回 stdout 的内容（注意：如果输出非常大，最好不要把全部保存在内存）
     */
    public String getOutput() {
        return (output == null) ? "" : output.toString();
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        String[] args = getExecString();
        for (String s : args) {
            if (s.indexOf(' ') >= 0) {
                builder.append('"').append(s).append('"');
            } else {
                builder.append(s);
            }
            builder.append(' ');
        }
        return builder.toString();
    }
}