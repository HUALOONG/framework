package cn.jowen.framework.i18n.source;

import cn.jowen.framework.data.jdbc.core.JdbcTemplate;
import cn.jowen.framework.i18n.api.ResourceLoadException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于数据库的消息源：从消息表按 {@code (locale, code)} 加载文案并缓存，
 * {@link #reload()} 全量重建缓存实现热加载，适合与 {@code DatabasePollingWatcher} 配合。
 *
 * <p>约定表结构（可通过构造参数覆盖列名）：
 * <pre>{@code
 * CREATE TABLE i18n_message (
 *     locale  VARCHAR(32)  NOT NULL,   -- 如 zh_CN / en / 空串=默认
 *     code    VARCHAR(128) NOT NULL,   -- 消息键
 *     message TEXT         NOT NULL,   -- 文案
 *     PRIMARY KEY (locale, code)
 * );
 * }</pre>
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class DatabaseMessageSource extends AbstractMessageSource {

    private final JdbcTemplate jdbcTemplate;
    private final String tableName;
    private final String localeColumn;
    private final String codeColumn;
    private final String messageColumn;

    /**
     * locale → (code → message)。
     */
    private volatile Map<String, Map<String, String>> cache = Map.of();

    /**
     * 构造消息源（默认列名 {@code locale/code/message}，表名 {@code i18n_message}）。
     *
     * @param jdbcTemplate framework-data-jdbc 的 JdbcTemplate，不可为 {@code null}
     */
    public DatabaseMessageSource(JdbcTemplate jdbcTemplate) {
        this(jdbcTemplate, "i18n_message", "locale", "code", "message");
    }

    /**
     * 构造消息源。
     *
     * @param jdbcTemplate  framework-data-jdbc 的 JdbcTemplate
     * @param tableName     消息表名
     * @param localeColumn  区域列名
     * @param codeColumn    编码列名
     * @param messageColumn 文案列名
     */
    public DatabaseMessageSource(JdbcTemplate jdbcTemplate, String tableName, String localeColumn,
                                 String codeColumn, String messageColumn) {
        this.jdbcTemplate = jdbcTemplate;
        this.tableName = tableName;
        this.localeColumn = localeColumn;
        this.codeColumn = codeColumn;
        this.messageColumn = messageColumn;
        reload();
    }

    private static String localeKey(Locale locale) {
        return locale.toString();
    }

    @Override
    protected @Nullable String loadRaw(String code, Locale locale) {
        Map<String, String> row = cache.get(localeKey(locale));
        return row != null ? row.get(code) : null;
    }

    @Override
    public void reload() {
        String sql = "SELECT " + localeColumn + ", " + codeColumn + ", " + messageColumn
                + " FROM " + tableName;
        try {
            Map<String, Map<String, String>> next = new ConcurrentHashMap<>();
            for (Map<String, Object> row : jdbcTemplate.queryForMaps(sql)) {
                Object localeObj = row.get(localeColumn);
                Object codeObj = row.get(codeColumn);
                Object messageObj = row.get(messageColumn);
                if (localeObj == null || codeObj == null || messageObj == null) {
                    continue;
                }
                String locale = String.valueOf(localeObj);
                String code = String.valueOf(codeObj);
                String message = String.valueOf(messageObj);
                next.computeIfAbsent(locale, k -> new ConcurrentHashMap<>()).put(code, message);
            }
            cache = next;
        } catch (RuntimeException ex) {
            throw new ResourceLoadException("加载数据库消息失败: " + tableName, ex);
        }
    }

    /**
     * 当前缓存的区域数。
     */
    public int localeCount() {
        return cache.size();
    }
}
