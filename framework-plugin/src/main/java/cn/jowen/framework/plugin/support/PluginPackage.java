package cn.jowen.framework.plugin.support;

import cn.jowen.framework.plugin.descriptor.PluginDescriptor;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

/**
 * 插件打包工具。将插件 classes + lib（第三方依赖）打包为标准 plugin jar。
 *
 * <p>打包结构：
 * <pre>
 * my-plugin.jar
 * ├── plugin.json          # 描述符
 * ├── com/example/         # 插件 classes
 * └── lib/                 # 第三方依赖 jar
 * </pre>
 *
 * @author 王飞
 */
@NullMarked
public final class PluginPackage {

    private PluginPackage() {
    }

    /**
     * 打包插件 jar。
     *
     * @param descriptor 插件描述符
     * @param classesDir 编译输出目录
     * @param libJars    第三方依赖 jar 列表（可为空）
     * @param outputFile 输出 jar 路径
     * @throws IOException 打包失败时抛出
     */
    public static void pack(PluginDescriptor descriptor, Path classesDir,
                            java.util.List<Path> libJars, Path outputFile) throws IOException {
        Files.createDirectories(outputFile.getParent());
        try (JarOutputStream jos = new JarOutputStream(Files.newOutputStream(outputFile))) {
            // 1. 写入 plugin.json
            jos.putNextEntry(new JarEntry("plugin.json"));
            String json = descriptorToJson(descriptor);
            jos.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            jos.closeEntry();
            // 2. 写入 classes
            if (Files.isDirectory(classesDir)) {
                try (java.util.stream.Stream<Path> stream = Files.walk(classesDir)) {
                    stream.filter(Files::isRegularFile)
                            .filter(p -> p.toString().endsWith(".class"))
                            .forEach(p -> {
                                String entryName = classesDir.relativize(p).toString()
                                        .replace('\\', '/');
                                try {
                                    jos.putNextEntry(new JarEntry(entryName));
                                    Files.copy(p, jos);
                                    jos.closeEntry();
                                } catch (IOException e) {
                                    throw new RuntimeException("写入 class 失败：" + entryName, e);
                                }
                            });
                }
            }
            // 3. 写入 lib jars
            if (libJars != null) {
                for (Path lib : libJars) {
                    String entryName = "lib/" + lib.getFileName().toString();
                    jos.putNextEntry(new JarEntry(entryName));
                    Files.copy(lib, jos);
                    jos.closeEntry();
                }
            }
        }
    }

    private static String descriptorToJson(PluginDescriptor d) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n");
        sb.append("  \"pluginId\": \"").append(d.pluginId()).append("\",\n");
        sb.append("  \"pluginName\": \"").append(d.pluginName()).append("\",\n");
        sb.append("  \"version\": \"").append(d.version()).append("\",\n");
        sb.append("  \"description\": \"").append(d.description()).append("\",\n");
        sb.append("  \"author\": \"").append(d.author()).append("\",\n");
        sb.append("  \"license\": \"").append(d.license()).append("\",\n");
        sb.append("  \"pluginClass\": \"").append(d.pluginClass()).append("\",\n");
        sb.append("  \"enabledByDefault\": ").append(d.enabledByDefault()).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
