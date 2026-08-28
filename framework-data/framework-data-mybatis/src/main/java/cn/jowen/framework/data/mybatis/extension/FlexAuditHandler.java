package cn.jowen.framework.data.mybatis.extension;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.json.JsonMapper;

/**
 * 数据审计处理器：记录 insert/update/delete 前后对象快照（Jackson 3 JSON），经 {@link AuditSink} 落库。
 *
 * <p>默认 {@link #DEFAULT_SINK} 丢弃记录；业务方注入自定义 {@link AuditSink} 实现审计表写入。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public class FlexAuditHandler implements ExtensionRegistry.Extension {

    /**
     * 变更类型。
     */
    public enum ChangeType {
        INSERT, UPDATE, DELETE
    }

    /**
     * 审计记录：表名（实体类简单名）、变更类型、变更前后 JSON 快照与时间戳。
     */
    public record AuditRecord(String table, ChangeType changeType,
                              @Nullable String beforeJson, @Nullable String afterJson, long timestamp) {
    }

    /**
     * 审计落库出口；业务方实现为审计表写入口。
     */
    @FunctionalInterface
    public interface AuditSink {
        void write(AuditRecord record);
    }

    private static final AuditSink DEFAULT_SINK = record -> { };

    private final AuditSink sink;
    private @Nullable JsonMapper jsonMapper;

    public FlexAuditHandler() {
        this(DEFAULT_SINK);
    }

    /**
     * 构造审计处理器。
     *
     * @param sink 审计落库出口，不可为 {@code null}
     */
    public FlexAuditHandler(AuditSink sink) {
        this.sink = sink;
    }

    @Override
    public String name() {
        return "audit";
    }

    @Override
    public int order() {
        return 200;
    }

    /**
     * 记录新增：after 快照；before 为 {@code null}。
     *
     * @param entity 新增实体
     */
    public void onInsert(Object entity) {
        sink.write(buildRecord(entity, ChangeType.INSERT, null, snapshot(entity)));
    }

    /**
     * 记录更新：仅记录 after 快照（before 快照需业务侧在变更前自行捕获）。
     *
     * @param entity 更新后实体
     */
    public void onUpdate(Object entity) {
        sink.write(buildRecord(entity, ChangeType.UPDATE, null, snapshot(entity)));
    }

    /**
     * 记录删除：仅保留变更前快照。
     *
     * @param entity 删除前实体
     */
    public void onDelete(Object entity) {
        sink.write(buildRecord(entity, ChangeType.DELETE, snapshot(entity), null));
    }

    /**
     * 生成对象 JSON 快照（Jackson 3 不可变 {@link JsonMapper}，懒加载以保持依赖可选）。
     *
     * @param entity 待快照对象
     * @return JSON 字符串；序列化失败返回 {@code null}
     */
    public @Nullable String snapshot(Object entity) {
        try {
            return mapper().writeValueAsString(entity);
        } catch (Exception e) {
            // jackson 为可选依赖；catch 不用 tools.jackson 具体异常，避免类验证时解析缺失类型
            return null;
        }
    }

    private JsonMapper mapper() {
        JsonMapper current = jsonMapper;
        if (current == null) {
            current = JsonMapper.builder().build();
            jsonMapper = current;
        }
        return current;
    }

    private AuditRecord buildRecord(Object entity, ChangeType type,
                                    @Nullable String before, @Nullable String after) {
        return new AuditRecord(entity.getClass().getSimpleName(), type, before, after,
                System.currentTimeMillis());
    }
}