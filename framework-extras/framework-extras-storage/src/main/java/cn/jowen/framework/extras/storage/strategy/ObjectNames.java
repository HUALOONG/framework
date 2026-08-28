package cn.jowen.framework.extras.storage.strategy;

import org.jspecify.annotations.NullMarked;

/**
 * 对象键处理的公共工具：文件名提取与扩展名解析。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
final class ObjectNames {

    private ObjectNames() {
    }

    /**
     * 从可能含路径的原始名中取末段文件名。
     *
     * @param originalName 原始名
     * @return 文件名；入参为空时返回 {@code "unnamed"}
     */
    static String fileNameOf(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "unnamed";
        }
        String normalized = originalName.replace('\\', '/');
        int idx = normalized.lastIndexOf('/');
        String name = idx < 0 ? normalized : normalized.substring(idx + 1);
        return name.isBlank() ? "unnamed" : name;
    }

    /**
     * 提取扩展名（含点），无扩展名返回空串。
     *
     * @param fileName 文件名
     * @return 形如 {@code ".jpg"} 的扩展名，或空串
     */
    static String extensionOf(String fileName) {
        String name = fileNameOf(fileName);
        int idx = name.lastIndexOf('.');
        return idx <= 0 ? "" : name.substring(idx);
    }
}
