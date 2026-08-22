package cn.jowen.framework.plugin.extension;

import cn.jowen.framework.plugin.PluginLoader;
import org.jspecify.annotations.NullMarked;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 扩展点运行时注册表。
 *
 * <p>支持按扩展点接口获取已注册的实现列表（按 {@link Extension#order()} 升序），
 * 以及按名称获取单个实现。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
public final class ExtensionRegistry {

    /** 扩展点接口 → 有序实现列表（不可变快照）。 */
    private final Map<Class<?>, List<Object>> registry = new ConcurrentHashMap<>();

    /**
     * 注册一个扩展实现实例。
     *
     * @param extension 已标注 {@link Extension} 的实例，不可为 {@code null}
     * @throws PluginLoader.PluginException 扩展点未标注或实例类型不匹配时抛出
     */
    public void register(Object extension) {
        Extension ann = extension.getClass().getAnnotation(Extension.class);
        if (ann == null) {
            throw new PluginLoader.PluginException("扩展实现必须标注 @Extension：" + extension.getClass().getName());
        }
        Class<?> point = ann.point();
        if (!point.isInstance(extension)) {
            throw new PluginLoader.PluginException(
                    "扩展实现类型与扩展点不匹配：" + extension.getClass().getName() + " != " + point.getName());
        }
        registry.computeIfAbsent(point, k -> new ArrayList<>()).add(extension);
        registry.get(point).sort(Comparator.comparingInt(e -> {
            Extension a = e.getClass().getAnnotation(Extension.class);
            return a != null ? a.order() : 0;
        }));
    }

    /**
     * 按扩展点接口获取所有实现列表（按 order 升序）。
     *
     * @param point 扩展点接口，不可为 {@code null}
     * @return 实现列表，不可为 {@code null}（可能为空）
     */
    @SuppressWarnings("unchecked")
    public <T> List<T> getExtensions(Class<T> point) {
        List<Object> list = registry.get(point);
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        return (List<T>) List.copyOf(list);
    }

    /**
     * 按扩展点接口和实现名获取单个实现。
     *
     * @param point 扩展点接口，不可为 {@code null}
     * @param name  实现名，由 {@link Extension#name()} 指定
     * @return 实现实例或 {@code null}
     */
    @SuppressWarnings("unchecked")
    public <T> T getExtension(Class<T> point, String name) {
        List<Object> list = registry.get(point);
        if (list == null || list.isEmpty()) {
            return null;
        }
        for (Object ext : list) {
            Extension ann = ext.getClass().getAnnotation(Extension.class);
            if (ann != null && name.equals(defaultName(ann))) {
                return (T) ext;
            }
        }
        return null;
    }

    private static String defaultName(Extension ann) {
        return ann.name().isEmpty()
                ? ann.point().getSimpleName().toLowerCase()
                : ann.name();
    }

    /**
     * 扫描指定包路径下所有标注 {@link Extension} 的类并自动注册。
     *
     * @param basePackage 基础包名，不可为 {@code null}
     */
    public void scanAndRegister(String basePackage) {
        try {
            String path = basePackage.replace('.', '/');
            java.net.URL url = Thread.currentThread().getContextClassLoader().getResource(path);
            if (url == null) return;
            if ("file".equals(url.getProtocol())) {
                java.nio.file.Path root = java.nio.file.Paths.get(url.toURI());
                scanDirectory(root, basePackage);
            }
        } catch (Exception e) {
            // 扫描失败不阻断
        }
    }

    private void scanDirectory(java.nio.file.Path dir, String basePackage) throws Exception {
        if (!java.nio.file.Files.isDirectory(dir)) return;
        try (java.util.stream.Stream<java.nio.file.Path> stream = java.nio.file.Files.list(dir)) {
            stream.filter(p -> java.nio.file.Files.isRegularFile(p) && p.toString().endsWith(".class"))
                    .forEach(p -> {
                        String className = basePackage + "." + p.getFileName().toString().replace(".class", "");
                        try {
                            Class<?> clazz = Class.forName(className, false,
                                    Thread.currentThread().getContextClassLoader());
                            if (clazz.isAnnotationPresent(Extension.class) && !clazz.isInterface()
                                    && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
                                Object instance = clazz.getDeclaredConstructor().newInstance();
                                register(instance);
                            }
                        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException
                                | InvocationTargetException ignored) {
                        }
                    });
        }
        try (java.util.stream.Stream<java.nio.file.Path> sub = java.nio.file.Files.list(dir)) {
            sub.filter(p -> java.nio.file.Files.isDirectory(p)).forEach(p -> {
                try {
                    scanDirectory(p, basePackage + "." + p.getFileName());
                } catch (Exception ignored) {
                }
            });
        }
    }
}
