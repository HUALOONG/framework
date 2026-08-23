package cn.jowen.framework.plugin.hotswap;

import org.jspecify.annotations.NullMarked;

import java.time.Instant;

/**
 * 热部署上下文。
 *
 * @param oldVersion 旧版本插件 id
 * @param newVersion 新版本插件 id
 * @param swapTime   切换时间
 * @param success    是否成功
 * @author 王飞
 */
@NullMarked
public record HotSwapContext(
        String oldVersion,
        String newVersion,
        Instant swapTime,
        boolean success
) {
    public static HotSwapContext success(String oldId, String newId) {
        return new HotSwapContext(oldId, newId, Instant.now(), true);
    }

    public static HotSwapContext failed(String oldId, String reason) {
        return new HotSwapContext(oldId, null, Instant.now(), false);
    }
}
