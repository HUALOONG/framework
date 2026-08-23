package cn.jowen.framework.plugin.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Pattern;

/**
 * 插件工具类。
 *
 * @author 王飞
 */
public final class PluginUtils {

    private static final Pattern PLUGIN_ID_PATTERN =
            Pattern.compile("^[a-zA-Z0-9_.:-]{2,64}$");

    private PluginUtils() {
    }

    /**
     * 校验插件 id 合法性。
     */
    public static boolean isValidPluginId(String id) {
        return id != null && PLUGIN_ID_PATTERN.matcher(id).matches();
    }

    /**
     * 从 JAR 中提取插件主 jar（排除 lib 目录中的依赖 jar）。
     *
     * @param pluginsDir 插件目录
     * @return 主 jar 路径列表
     */
    public static java.util.List<Path> extractPluginJars(java.nio.file.Path pluginsDir) throws IOException {
        java.util.List<Path> result = new java.util.ArrayList<>();
        if (!Files.isDirectory(pluginsDir)) return result;
        try (java.util.stream.Stream<Path> stream = Files.list(pluginsDir)) {
            stream.filter(p -> Files.isRegularFile(p) && p.toString().endsWith(".jar"))
                    .filter(p -> !p.toString().contains("/lib/"))
                    .forEach(result::add);
        }
        return result;
    }

    /**
     * 计算 JAR 文件的 SHA-256 checksum。
     */
    public static String calculateChecksum(Path jarPath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return bytesToHex(digest.digest(Files.readAllBytes(jarPath)));
        } catch (NoSuchAlgorithmException e) {
            throw new IOException("SHA-256 algorithm not available", e);
        }
    }

    /**
     * 获取插件的依赖 jar 列表（lib 目录下的所有 jar）。
     */
    public static java.util.List<Path> getPluginLibs(Path pluginJarPath) throws IOException {
        java.util.List<Path> libs = new java.util.ArrayList<>();
        try (JarFile jarFile = new JarFile(pluginJarPath.toFile())) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String name = entry.getName();
                if (name.startsWith("lib/") && name.endsWith(".jar")) {
                    // 解压到临时目录
                    Path libPath = pluginJarPath.getParent().resolve(name.substring("lib/".length()));
                    if (!Files.exists(libPath)) {
                        try (InputStream is = jarFile.getInputStream(entry)) {
                            Files.createDirectories(libPath.getParent());
                            Files.copy(is, libPath);
                        }
                    }
                    libs.add(libPath);
                }
            }
        }
        return libs;
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
