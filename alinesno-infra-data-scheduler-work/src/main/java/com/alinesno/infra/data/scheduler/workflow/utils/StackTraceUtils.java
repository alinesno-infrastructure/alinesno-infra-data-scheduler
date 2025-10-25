package com.alinesno.infra.data.scheduler.workflow.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

public class StackTraceUtils {
    public static String getStackTrace(Throwable throwable) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        return sw.toString();
    }
}