package cn.jowen.framework.plugin.dependency;

import org.jspecify.annotations.NullMarked;

/**
 * Maven 风格版本区间（闭/开区间）。
 *
 * <p>示例：{@code [1.0.0,2.0.0)} 表示包含 1.0.0、不包含 2.0.0 的版本区间。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class VersionRange {

    private final String lower;
    private final String upper;
    private final boolean lowerInclusive;
    private final boolean upperInclusive;

    /**
     * 解析 Maven 风格版本区间字符串。
     *
     * @param expression 版本区间表达式，如 {@code [1.0.0,2.0.0)}，不可为 {@code null}
     * @throws DependencyResolutionException 表达式非法时抛出
     */
    public VersionRange(String expression) {
        if (expression == null || expression.isBlank()) {
            throw new DependencyResolutionException("版本区间表达式不能为空");
        }
        String trimmed = expression.trim();
        if (trimmed.length() < 5) {
            throw new DependencyResolutionException("版本区间表达式过短：" + expression);
        }
        boolean lb = trimmed.charAt(0) == '[';
        boolean ub = trimmed.charAt(trimmed.length() - 1) == ']';
        int comma = trimmed.indexOf(',');
        if (comma < 0) {
            throw new DependencyResolutionException("版本区间表达式缺少逗号分隔：" + expression);
        }
        this.lower = trimmed.substring(1, comma).trim();
        this.upper = trimmed.substring(comma + 1, trimmed.length() - 1).trim();
        this.lowerInclusive = lb;
        this.upperInclusive = ub;
        validateVersion(this.lower);
        validateVersion(this.upper);
    }

    private static void validateVersion(String v) {
        if (!v.matches("\\d+\\.\\d+\\.\\d+")) {
            throw new DependencyResolutionException("非法版本号格式：" + v);
        }
    }

    /**
     * 判断给定版本是否落在此区间内。
     *
     * @param version 待判断版本，不可为 {@code null}
     * @return {@code true} 当且仅当版本在区间内
     */
    public boolean matches(String version) {
        int cmp = compareVersions(version, this.lower);
        if (cmp < 0 || (cmp == 0 && !lowerInclusive)) return false;
        int cmpUp = compareVersions(version, this.upper);
        return cmpUp <= 0 && (cmpUp < 0 || upperInclusive);
    }

    /**
     * 比较两个语义化版本号。
     */
    public static int compareVersions(String a, String b) {
        String[] pa = a.split("\\.");
        String[] pb = b.split("\\.");
        int len = Math.max(pa.length, pb.length);
        for (int i = 0; i < len; i++) {
            int va = i < pa.length ? parseIntSafe(pa[i]) : 0;
            int vb = i < pb.length ? parseIntSafe(pb[i]) : 0;
            if (va != vb) return Integer.compare(va, vb);
        }
        return 0;
    }

    private static int parseIntSafe(String s) {
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return 0; }
    }

    public String getLower() { return lower; }
    public String getUpper() { return upper; }
    public boolean isLowerInclusive() { return lowerInclusive; }
    public boolean isUpperInclusive() { return upperInclusive; }

    @Override
    public String toString() {
        return (lowerInclusive ? "[" : "(") + lower + "," + upper + (upperInclusive ? "]" : ")");
    }
}
