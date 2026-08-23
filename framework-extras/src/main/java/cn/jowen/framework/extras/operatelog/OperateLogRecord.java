package cn.jowen.framework.extras.operatelog;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * 操作日志记录。
 *
 * @param traceId      链路追踪 ID（可为 null）
 * @param module       业务模块（不可为 null）
 * @param action       操作动作（不可为 null）
 * @param description  描述（可为 null）
 * @param content      操作内容（可为 null）
 * @param operator     操作人（可为 null）
 * @param operatorId   操作人 ID（可为 null）
 * @param status       状态（不可为 null）
 * @param errorMessage 错误信息（失败时为非 null）
 * @param costTime     耗时（毫秒）
 * @param operateTime  操作时间（不可为 null）
 * @param extra        扩展字段（可为 null，默认空 Map）
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public record OperateLogRecord(@Nullable String traceId, String module, String action,
                               @Nullable String description, @Nullable String content, @Nullable String operator,
                               @Nullable String operatorId, OperateStatus status, @Nullable String errorMessage,
                               long costTime, Instant operateTime, @Nullable Map<String, Object> extra) {

    public OperateLogRecord {
        Objects.requireNonNull(module, "module must not be null");
        if (module.isBlank()) {
            throw new IllegalArgumentException("module must not be blank");
        }
        Objects.requireNonNull(action, "action must not be null");
        if (action.isBlank()) {
            throw new IllegalArgumentException("action must not be blank");
        }
        Objects.requireNonNull(status, "status must not be null");
        Objects.requireNonNull(operateTime, "operateTime must not be null");
        extra = extra == null ? Map.of() : extra;
    }

    /**
     * 便捷构建器。
     */
    public static Builder builder() {
        return new Builder();
    }

    /**
     * @author 王飞
     */
    public static final class Builder {
        private String traceId;
        private String module;
        private String action;
        private String description;
        private String content;
        private String operator;
        private String operatorId;
        private OperateStatus status = OperateStatus.SUCCESS;
        private String errorMessage;
        private long costTime;
        private Instant operateTime;
        private Map<String, Object> extra;

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder module(String module) {
            this.module = module;
            return this;
        }

        public Builder action(String action) {
            this.action = action;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder content(String content) {
            this.content = content;
            return this;
        }

        public Builder operator(String operator) {
            this.operator = operator;
            return this;
        }

        public Builder operatorId(String operatorId) {
            this.operatorId = operatorId;
            return this;
        }

        public Builder status(OperateStatus status) {
            this.status = status;
            return this;
        }

        public Builder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public Builder costTime(long costTime) {
            this.costTime = costTime;
            return this;
        }

        public Builder operateTime(Instant operateTime) {
            this.operateTime = operateTime;
            return this;
        }

        public Builder extra(String key, Object value) {
            if (this.extra == null) {
                this.extra = new java.util.HashMap<>();
            }
            this.extra.put(key, value);
            return this;
        }

        public OperateLogRecord build() {
            Objects.requireNonNull(module, "module must not be null");
            Objects.requireNonNull(action, "action must not be null");
            Instant time = operateTime == null ? Instant.now() : operateTime;
            return new OperateLogRecord(traceId, module, action, description, content, operator,
                    operatorId, status, errorMessage, costTime, time, extra);
        }
    }
}
