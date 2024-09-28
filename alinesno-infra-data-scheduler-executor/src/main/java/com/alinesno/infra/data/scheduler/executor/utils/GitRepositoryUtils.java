package com.alinesno.infra.data.scheduler.executor.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GitRepositoryUtils {

    /**
     * 从 Git 仓库 URL 中提取仓库名称。
     *
     * @param gitUrl Git 仓库 URL
     * @return 仓库名称（不包括扩展名 .git）
     */
    public static String getRepositoryNameFromUrl(String gitUrl) {
        // 定义正则表达式模式
        String patternString = "([^/]+)\\.git$";
        Pattern pattern = Pattern.compile(patternString);

        // 创建 Matcher 对象
        Matcher matcher = pattern.matcher(gitUrl);

        // 查找匹配
        if (matcher.find()) {
            // 返回匹配到的仓库名称
            return matcher.group(1);
        }

        // 如果没有找到匹配，返回 null 或抛出异常
        return null;
    }

}