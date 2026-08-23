package cn.jowen.framework.plugin.descriptor;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 语义化版本解析与比较工具。
 *
 * <p>支持格式：{@code major.minor.patch[-preRelease]+buildMetadata}
 *
 * @author 王飞
 */
@NullMarked
public final class PluginVersion {

    private final int major;
    private final int minor;
    private final int patch;
    private final @Nullable String preRelease;

    private PluginVersion(int major, int minor, int patch, @Nullable String preRelease) {
        this.major = major;
        this.minor = minor;
        this.patch = patch;
        this.preRelease = preRelease;
    }

    /**
     * 解析版本字符串。
     *
     * @param version 版本字符串，不可为 {@code null}
     * @return 解析后的版本对象
     * @throws IllegalArgumentException 格式非法时抛出
     */
    public static PluginVersion parse(String version) {
        if (version == null || version.isBlank()) {
            throw new IllegalArgumentException("版本字符串不能为空");
        }
        String clean = version.trim();
        // 分离 preRelease 和 buildMetadata
        String base = clean;
        String preRelease = null;
        int plusIdx = base.indexOf('+');
        if (plusIdx >= 0) {
            base = base.substring(0, plusIdx);
        }
        int hyphenIdx = base.indexOf('-');
        if (hyphenIdx >= 0) {
            preRelease = base.substring(hyphenIdx + 1);
            base = base.substring(0, hyphenIdx);
        }
        String[] parts = base.split("\\.");
        if (parts.length < 2 || parts.length > 3) {
            throw new IllegalArgumentException("非法版本号格式：" + version);
        }
        int major = parseIntPart(parts[0], "major");
        int minor = parseIntPart(parts[1], "minor");
        int patch = parts.length >= 3 ? parseIntPart(parts[2], "patch") : 0;
        return new PluginVersion(major, minor, patch, preRelease);
    }

    private static int parseIntPart(String s, String field) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("非法版本号字段 " + field + "：" + s, e);
        }
    }

    /**
     * 比较两个版本。
     *
     * @return -1 表示 this < other，0 表示相等，1 表示 this > other
     */
    public int compareTo(PluginVersion other) {
        if (other == null) return 1;
        int c = Integer.compare(this.major, other.major);
        if (c != 0) return c;
        c = Integer.compare(this.minor, other.minor);
        if (c != 0) return c;
        c = Integer.compare(this.patch, other.patch);
        if (c != 0) return c;
        // preRelease 版本低于正式版（1.0.0-alpha < 1.0.0）
        if (this.preRelease == null && other.preRelease != null) return 1;
        if (this.preRelease != null && other.preRelease == null) return -1;
        if (this.preRelease != null && other.preRelease != null) {
            c = this.preRelease.compareTo(other.preRelease);
        }
        return c;
    }

    /**
     * 判断此版本是否在指定范围内兼容。
     *
     * @param range 版本范围，不可为 {@code null}
     * @return {@code true} 表示兼容
     */
    public boolean isCompatible(VersionRange range) {
        return range.contains(this);
    }

    public int getMajor() {
        return major;
    }

    public int getMinor() {
        return minor;
    }

    public int getPatch() {
        return patch;
    }

    public @Nullable String getPreRelease() {
        return preRelease;
    }

    @Override
    public String toString() {
        String s = major + "." + minor + "." + patch;
        if (preRelease != null) s += "-" + preRelease;
        return s;
    }
}
