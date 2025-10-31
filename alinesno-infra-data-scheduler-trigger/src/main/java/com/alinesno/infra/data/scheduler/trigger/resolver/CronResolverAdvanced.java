package com.alinesno.infra.data.scheduler.trigger.resolver;

import lombok.Data;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * CronResolverAdvanced
 * - 支持 5/6 字段
 * - 支持 H, H(range), H/step, H(range)/step
 * - 支持 list/range/step、月份/星期名
 * - 支持 DOM: L, LW, nW ; DOW: x#n (nth weekday of month)
 * - 解析后生成 CronSchedule，可用 matches(LocalDateTime) 校验
 */
public class CronResolverAdvanced {

    private static final int[][] RANGES = new int[][] {
            {0,59}, // sec
            {0,59}, // min
            {0,23}, // hour
            {1,31}, // dom
            {1,12}, // month
            {0,6}   // dow (0=SUN)
    };

    private static final Pattern SEG_SPLIT = Pattern.compile("\\s+");
    private static final Pattern LIST_SPLIT = Pattern.compile("\\s*,\\s*");
    private static final Pattern RANGE_STEP = Pattern.compile("^(\\*|\\d+|[A-Za-z]+)(?:-(\\d+|[A-Za-z]+))?/(\\d+)$");
    private static final Pattern RANGE_ONLY = Pattern.compile("^(\\d+|[A-Za-z]+)-(\\d+|[A-Za-z]+)$");
    private static final Pattern STEP_ONLY = Pattern.compile("^\\*/(\\d+)$");
    private static final Pattern NUMBER_ONLY = Pattern.compile("^\\d+$");

    private static final Pattern H_SIMPLE = Pattern.compile("^H$");
    private static final Pattern H_RANGE = Pattern.compile("^H\\((\\d+)-(\\d+)\\)$");
    private static final Pattern H_STEP = Pattern.compile("^H/(\\d+)$");
    private static final Pattern H_RANGE_STEP = Pattern.compile("^H\\((\\d+)-(\\d+)\\)/(\\d+)$");

    private static final Pattern DOM_L = Pattern.compile("^L$");
    private static final Pattern DOM_LW = Pattern.compile("^LW$");
    private static final Pattern DOM_NEAREST_W = Pattern.compile("^(\\d+)W$"); // e.g. 15W

    private static final Pattern DOW_NTH = Pattern.compile("^([0-7]|[A-Za-z]+)#(\\d+)$"); // e.g. MON#2 or 2#3 (nth weekday)

    private static final Map<String,Integer> MONTHS = new HashMap<>();
    private static final Map<String,Integer> DOWS = new HashMap<>();

    static {
        MONTHS.put("JAN", 1); MONTHS.put("FEB",2); MONTHS.put("MAR",3); MONTHS.put("APR",4);
        MONTHS.put("MAY",5); MONTHS.put("JUN",6); MONTHS.put("JUL",7); MONTHS.put("AUG",8);
        MONTHS.put("SEP",9); MONTHS.put("OCT",10); MONTHS.put("NOV",11); MONTHS.put("DEC",12);

        DOWS.put("SUN",0); DOWS.put("MON",1); DOWS.put("TUE",2); DOWS.put("WED",3);
        DOWS.put("THU",4); DOWS.put("FRI",5); DOWS.put("SAT",6);
    }

    public CronSchedule parse(String triggerCron, String jobId) {
        if (triggerCron == null) throw new IllegalArgumentException("cron null");
        String[] parts = SEG_SPLIT.split(triggerCron.trim());
        if (parts.length == 5) {
            String[] p6 = new String[6];
            p6[0] = "0";
            System.arraycopy(parts, 0, p6, 1, 5);
            parts = p6;
        } else if (parts.length != 6) {
            throw new IllegalArgumentException("Expect 5- or 6-field cron expression");
        }

        CronSchedule schedule = new CronSchedule();
        for (int i = 0; i < 6; i++) {
            FieldParseResult res = parseField(parts[i], jobId, i);
            schedule.setField(i, res.bitmask, res.isWildcard);
            // 如果是 dom/dow 的特殊规则，也交给 CronSchedule 记录（parseField 会在 CronScheduleRules 中填充）
            if (i == 3) { // dom rules
                schedule.domL = res.domL;
                schedule.domLW = res.domLW;
                schedule.domNearestWeekdays = res.domNearestWeekdays;
            }
            if (i == 5) { // dow rules
                schedule.dowNthMap = res.dowNthMap;
            }
        }
        return schedule;
    }

    private static class FieldParseResult {
        boolean[] bitmask;
        boolean isWildcard;
        // special dom/dow
        boolean domL = false;
        boolean domLW = false;
        List<Integer> domNearestWeekdays = new ArrayList<>();
        Map<Integer,Integer> dowNthMap = new HashMap<>();
        FieldParseResult(boolean[] bitmask, boolean isWildcard) { this.bitmask = bitmask; this.isWildcard = isWildcard; }
    }

    private FieldParseResult parseField(String text, String jobId, int fieldIndex) {
        int min = RANGES[fieldIndex][0];
        int max = RANGES[fieldIndex][1];
        boolean[] mask = new boolean[max + 1];
        text = text.trim();
        if (text.equals("?")) text = "*";
        if (text.equals("*")) {
            Arrays.fill(mask, true);
            return new FieldParseResult(mask, true);
        }

        String[] segments = LIST_SPLIT.split(text);
        boolean anySet = false;
        FieldParseResult result = new FieldParseResult(mask, false);

        for (String seg0 : segments) {
            String seg = seg0.trim();
            if (seg.isEmpty()) continue;
            Matcher m;

            // DOM-specific tokens
            if (fieldIndex == 3) {
                if (DOM_L.matcher(seg).matches()) {
                    result.domL = true;
                    anySet = true;
                    continue;
                }
                if (DOM_LW.matcher(seg).matches()) {
                    result.domLW = true;
                    anySet = true;
                    continue;
                }
                m = DOM_NEAREST_W.matcher(seg);
                if (m.matches()) {
                    int n = Integer.parseInt(m.group(1));
                    n = clamp(n, min, max);
                    result.domNearestWeekdays.add(n);
                    anySet = true;
                    continue;
                }
            }

            // DOW-specific nth: e.g. MON#2 or 2#3
            if (fieldIndex == 5) {
                m = DOW_NTH.matcher(seg);
                if (m.matches()) {
                    String left = m.group(1);
                    int nth = Integer.parseInt(m.group(2));
                    int weekday;
                    if (NUMBER_ONLY.matcher(left).matches()) {
                        weekday = Integer.parseInt(left);
                        if (weekday == 7) weekday = 0;
                    } else {
                        weekday = parseNameToDow(left);
                    }
                    weekday = clamp(weekday, RANGES[5][0], RANGES[5][1]);
                    if (nth >= 1 && nth <= 5) {
                        result.dowNthMap.put(weekday, nth);
                        anySet = true;
                        continue;
                    }
                }
            }

            // H token support
            m = H_RANGE_STEP.matcher(seg);
            if (m.matches()) {
                int a = Integer.parseInt(m.group(1));
                int b = Integer.parseInt(m.group(2));
                int step = Integer.parseInt(m.group(3));
                a = clamp(a, min, max); b = clamp(b, min, max);
                if (a <= b) {
                    int span = b - a + 1;
                    int start = a + mod(hash(jobId, fieldIndex), span);
                    fillWithStep(mask, a, b, start, step);
                    anySet = true;
                    continue;
                }
            }
            m = H_RANGE.matcher(seg);
            if (m.matches()) {
                int a = Integer.parseInt(m.group(1));
                int b = Integer.parseInt(m.group(2));
                a = clamp(a, min, max); b = clamp(b, min, max);
                if (a <= b) {
                    int span = b - a + 1;
                    int val = a + mod(hash(jobId, fieldIndex), span);
                    if (val >= min && val <= max) { mask[val] = true; anySet = true; }
                    continue;
                }
            }
            m = H_STEP.matcher(seg);
            if (m.matches()) {
                int step = Integer.parseInt(m.group(1));
                if (step <= 0) step = 1;
                int offset = mod(hash(jobId, fieldIndex), step);
                int start = min + offset;
                fillWithStep(mask, min, max, start, step);
                anySet = true;
                continue;
            }
            m = H_SIMPLE.matcher(seg);
            if (m.matches()) {
                int val = min + mod(hash(jobId, fieldIndex), (max - min + 1));
                mask[val] = true;
                anySet = true;
                continue;
            }

            // Non-H tokens
            m = RANGE_STEP.matcher(seg);
            if (m.matches()) {
                String left = m.group(1);
                String right = m.group(2);
                int step = Integer.parseInt(m.group(3));
                if ("*".equals(left)) {
                    fillWithStep(mask, min, max, min, step);
                } else {
                    int a = parseValue(left, fieldIndex, min, max);
                    int b = (right == null) ? max : parseValue(right, fieldIndex, min, max);
                    if (a > b) { int tmp=a; a=b; b=tmp; }
                    fillWithStep(mask, a, b, a, step);
                }
                anySet = true;
                continue;
            }

            m = STEP_ONLY.matcher(seg);
            if (m.matches()) {
                int step = Integer.parseInt(m.group(1));
                fillWithStep(mask, min, max, min, step);
                anySet = true;
                continue;
            }

            m = RANGE_ONLY.matcher(seg);
            if (m.matches()) {
                int a = parseValue(m.group(1), fieldIndex, min, max);
                int b = parseValue(m.group(2), fieldIndex, min, max);
                if (a > b) { int tmp=a; a=b; b=tmp; }
                for (int v = a; v <= b; v++) mask[v] = true;
                anySet = true;
                continue;
            }

            if (NUMBER_ONLY.matcher(seg).matches() || isName(seg)) {
                int v = parseValue(seg, fieldIndex, min, max);
                mask[v] = true;
                anySet = true;
                continue;
            }

            // fallback for complex with H
            if (seg.contains("H")) {
                String replaced = fallbackReplaceH(seg, jobId, fieldIndex);
                FieldParseResult fr = parseField(replaced, jobId, fieldIndex);
                for (int k = 0; k < fr.bitmask.length; k++) if (fr.bitmask[k]) mask[k] = true;
                anySet = anySet || !allFalse(fr.bitmask);
                // merge special rules too
                if (fr.domL) result.domL = true;
                if (fr.domLW) result.domLW = true;
                if (fr.domNearestWeekdays != null) result.domNearestWeekdays.addAll(fr.domNearestWeekdays);
                if (fr.dowNthMap != null) result.dowNthMap.putAll(fr.dowNthMap);
                continue;
            }

            throw new IllegalArgumentException("Unsupported cron token: " + seg);
        }

        if (!anySet) {
            Arrays.fill(mask, true);
            result.isWildcard = true;
            result.bitmask = mask;
            return result;
        }
        result.bitmask = mask;
        result.isWildcard = false;
        return result;
    }

    private static boolean allFalse(boolean[] a) {
        for (boolean b : a) if (b) return false;
        return true;
    }

    private static void fillWithStep(boolean[] mask, int a, int b, int start, int step) {
        if (step <= 0) step = 1;
        int width = b - a + 1;
        int s = a + mod(start - a, width);
        for (int v = s; v <= b; v += step) {
            if (v >= 0 && v < mask.length) mask[v] = true;
        }
    }

    private static int parseNameToDow(String t) {
        String u = t.toUpperCase(Locale.ROOT);
        if (DOWS.containsKey(u)) return DOWS.get(u);
        try {
            int v = Integer.parseInt(u);
            if (v == 7) v = 0;
            return v;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Unknown dow: " + t);
        }
    }

    private static int parseValue(String token, int fieldIndex, int min, int max) {
        token = token.toUpperCase(Locale.ROOT);
        if (fieldIndex == 4 || fieldIndex == 5) {
            if (MONTHS.containsKey(token)) return MONTHS.get(token);
            if (DOWS.containsKey(token)) return DOWS.get(token);
        }
        if (MONTHS.containsKey(token)) return MONTHS.get(token);
        if (DOWS.containsKey(token)) return DOWS.get(token);
        try {
            int v = Integer.parseInt(token);
            if (fieldIndex == 5 && v == 7) v = 0;
            if (v < min) return min;
            if (v > max) return max;
            return v;
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("Unknown token: " + token);
        }
    }

    private static boolean isName(String token) {
        String t = token.toUpperCase(Locale.ROOT);
        return MONTHS.containsKey(t) || DOWS.containsKey(t);
    }

    // fallback Replace H(...) with deterministic numbers (same as earlier)
    private static String fallbackReplaceH(String seg, String jobId, int fieldIndex) {
        Matcher m;
        m = H_RANGE_STEP.matcher(seg);
        if (m.find()) {
            String rep = resolveHSimple(jobId, fieldIndex, Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
            return seg.replaceFirst(Pattern.quote(m.group(0)), rep + "/" + m.group(3));
        }
        m = H_RANGE.matcher(seg);
        if (m.find()) {
            String rep = resolveHSimple(jobId, fieldIndex, Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2)));
            return seg.replaceFirst(Pattern.quote(m.group(0)), rep);
        }
        m = H_STEP.matcher(seg);
        if (m.find()) {
            String rep = resolveHSimple(jobId, fieldIndex, RANGES[fieldIndex][0], RANGES[fieldIndex][1]);
            return seg.replaceFirst(Pattern.quote(m.group(0)), rep + "/" + m.group(1));
        }
        m = H_SIMPLE.matcher(seg);
        if (m.find()) {
            String rep = resolveHSimple(jobId, fieldIndex, RANGES[fieldIndex][0], RANGES[fieldIndex][1]);
            return seg.replaceFirst("H", rep);
        }
        return seg.replaceAll("H(\\([^)]*\\))?", "0");
    }

    private static String resolveHSimple(String jobId, int fieldIndex, int a, int b) {
        a = clamp(a, RANGES[fieldIndex][0], RANGES[fieldIndex][1]);
        b = clamp(b, RANGES[fieldIndex][0], RANGES[fieldIndex][1]);
        int span = Math.max(1, b - a + 1);
        int v = a + mod(hash(jobId, fieldIndex), span);
        return String.valueOf(v);
    }

    private static int clamp(int v, int a, int b) {
        if (v < a) return a;
        if (v > b) return b;
        return v;
    }

    private static int mod(int a, int m) {
        if (m <= 0) return 0;
        int r = a % m;
        if (r < 0) r += m;
        return r;
    }

    private static int hash(String jobId, int fieldIndex) {
        String key = (jobId == null ? "" : jobId) + ":" + fieldIndex;
        CRC32 crc = new CRC32();
        crc.update(key.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        long v = crc.getValue();
        return (int)(v & 0x7fffffff);
    }

    // CronSchedule stores bitmasks and special dom/dow rules
    @Data
    public static class CronSchedule {
        private boolean[] sec, min, hour, dom, month, dow;
        private boolean secWildcard, minWildcard, hourWildcard, domWildcard, monthWildcard, dowWildcard;

        // special rules
        // DOM:
        boolean domL = false; // L
        boolean domLW = false; // LW
        List<Integer> domNearestWeekdays = new ArrayList<>(); // nW tokens
        // DOW:
        Map<Integer,Integer> dowNthMap = new HashMap<>(); // weekday -> nth

        // 返回允许的 seconds 列表
        public List<Integer> getAllowedSeconds() {
            return indicesOf(this.sec);
        }

        public List<Integer> getAllowedMinutes() {
            return indicesOf(this.min);
        }

        public List<Integer> getAllowedHours() {
            return indicesOf(this.hour);
        }

        public List<Integer> getAllowedDom() {
            return indicesOf(this.dom);
        }

        public List<Integer> getAllowedMonths() {
            return indicesOf(this.month);
        }

        public List<Integer> getAllowedDow() {
            return indicesOf(this.dow);
        }

        void setField(int idx, boolean[] mask, boolean wildcard) {
            switch (idx) {
                case 0: this.sec = mask; this.secWildcard = wildcard; break;
                case 1: this.min = mask; this.minWildcard = wildcard; break;
                case 2: this.hour = mask; this.hourWildcard = wildcard; break;
                case 3: this.dom = mask; this.domWildcard = wildcard; break;
                case 4: this.month = mask; this.monthWildcard = wildcard; break;
                case 5: this.dow = mask; this.dowWildcard = wildcard; break;
                default: throw new IllegalArgumentException("bad idx");
            }
        }

        // matches(LocalDateTime) 包含对 L/W/# 的判断
        public boolean matches(LocalDateTime dt) {
            int s = dt.getSecond();
            int m = dt.getMinute();
            int h = dt.getHour();
            int day = dt.getDayOfMonth();
            int mon = dt.getMonthValue();
            int dowVal = dt.getDayOfWeek().getValue() % 7; // MON=1..SUN=7 -> SUN->0

            if (sec != null && (s < 0 || s >= sec.length || !sec[s])) return false;
            if (min != null && (m < 0 || m >= min.length || !min[m])) return false;
            if (hour != null && (h < 0 || h >= hour.length || !hour[h])) return false;
            if (month != null && (mon < 0 || mon >= month.length || !month[mon])) return false;

            // normal bitmask checks
            boolean domMatch = (dom != null && day < dom.length && dom[day]);
            boolean dowMatch = (dow != null && dowVal < dow.length && dow[dowVal]);

            // handle special DOM rules: L, LW, nW
            if (domL) {
                YearMonth ym = YearMonth.of(dt.getYear(), dt.getMonthValue());
                int last = ym.lengthOfMonth();
                if (day == last) domMatch = true;
            }
            if (domLW) {
                YearMonth ym = YearMonth.of(dt.getYear(), dt.getMonthValue());
                int last = ym.lengthOfMonth();
                int candidate = last;
                // if last is Saturday(6) -> Friday (last-1). if last is Sunday(0 mod from mapping) -> Friday (last-2) or Monday?
                // We choose nearest weekday on or before last day (Mon-Fri)
                int dowOfLast = LocalDateTime.of(dt.getYear(), dt.getMonthValue(), last, 0, 0).getDayOfWeek().getValue() % 7;
                if (dowOfLast == 6) candidate = last - 1; // Saturday -> Friday
                else if (dowOfLast == 0) candidate = last - 2; // Sunday -> Friday
                if (day == candidate) domMatch = true;
            }
            if (domNearestWeekdays != null && !domNearestWeekdays.isEmpty()) {
                for (Integer n : domNearestWeekdays) {
                    int candidate = nearestWeekdayOfMonth(dt.getYear(), dt.getMonthValue(), n);
                    if (candidate == day) { domMatch = true; break; }
                }
            }

            // handle DOW nth (#)
            if (dowNthMap != null && !dowNthMap.isEmpty()) {
                for (Map.Entry<Integer,Integer> e : dowNthMap.entrySet()) {
                    int targetDow = e.getKey(); // 0..6
                    int nth = e.getValue();
                    if (isNthWeekdayOfMonth(dt, targetDow, nth)) { dowMatch = true; break; }
                }
            }

            // DOM/DOW combine rule:
            boolean domWildcard = this.domWildcard;
            boolean dowWildcard = this.dowWildcard;

            if (domWildcard && dowWildcard) {
                return true;
            } else if (!domWildcard && !dowWildcard) {
                return domMatch || dowMatch;
            } else if (!domWildcard) {
                return domMatch;
            } else {
                return dowMatch;
            }
        }

        // nearest weekday to given day n in month (Mon-Fri)
        private int nearestWeekdayOfMonth(int year, int month, int n) {
            YearMonth ym = YearMonth.of(year, month);
            int last = ym.lengthOfMonth();
            if (n > last) n = last;
            if (n < 1) n = 1;
            // If n is weekend, move to nearest weekday:
            LocalDateTime dt = LocalDateTime.of(year, month, n, 0, 0);
            int dow = dt.getDayOfWeek().getValue() % 7;
            if (dow >= 1 && dow <= 5) return n; // Mon-Fri
            if (dow == 6) { // Saturday -> previous Friday (n-1)
                if (n - 1 >= 1) return n - 1;
                else return n + 2; // fallback
            }
            if (dow == 0) { // Sunday -> next Monday (n+1), unless n==last then n-2
                if (n + 1 <= last) return n + 1;
                else return n - 2;
            }
            return n;
        }

        // whether given datetime is nth occurrence of weekday in month
        private boolean isNthWeekdayOfMonth(LocalDateTime dt, int targetDow, int nth) {
            int year = dt.getYear();
            int month = dt.getMonthValue();
            int day = dt.getDayOfMonth();
            int count = 0;
            YearMonth ym = YearMonth.of(year, month);
            int last = ym.lengthOfMonth();
            for (int d = 1; d <= last; d++) {
                LocalDateTime cur = LocalDateTime.of(year, month, d, 0, 0);
                int dow = cur.getDayOfWeek().getValue() % 7;
                if (dow == targetDow) {
                    count++;
                    if (d == day) {
                        return count == nth;
                    }
                }
            }
            return false;
        }

        // expose bitmask conversion utility (for debug controller)
        public List<Integer> indicesOf(boolean[] mask) {
            List<Integer> out = new ArrayList<>();
            if (mask == null) return out;
            for (int i = 0; i < mask.length; i++) if (mask[i]) out.add(i);
            return out;
        }
    }
}