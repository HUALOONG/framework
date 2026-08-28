package cn.jowen.framework.data.mybatis.extension;

import cn.jowen.framework.core.desensitize.Desensitizer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

@NullMarked
/**
 * 「FlexMask」封装相关能力。
 *
 * @author Jowen
 * @since 0.0.1
 * @version 0.0.1
 */
public class FlexMaskProcessor implements ExtensionRegistry.Extension {

    /** DESENSITIZER 常量。 */
    private static final Desensitizer DESENSITIZER = Desensitizer.getInstance();

    /** return 字段。 */
    @Override public String name() { return "mask"; }
    /** return 字段。 */
    @Override public int order() { return 200; }

    /**
     * 执行mask object操作。
     * @param entity 参数 entity
     * @return 结果
     */
    @Nullable
    public Object maskObject(@Nullable Object entity) {
        return DESENSITIZER.maskObject(entity);
    }

    /**
     * 执行@ suppress warnings操作。
     * @return 结果
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> maskList(List<T> entities) {
        if (entities.isEmpty()) {
            return entities;
        } else {
            return entities.stream().map(e -> (T) DESENSITIZER.maskObject(e)).toList();
        }
    }

    /**
     * 执行mask map操作。
     * @param strategy 参数 strategy
     * @return 结果
     */
    @Nullable
    public Map<String, String> maskMap(@Nullable Map<String, String> result, String strategy) {
        return DESENSITIZER.maskMap(result, strategy);
    }
}
