package com.alinesno.infra.data.scheduler.spark.utils;

import lombok.extern.slf4j.Slf4j;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.update.Update;
import org.springframework.stereotype.Service;

import java.lang.reflect.Method;
import java.util.*;
import java.util.regex.Pattern;

/**
 * SQL 安全校验服务：分句、参数绑定、语句类型白名单、表名前缀白名单。
 */
@Slf4j
@Service
public class SqlSecurityService {

    private static final int PREVIEW_LEN = 200;

    // 允许的 SQL 类型（可根据业务收紧）
    private static final Set<Class<?>> ALLOWED_STATEMENT_TYPES = new HashSet<>(
            Arrays.asList(Select.class, Insert.class, Update.class, Delete.class)
    );

    // 允许写入的表名前缀（Iceberg catalog prefix）
    private static final List<String> ALLOWED_TABLE_PREFIXES = Collections.singletonList("aip_catalog");

    // 标识符安全正则（允许点用于 catalog.schema.table）
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_\\.]*$");

    private String preview(String s) {
        if (s == null) return "";
        if (s.length() <= PREVIEW_LEN) return s;
        return s.substring(0, PREVIEW_LEN) + "...[truncated,len=" + s.length() + "]";
    }

    private String shortId(String s) {
        if (s == null) return "null";
        return Integer.toHexString(s.hashCode());
    }

    /**
     * 把原始 SQL 文本按分号分割成语句，跳过引号与注释内的分号。
     */
    public List<String> splitStatements(String sql) {
        List<String> stmts = new ArrayList<>();
        if (sql == null) return stmts;

        log.debug("splitStatements: start, sqlLen={}, preview={}, id={}",
                sql.length(), preview(sql), shortId(sql));

        StringBuilder cur = new StringBuilder();
        boolean inSingle = false, inDouble = false, inLineComment = false, inBlockComment = false;
        for (int i = 0; i < sql.length(); i++) {
            char c = sql.charAt(i);
            char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';
            if (inLineComment) {
                if (c == '\n') inLineComment = false;
                cur.append(c);
                continue;
            }
            if (inBlockComment) {
                // 注意：当看到结束标记时也要把 '/' 追加并移动索引
                if (c == '*' && next == '/') {
                    cur.append(c);
                    cur.append(next);
                    i++;
                    inBlockComment = false;
                    continue;
                }
                cur.append(c);
                continue;
            }
            if (!inSingle && !inDouble) {
                if (c == '-' && next == '-') {
                    inLineComment = true;
                    cur.append(c);
                    cur.append(next);
                    i++;
                    continue;
                }
                if (c == '/' && next == '*') {
                    inBlockComment = true;
                    cur.append(c);
                    cur.append(next);
                    i++;
                    continue;
                }
            }
            if (c == '\'' && !inDouble) {
                inSingle = !inSingle;
                cur.append(c);
                // 处理连续单引号转义
                if (inSingle && i + 1 < sql.length() && sql.charAt(i + 1) == '\'') {
                    // keep as inString, append next and skip
                    cur.append('\'');
                    i++;
                }
                continue;
            }
            if (c == '"' && !inSingle) {
                inDouble = !inDouble;
                cur.append(c);
                continue;
            }
            if (c == ';' && !inSingle && !inDouble && !inLineComment && !inBlockComment) {
                String s = cur.toString().trim();
                if (!s.isEmpty()) {
                    stmts.add(s);
                    log.debug("splitStatements: found stmt id={}, preview={}", shortId(s), preview(s));
                }
                cur = new StringBuilder();
            } else {
                cur.append(c);
            }
        }
        String tail = cur.toString().trim();
        if (!tail.isEmpty()) {
            stmts.add(tail);
            log.debug("splitStatements: last stmt id={}, preview={}", shortId(tail), preview(tail));
        }
        log.debug("splitStatements: done, totalStatements={}, sqlId={}", stmts.size(), shortId(sql));
        return stmts;
    }

    /**
     * 绑定参数到 SQL 模板（只允许 ${name} 形式），字符串严格转义。
     * 不允许把参数直接替换为表名/列名（若必须允许，请先做白名单校验）。
     */
    public String bindParams(String sql, Map<String, Object> params) {
        if (sql == null) return null;
        log.debug("bindParams: start, sqlPreview={}, paramsCount={}", preview(sql), params == null ? 0 : params.size());

        if (params == null || params.isEmpty()) {
            if (sql.matches(".*\\$\\{.+?\\}.*")) {
                log.warn("bindParams: unbound parameters present in SQL preview={}", preview(sql));
                throw new IllegalArgumentException("Unbound parameters present");
            }
            log.debug("bindParams: no params, return original");
            return sql;
        }
        String out = sql;
        for (Map.Entry<String, Object> e : params.entrySet()) {
            String key = e.getKey();
            Object v = e.getValue();
            String replacement;
            if (v == null) replacement = "NULL";
            else if (v instanceof Number) replacement = v.toString();
            else if (v instanceof Boolean) replacement = (Boolean) v ? "TRUE" : "FALSE";
            else {
                replacement = "'" + v.toString().replace("'", "''") + "'";
            }
            out = out.replace("${" + key + "}", replacement);
            log.debug("bindParams: replaced key={}, replacementPreview={}", key, preview(replacement));
        }
        if (out.matches(".*\\$\\{.+?\\}.*")) {
            log.warn("bindParams: unbound parameters remain after replacement, sqlPreview={}", preview(out));
            throw new IllegalArgumentException("Unbound parameters present");
        }
        log.debug("bindParams: done, resultPreview={}", preview(out));
        return out;
    }

    /**
     * 从 Insert/Update/Delete 中提取受影响表名（兼容不同版本 JSqlParser）。
     * 返回 fully qualified table names 列表（如果提取失败返回空列表）。
     */
    private List<String> extractTargetTables(Statement statement) {
        if (statement == null) return Collections.emptyList();
        log.debug("extractTargetTables: start for statementType={}", statement.getClass().getSimpleName());
        try {
            if (statement instanceof Insert) {
                Insert insert = (Insert) statement;
                Table t = insert.getTable();
                String name = t == null ? null : t.getFullyQualifiedName();
                log.debug("extractTargetTables: Insert target={}", preview(name));
                return name == null ? Collections.emptyList() : Collections.singletonList(name);
            } else if (statement instanceof Delete) {
                Delete delete = (Delete) statement;
                Table t = delete.getTable();
                String name = t == null ? null : t.getFullyQualifiedName();
                log.debug("extractTargetTables: Delete target={}", preview(name));
                return name == null ? Collections.emptyList() : Collections.singletonList(name);
            } else if (statement instanceof Update) {
                Update update = (Update) statement;
                try {
                    Method getTables = Update.class.getMethod("getTables");
                    Object res = getTables.invoke(update);
                    if (res instanceof List) {
                        List<?> raw = (List<?>) res;
                        List<String> names = new ArrayList<>();
                        for (Object obj : raw) {
                            if (obj instanceof Table) {
                                names.add(((Table) obj).getFullyQualifiedName());
                            } else if (obj != null) {
                                names.add(obj.toString());
                            }
                        }
                        log.debug("extractTargetTables: Update targets (via getTables)={}", preview(names.toString()));
                        return names;
                    }
                } catch (NoSuchMethodException nsme) {
                    log.debug("extractTargetTables: Update.getTables() not available in this JSqlParser version");
                } catch (Throwable t) {
                    log.warn("extractTargetTables: reflection failed when calling getTables(): {}", t.getMessage());
                }
                try {
                    Table t = update.getTable();
                    String name = t == null ? null : t.getFullyQualifiedName();
                    log.debug("extractTargetTables: Update target (via getTable)={}", preview(name));
                    return name == null ? Collections.emptyList() : Collections.singletonList(name);
                } catch (Throwable e) {
                    log.warn("extractTargetTables: fallback getTable() failed: {}", e.getMessage());
                    return Collections.emptyList();
                }
            }
        } catch (Throwable e) {
            log.warn("extractTargetTables: exception while extracting tables: {}", e.getMessage(), e);
            return Collections.emptyList();
        }
        return Collections.emptyList();
    }

    /**
     * 判断单条 SQL 是否允许执行（基于 JSqlParser）
     */
    public boolean isStatementAllowed(String stmt, String user, Set<String> adminUsers) {
        if (stmt == null) {
            log.warn("isStatementAllowed: null stmt for user={}", user);
            return false;
        }

        log.debug("isStatementAllowed: start user={} stmtPreview={}", user, preview(stmt));

        // 如果 adminUsers 非空，优先判断是否管理员（允许所有语句）
        if (adminUsers != null && adminUsers.contains(user)) {
            log.info("isStatementAllowed: user is admin, allow all statements user={}", user);
            return true;
        }

        Statement statement;
        try {
            statement = CCJSqlParserUtil.parse(stmt);
            log.debug("isStatementAllowed: parsed statement class={}", statement.getClass().getSimpleName());
        } catch (Exception e) {
            log.warn("isStatementAllowed: parse failed for user={}, preview={}, error={}",
                    user, preview(stmt), e.getMessage());
            log.debug("isStatementAllowed: parse exception", e);
            return false; // 解析失败则拒绝
        }

        // 只允许白名单中类型（SELECT/INSERT/UPDATE/DELETE）
        boolean allowedType = ALLOWED_STATEMENT_TYPES.stream().anyMatch(t -> t.isInstance(statement));
        if (!allowedType) {
            log.warn("isStatementAllowed: statement type not allowed user={} type={}",
                    user, statement.getClass().getSimpleName());
            return false;
        }

        // 写操作：提取目标表并逐一校验
        if (statement instanceof Insert || statement instanceof Update || statement instanceof Delete) {
            List<String> tables = extractTargetTables(statement);
            if (tables.isEmpty()) {
                log.warn("isStatementAllowed: failed to extract target tables for write stmt, user={}, preview={}",
                        user, preview(stmt));
                return false;
            }
            for (String table : tables) {
                boolean ok = isTableAllowed(table);
                log.debug("isStatementAllowed: check table user={} table={} allowed={}", user, table, ok);
                if (!ok) {
                    log.warn("isStatementAllowed: table not allowed user={} table={}", user, table);
                    return false;
                }
            }
        }

        // SELECT 默认允许，但可扩展：检查是否引用禁止的表/函数（TODO）
        log.info("isStatementAllowed: statement allowed user={}, preview={}", user, preview(stmt));
        return true;
    }

    public boolean isTableAllowed(String fqTableName) {
        if (fqTableName == null) {
            log.debug("isTableAllowed: null table name");
            return false;
        }
        String cleaned = fqTableName.replace("`", "").trim();
        log.debug("isTableAllowed: checking table preview={}", preview(cleaned));
        if (!SAFE_IDENTIFIER.matcher(cleaned).matches()) {
            log.warn("isTableAllowed: identifier fails SAFE_IDENTIFIER check, cleaned={}", preview(cleaned));
            return false;
        }
        for (String p : ALLOWED_TABLE_PREFIXES) {
            if (cleaned.startsWith(p)) {
                log.debug("isTableAllowed: table allowed by prefix={}, table={}", p, cleaned);
                return true;
            }
        }
        log.warn("isTableAllowed: table not matched allowed prefixes, table={}", cleaned);
        return false;
    }
}