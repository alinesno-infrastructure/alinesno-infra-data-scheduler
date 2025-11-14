package com.alinesno.infra.data.scheduler.workflow.utils;

import java.io.PrintStream;

/**
 * 线程局部变量工具类，管理每个线程的输出流重定向状态
 */
public class ThreadLocalStreamHolder {
    // 存储当前线程的原始System.out
    private static final ThreadLocal<PrintStream> originalOut = new ThreadLocal<>();
    // 存储当前线程的原始System.err
    private static final ThreadLocal<PrintStream> originalErr = new ThreadLocal<>();

    /**
     * 仅对当前线程重定向System.out和System.err
     * @param newOut 新的System.out
     * @param newErr 新的System.err
     */
    public static void redirect(PrintStream newOut, PrintStream newErr) {
        originalOut.set(System.out);
        originalErr.set(System.err);
        System.setOut(newOut);
        System.setErr(newErr);
    }

    /**
     * 仅恢复当前线程的原始输出流，不影响其他线程
     */
    public static void restore() {
        PrintStream out = originalOut.get();
        PrintStream err = originalErr.get();
        if (out != null) {
            System.setOut(out);
        }
        if (err != null) {
            System.setErr(err);
        }
        // 清除ThreadLocal，避免内存泄漏
        originalOut.remove();
        originalErr.remove();
    }
}