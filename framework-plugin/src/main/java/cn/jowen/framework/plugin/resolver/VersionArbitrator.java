package cn.jowen.framework.plugin.resolver;

import cn.jowen.framework.plugin.descriptor.PluginVersion;
import cn.jowen.framework.plugin.descriptor.VersionRange;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * 版本仲裁器。计算多个版本范围的交集，返回最高兼容版本。
 *
 * @author 王飞
 */
@NullMarked
public final class VersionArbitrator {

    /**
     * 计算多个版本范围的交集。
     *
     * @param ranges 版本范围列表，不可为 {@code null}
     * @return 交集范围，无交集时返回 {@code null}
     */
    public static @Nullable VersionRange intersect(List<VersionRange> ranges) {
        if (ranges == null || ranges.isEmpty()) return null;
        VersionRange result = ranges.getFirst();
        for (int i = 1; i < ranges.size(); i++) {
            VersionRange next = ranges.get(i);
            VersionRange intersection = result.intersect(next);
            if (intersection == null) return null;
            result = intersection;
        }
        return result;
    }

    /**
     * 从多个版本中选出与给定范围最高兼容的版本。
     *
     * @param versions 候选版本列表
     * @param range    版本范围
     * @return 最高兼容版本，无兼容版本时返回 {@code null}
     */
    public static @Nullable String resolveHighest(List<String> versions, VersionRange range) {
        String highest = null;
        for (String v : versions) {
            if (range.contains(v)) {
                PluginVersion pv = PluginVersion.parse(v);
                if (highest == null || pv.compareTo(PluginVersion.parse(highest)) > 0) {
                    highest = v;
                }
            }
        }
        return highest;
    }
}
