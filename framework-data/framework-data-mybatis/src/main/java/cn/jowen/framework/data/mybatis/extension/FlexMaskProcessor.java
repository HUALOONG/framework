package cn.jowen.framework.data.mybatis.extension;

import cn.jowen.framework.core.desensitize.Desensitizer;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;

@NullMarked
public class FlexMaskProcessor implements ExtensionRegistry.Extension {

    private static final Desensitizer DESENSITIZER = Desensitizer.getInstance();

    @Override public String name() { return "mask"; }
    @Override public int order() { return 200; }

    @Nullable
    public Object maskObject(@Nullable Object entity) {
        return DESENSITIZER.maskObject(entity);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> maskList(List<T> entities) {
        if (entities.isEmpty()) {
            return entities;
        } else {
            return entities.stream().map(e -> (T) DESENSITIZER.maskObject(e)).toList();
        }
    }

    @Nullable
    public Map<String, String> maskMap(@Nullable Map<String, String> result, String strategy) {
        return DESENSITIZER.maskMap(result, strategy);
    }
}
