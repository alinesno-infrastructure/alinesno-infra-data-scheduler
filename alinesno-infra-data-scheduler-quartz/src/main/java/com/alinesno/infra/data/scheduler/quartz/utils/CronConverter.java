//package com.alinesno.infra.data.scheduler.quartz.utils;
//
///**
// * Cron 转换工具类
// */
//public class CronConverter {
//
//    /**
//     * 将前端可能传来的 5/6/7 字段 cron 转为 Quartz 可接受的 6 或 7 字段表达式。
//     * 优先返回不带 year 的 6 字段表达式（如果输入带 year 则保持 7 字段）。
//     */
//    public static String toQuartzCron(String input) {
//        if (input == null) throw new IllegalArgumentException("cron is null");
//        String s = input.trim();
//        if (s.isEmpty()) throw new IllegalArgumentException("cron is empty");
//
//        String[] parts = s.split("\\s+");
//        if (parts.length < 5 || parts.length > 7) {
//            throw new IllegalArgumentException("Unsupported cron fields count: " + parts.length);
//        }
//
//        // 如果是 5 字段（crontab: min hour day month dow），在前面加秒位 "0"
//        if (parts.length == 5) {
//            String[] tmp = new String[6];
//            tmp[0] = "0";
//            System.arraycopy(parts, 0, tmp, 1, 5);
//            parts = tmp;
//        }
//
//        // parts.length == 6 或 7
//        boolean hasYear = parts.length == 7;
//
//        // 索引映射（0-based）
//        // 0: seconds, 1: minutes, 2: hours, 3: day-of-month, 4: month, 5: day-of-week, [6: year]
//
//        // 如果 day-of-month 和 day-of-week 都不是 "?"，则将 day-of-week 置为 "?"
//        String dom = parts[3];
//        String dow = parts[5];
//        if (!"?".equals(dom) && !"?".equals(dow)) {
//            // 如果两者都为 "*" 或者任意具体值，优先把 day-of-week 置为 '?'
//            parts[5] = "?";
//        }
//
//        String result = String.join(" ", parts);
//        // 验证是否为 Quartz 有效表达式
//        if (!CronExpression.isValidExpression(result)) {
//            throw new IllegalArgumentException("转换后不是有效的 Quartz Cron: " + result);
//        }
//        return result;
//    }
//
//    // 简单测试
////    public static void main(String[] args) {
////        System.out.println(toQuartzCron("0 0/1 * * * * *")); // -> "0 0/1 * * * ? *"
////        System.out.println(toQuartzCron("*/5 * * * *"));     // -> "0 */5 * * * ?"
////    }
//
//}