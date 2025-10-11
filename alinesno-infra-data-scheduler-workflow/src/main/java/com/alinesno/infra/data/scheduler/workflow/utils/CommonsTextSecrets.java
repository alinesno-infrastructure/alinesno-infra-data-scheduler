package com.alinesno.infra.data.scheduler.workflow.utils;

import com.alinesno.infra.common.web.log.utils.SpringUtils;
import com.alinesno.infra.data.scheduler.workflow.logger.NodeLog;
import com.alinesno.infra.data.scheduler.workflow.logger.NodeLogService;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.commons.text.lookup.StringLookupFactory;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 密文替换工具类（兼容不同 commons-text 版本），参考github-action密钥替换方式
 */
@Slf4j
@NoArgsConstructor
public class CommonsTextSecrets {

    public static String replace(String input, Map<String, ?> secrets) {
        if (input == null) return null;

        // 基础 StringLookup（由原始 Map 包装）
        final StringLookup baseLookup = StringLookupFactory.INSTANCE.mapStringLookup(secrets);

        // 自定义 StringLookup：trim key，并支持带或不带 "secrets." 前缀
        final StringLookup lookup = new StringLookup() {

            @Override
            public String lookup(String key) {
                if (key == null) return null;
                String k = key.trim();
                String v = baseLookup.apply(k);
                if (v != null) return v;
                String prefix = "secrets.";
                if (k.startsWith(prefix)) {
                    v = baseLookup.apply(k.substring(prefix.length()));
                    if (v != null) return v;
                } else {
                    v = baseLookup.apply(prefix + k);
                    if (v != null) return v;
                }
                return null;
            }
        };

        // 先尝试使用直接接受 StringLookup 的构造器（4 参数 -> lookup, prefix, suffix, escapeChar）
        StringSubstitutor substitutor = tryCreateWithStringLookup(lookup, "${{", "}}");

        // 若上面失败，则把 lookup 包装为 Map，并尝试使用 Map 的构造器
        if (substitutor == null) {
            Map<String, String> mapView = new LookupAsMap(lookup);
            substitutor = tryCreateWithMap(mapView, "${{", "}}");
        }

        if (substitutor == null) {
            throw new IllegalStateException("Cannot create StringSubstitutor: incompatible commons-text version.");
        }

        // 可选：当希望未定义变量时抛异常，可启用下一行
        // substitutor.setEnableUndefinedVariableException(true);

        String result = substitutor.replace(input);
        System.out.println(">>>> " + result);

        return result;
    }

    // 尝试反射创建接收 StringLookup 的构造器实例（优先 4 参数，再 3 参数）
    private static StringSubstitutor tryCreateWithStringLookup(StringLookup lookup, String prefix, String suffix) {
        try {
            // 4 参数：(StringLookup, String prefix, String suffix, char escapeChar)
            Constructor<StringSubstitutor> c4 =
                    StringSubstitutor.class.getConstructor(StringLookup.class, String.class, String.class, char.class);
            return c4.newInstance(lookup, prefix, suffix, '$');
        } catch (NoSuchMethodException ignored) {
            // try 3-arg
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to instantiate StringSubstitutor via 4-arg ctor", e);
        }

        try {
            // 3 参数：(StringLookup, String prefix, String suffix)
            Constructor<StringSubstitutor> c3 = StringSubstitutor.class.getConstructor(StringLookup.class, String.class, String.class);
            return c3.newInstance(lookup, prefix, suffix);
        } catch (NoSuchMethodException ignored) {
            // not available in this commons-text version
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to instantiate StringSubstitutor via 3-arg ctor", e);
        }

        return null;
    }

    // 尝试反射创建接收 Map 的构造器实例（常见签名 Map, prefix, suffix）
    private static StringSubstitutor tryCreateWithMap(Map<String, String> map, String prefix, String suffix) {
        try {
            Constructor<StringSubstitutor> cm =
                    StringSubstitutor.class.getConstructor(Map.class, String.class, String.class);
            return cm.newInstance(map, prefix, suffix);
        } catch (NoSuchMethodException ignored) {
            // not available
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new RuntimeException("Failed to instantiate StringSubstitutor via Map ctor", e);
        }
        return null;
    }

    // 一个非常轻量的 Map 视图，将 Map.get(key) 委托给 StringLookup.lookup(key)
    private static final class LookupAsMap extends AbstractMap<String, String> {
        private final StringLookup lookup;

        LookupAsMap(StringLookup lookup) {
            this.lookup = lookup;
        }

        @Override
        public String get(Object key) {
            if (key == null) return null;
            return lookup.apply(String.valueOf(key));
        }

        @Override
        public boolean containsKey(Object key) {
            return get(key) != null;
        }

        @NotNull
        @Override
        public Set<Entry<String, String>> entrySet() {
            // 我们不支持遍历（StringLookup 可能是动态的），返回空集以满足 AbstractMap 要求。
            return Collections.emptySet();
        }
    }

    /**
     * 将字符串中形如 ${{ secrets.KEY }} 的占位符进行脱敏，
     * 整个占位符（包括 $、{、} 及内部空格/内容）都会被替换为等长的 '*' 串。
     *
     * 另外，如果输入中包含 orgSecret map 中的某个 value（字面匹配），
     * 也会将该 value 替换为等长的 '*' 串。
     *
     * @param input 原始字符串
     * @param orgSecret key->secretValue 的映射（如果为 null 或空则仅处理占位符）
     * @return 脱敏后的字符串
     */
    public static String maskSecretsPlaceholders(String input, Map<String, String> orgSecret) {
        if (input == null) return null;

        // 第一步：把 ${{ secrets.X }} 整体替换为等长 '*'
        Pattern pattern = Pattern.compile("\\$\\{\\{\\s*secrets\\.[A-Za-z0-9_.\\-]+\\s*\\}\\}");
        Matcher matcher = pattern.matcher(input);
        StringBuffer sb = new StringBuffer();
        while (matcher.find()) {
            String whole = matcher.group(); // 整个匹配，如 "${{ secrets.PROJECT_CODE }}"
            String stars = repeat('*', whole.length());
            matcher.appendReplacement(sb, Matcher.quoteReplacement(stars));
        }
        matcher.appendTail(sb);
        String result = sb.toString();

        // 第二步：把 orgSecret 中的 value 在结果中进行字面替换（按长度降序，避免短串先替换覆盖长串的一部分）
        if (orgSecret != null && !orgSecret.isEmpty()) {
            // 收集所有非空的 value，去重并按长度降序
            List<String> values = new ArrayList<>();
            for (String v : orgSecret.values()) {
                if (v != null && !v.isEmpty()) values.add(v);
            }
            // 去重并按长度降序
            values = values.stream()
                    .distinct()
                    .sorted(Comparator.comparingInt(String::length).reversed())
                    .toList();

            for (String secretVal : values) {
                if (secretVal.isEmpty()) continue;
                String stars = repeat('*', secretVal.length());
                // 使用 String.replace 做字面替换（不使用正则）
                result = result.replace(secretVal, stars);
            }
        }

        return result;
    }

    private static String repeat(char c, int count) {
        if (count <= 0) return "";
        char[] arr = new char[count];
        Arrays.fill(arr, c);
        return new String(arr);
    }

}