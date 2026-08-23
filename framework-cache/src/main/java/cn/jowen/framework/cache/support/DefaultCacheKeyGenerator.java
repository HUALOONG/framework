package cn.jowen.framework.cache.support;

import org.jspecify.annotations.NullMarked;

/**
 * 默认缓存键生成器，基于方法参数名与参数值生成缓存键。
 * 当注解中未指定 key 时启用此默认逻辑。
 *
 * @author 王飞
 * @since 2026-08-24
 */
@NullMarked
public class DefaultCacheKeyGenerator implements CacheKeyGenerator {

    @Override
    public String generate(CacheOperationContext context) {
        StringBuilder sb = new StringBuilder();
        Object[] args = context.args();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(":");
            }
            sb.append("arg").append(i).append("=")
              .append(args[i] == null ? "null" : args[i].toString());
        }
        return sb.toString();
    }
}
