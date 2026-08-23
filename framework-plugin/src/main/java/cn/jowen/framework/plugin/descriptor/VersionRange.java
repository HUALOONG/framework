package cn.jowen.framework.plugin.descriptor;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 版本范围表达式，如 {@code [1.0.0,2.0.0)}。
 *
 * @author 王飞
 */
@NullMarked
public final class VersionRange {

    private final String lower;
    private final String upper;
    private final boolean lowerInclusive;
    private final boolean upperInclusive;

    /**
     * 解析版本范围表达式。
     *
     * @param expression 如 {@code [1.0.0,2.0.0)}，不可为 {@code null}
     * @throws IllegalArgumentException 表达式非法时抛出
     */
    public VersionRange(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new IllegalArgumentException("版本区间表达式不能为空");
        }
        String trimmed = expression.trim();
        if (trimmed.length() < 5) {
            throw new IllegalArgumentException("版本区间表达式过短：" + expression);
        }
        boolean lb = trimmed.charAt(0) == '[';
        boolean ub = trimmed.charAt(trimmed.length() - 1) == ']';
        int comma = trimmed.indexOf(',');
        if (comma < 0) {
            throw new IllegalArgumentException("版本区间表达式缺少逗号分隔：" + expression);
        }
        this.lower = trimmed.substring(1, comma).trim();
        this.upper = trimmed.substring(comma + 1, trimmed.length() - 1).trim();
        this.lowerInclusive = lb;
        this.upperInclusive = ub;
        validateVersion(this.lower);
        validateVersion(this.upper);
    }

    private static void validateVersion(String v) {
        if (!v.matches("\\d+\\.\\d+(\\.\\d+)?")) {
            throw new IllegalArgumentException("非法版本号格式：" + v);
        }
    }

    /**
     * 判断给定版本是否落在此区间内。
     *
     * @param version 待判断版本，不可为 {@code null}
     * @return {@code true} 当且仅当版本在区间内
     */
    public boolean contains(PluginVersion version) {
        PluginVersion lowerV = PluginVersion.parse(this.lower);
        PluginVersion upperV = PluginVersion.parse(this.upper);
        int cmpLower = version.compareTo(lowerV);
        if (cmpLower < 0 || (cmpLower == 0 && !lowerInclusive)) return false;
        int cmpUpper = version.compareTo(upperV);
        return cmpUpper <= 0 && (cmpUpper < 0 || upperInclusive);
    }

    /**
     * 判断给定版本字符串是否落在此区间内。
     *
     * @param version 版本字符串，不可为 {@code null}
     * @return {@code true} 当且仅当版本在区间内
     */
    public boolean contains(String version) {
        return contains(PluginVersion.parse(version));
    }

    /**
     * 计算两个版本范围的交集。
     *
     * @param other 另一个版本范围，不可为 {@code null}
     * @return 交集范围，无交集时返回 {@code null}
     */
    public @Nullable VersionRange intersect(VersionRange other) {
        // 取较严格的下界
        int cmpLower = PluginVersion.parse(this.lower).compareTo(PluginVersion.parse(other.lower));
        String newLower;
        boolean newLowerInc = this.lowerInclusive;
        if (cmpLower > 0) {
            newLower = this.lower;
            newLowerInc = this.lowerInclusive;
        } else if (cmpLower < 0) {
            newLower = other.lower;
            newLowerInc = other.lowerInclusive;
        } else {
            newLower = this.lower;
            newLowerInc = this.lowerInclusive && other.lowerInclusive;
        }
        // 取较严格的上界
        int cmpUpper = PluginVersion.parse(this.upper).compareTo(PluginVersion.parse(other.upper));
        String newUpper;
        boolean newUpperInc = this.upperInclusive;
        if (cmpUpper < 0) {
            newUpper = this.upper;
            newUpperInc = this.upperInclusive;
        } else if (cmpUpper > 0) {
            newUpper = other.upper;
            newUpperInc = other.upperInclusive;
        } else {
            newUpper = this.upper;
            newUpperInc = this.upperInclusive && other.upperInclusive;
        }
        // 检查交集是否有效
        int cmp = PluginVersion.parse(newLower).compareTo(PluginVersion.parse(newUpper));
        if (cmp > 0 || (cmp == 0 && !(newLowerInc && newUpperInc))) {
            return null;
        }
        return new VersionRange((newLowerInc ? "[" : "(") + newLower + "," + newUpper + (newUpperInc ? "]" : ")"));
    }

    public String getLower() {
        return lower;
    }

    public String getUpper() {
        return upper;
    }

    public boolean isLowerInclusive() {
        return lowerInclusive;
    }

    public boolean isUpperInclusive() {
        return upperInclusive;
    }

    @Override
    public String toString() {
        return (lowerInclusive ? "[" : "(") + lower + "," + upper + (upperInclusive ? "]" : ")");
    }
}
