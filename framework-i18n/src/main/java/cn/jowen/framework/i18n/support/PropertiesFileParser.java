package cn.jowen.framework.i18n.support;

import cn.jowen.framework.i18n.api.ResourceLoadException;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Properties 文件解析器：统一从 classpath 或文件系统加载资源包，UTF-8 编码，
 * 供各消息源（Properties / ResourceBundle 适配）复用。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class PropertiesFileParser {

    private PropertiesFileParser() {
    }

    /**
     * 从 classpath 加载。
     *
     * @param resourcePath 资源路径（斜杠分隔，如 {@code i18n/messages_zh_CN.properties}）
     * @return 资源内容；不存在返回空 {@link Properties}
     */
    public static Properties loadFromClasspath(String resourcePath) {
        Properties props = new Properties();
        try (InputStream in = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream(resourcePath)) {
            if (in != null) {
                props.load(new InputStreamReader(in, StandardCharsets.UTF_8));
            }
        } catch (IOException ex) {
            throw new ResourceLoadException("加载资源失败: " + resourcePath, ex);
        }
        return props;
    }

    /**
     * 从文件系统加载。
     *
     * @param path 文件路径
     * @return 资源内容；文件不存在返回空 {@link Properties}
     */
    public static Properties loadFromFile(Path path) {
        Properties props = new Properties();
        if (!Files.exists(path)) {
            return props;
        }
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            props.load(reader);
        } catch (IOException ex) {
            throw new ResourceLoadException("读取资源失败: " + path, ex);
        }
        return props;
    }

    /**
     * 按 basename 与区域后缀加载（如 {@code i18n/messages} + {@code _zh_CN}）。
     *
     * @param base       基路径（斜杠分隔，不含后缀）
     * @param suffix     区域后缀（如 {@code _zh_CN}），可为 {@code null}（默认区域）
     * @param fileSystem 为 {@code true} 时按文件系统解析，否则按 classpath
     * @return 解析后的资源内容
     */
    public static Properties load(String base, @Nullable String suffix, boolean fileSystem) {
        String fileName = base + (suffix != null ? suffix : "") + ".properties";
        return fileSystem ? loadFromFile(Path.of(fileName)) : loadFromClasspath(fileName);
    }
}
