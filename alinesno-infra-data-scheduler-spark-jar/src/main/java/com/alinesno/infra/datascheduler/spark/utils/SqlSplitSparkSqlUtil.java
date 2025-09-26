package com.alinesno.infra.datascheduler.spark.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SqlSplitSparkSqlUtil {

    public static List<String> splitSparkSql(String sql, int maxStatements, int maxSqlLength) {
        if (sql == null) return Collections.emptyList();
        if (sql.length() > maxSqlLength) {
            throw new IllegalArgumentException("SQL too long: " + sql.length());
        }

        List<String> result = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inSingle = false;
        boolean inDouble = false;
        boolean inBacktick = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        int parenDepth = 0;
        int len = sql.length();

        for (int i = 0; i < len; i++) {
            char c = sql.charAt(i);
            char next = (i + 1 < len) ? sql.charAt(i + 1) : '\0';

            // 先处理注释进入（仅在不在字符串/反引号内）
            if (!inSingle && !inDouble && !inBacktick) {
                if (!inBlockComment && c == '-' && next == '-') {
                    inLineComment = true;
                    cur.append(c);
                    cur.append(next);
                    i++;
                    continue;
                }
                if (!inLineComment && c == '/' && next == '*') {
                    inBlockComment = true;
                    cur.append(c);
                    cur.append(next);
                    i++;
                    continue;
                }
            }

            // 处理注释退出及注释内容
            if (inLineComment) {
                cur.append(c);
                if (c == '\n' || c == '\r') {
                    inLineComment = false;
                }
                continue;
            }
            if (inBlockComment) {
                cur.append(c);
                if (c == '*' && next == '/') {
                    cur.append(next);
                    i++;
                    inBlockComment = false;
                }
                continue;
            }

            // 处理引号（注意单引号内的 '' 转义）
            if (!inDouble && !inBacktick && c == '\'') {
                cur.append(c);
                if (inSingle) {
                    // 如果是连续两个单引号，视为转义，消费下一个并保持 inSingle
                    if (next == '\'') {
                        cur.append(next);
                        i++;
                    } else {
                        inSingle = false;
                    }
                } else {
                    inSingle = true;
                }
                continue;
            }
            if (!inSingle && !inBacktick && c == '"') {
                cur.append(c);
                inDouble = !inDouble;
                continue;
            }
            if (!inSingle && !inDouble && c == '`') {
                cur.append(c);
                inBacktick = !inBacktick;
                continue;
            }

            // 普通字符处理：括号深度，用于避免在括号内错误分割（例如函数体、复杂表达式）
            if (!inSingle && !inDouble && !inBacktick) {
                if (c == '(') parenDepth++;
                else if (c == ')') if (parenDepth > 0) parenDepth--;
            }

            // 遇到分号且不在任何引号/注释/括号嵌套时，视为语句分隔
            if (c == ';' && !inSingle && !inDouble && !inBacktick && !inLineComment && !inBlockComment && parenDepth == 0) {
                String stmt = cur.toString().trim();
                if (!stmt.isEmpty()) {
                    result.add(stmt);
                    if (result.size() > maxStatements) {
                        throw new IllegalArgumentException("Too many statements: " + result.size());
                    }
                }
                cur.setLength(0);
                continue;
            }

            // 其他字符追加
            cur.append(c);
        }

        String tail = cur.toString().trim();
        if (!tail.isEmpty()) {
            result.add(tail);
        }
        if (result.size() > maxStatements) {
            throw new IllegalArgumentException("Too many statements: " + result.size());
        }
        return result;
    }
}
