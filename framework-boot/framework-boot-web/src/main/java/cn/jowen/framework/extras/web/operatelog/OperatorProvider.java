package cn.jowen.framework.extras.web.operatelog;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 操作人解析策略：从当前上下文（Security / Session / 自定义）取出操作人标识。
 *
 * <p>默认实现返回 {@code null}；用户可注入自己的实现（如从 Spring Security 取用户名）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
@FunctionalInterface
public interface OperatorProvider {

    /** @return 当前操作人标识，未知返回 {@code null} */
    @Nullable String currentOperator();

    /** 默认实现：不解析操作人 */
    OperatorProvider NONE = () -> null;
}
