package cn.jowen.framework.plugin.extension;

import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import cn.jowen.framework.plugin.registry.ExtensionRegistry;
import org.jspecify.annotations.NullMarked;

import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * 扩展扫描器。按注解扫描或按 plugin.json 的 extensions 配置加载扩展。
 *
 * @author 王飞
 */
@NullMarked
public final class ExtensionScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExtensionScanner.class);

    private final ExtensionRegistry registry;

    public ExtensionScanner(ExtensionRegistry registry) {
        this.registry = registry;
    }

    /**
     * 扫描指定包路径下所有标注 @Extension 的类并注册。
     *
     * @param basePackage 基础包名
     */
    public void scanAndRegister(String basePackage) {
        try {
            String path = basePackage.replace('.', '/');
            java.net.URL url = Thread.currentThread().getContextClassLoader().getResource(path);
            if (url == null) return;
            if ("file".equals(url.getProtocol())) {
                java.nio.file.Path root = java.nio.file.Paths.get(url.toURI());
                scanDirectory(root, basePackage);
            } else if ("jar".equals(url.getProtocol())) {
                String urlPath = url.getPath();
                int exclamationIdx = urlPath.indexOf('!');
                String jarPathStr = exclamationIdx >= 0 ? urlPath.substring(0, exclamationIdx) : urlPath;
                if (jarPathStr.startsWith("/")) jarPathStr = jarPathStr.substring(1);
                java.nio.file.Path jarPath = java.nio.file.Paths.get(jarPathStr);
                try (java.util.jar.JarFile jarFile = new java.util.jar.JarFile(jarPath.toFile())) {
                    scanJarEntries(jarFile, basePackage);
                }
            }
        } catch (Exception e) {
            LOGGER.warn("扩展扫描失败：" + basePackage + "，原因：" + e.getMessage());
        }
    }

    /**
     * 从插件描述符的 extensions 配置中加载扩展。
     *
     * @param definitions 扩展定义列表
     * @param classLoader 插件类加载器
     */
    public void loadFromDescriptor(List<ExtensionDefinition> definitions, ClassLoader classLoader) {
        for (ExtensionDefinition def : definitions) {
            try {
                Object instance = def.instantiate(classLoader);
                if (instance != null) {
                    registry.register(new cn.jowen.framework.plugin.registry.Extension(
                            def.id(), def.extensionPointId(), instance, def.order(),
                            "", def.properties()));
                }
            } catch (Exception e) {
                LOGGER.warn("扩展实例化失败：" + def.className() + "：" + e.getMessage());
            }
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
                            registerIfAnnotated(clazz);
                        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException
                                 | IllegalAccessException | InvocationTargetException e) {
                            LOGGER.debug("跳过扩展类 " + className + "：" + e.getMessage());
                        } catch (Exception e) {
                            LOGGER.debug("跳过扩展类 " + className + "：" + e.getMessage());
                        }
                    });
        }
        try (java.util.stream.Stream<java.nio.file.Path> sub = java.nio.file.Files.list(dir)) {
            sub.filter(p -> java.nio.file.Files.isDirectory(p)).forEach(p -> {
                try {
                    scanDirectory(p, basePackage + "." + p.getFileName());
                } catch (Exception e) {
                    LOGGER.debug("跳过子目录 " + p + "：" + e.getMessage());
                }
            });
        }
    }

    private void scanJarEntries(java.util.jar.JarFile jarFile, String basePackage) throws Exception {
        String prefix = basePackage.replace('.', '/') + "/";
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        java.util.Enumeration<java.util.jar.JarEntry> entries = jarFile.entries();
        while (entries.hasMoreElements()) {
            java.util.jar.JarEntry entry = entries.nextElement();
            String name = entry.getName();
            if (!entry.isDirectory() && name.endsWith(".class") && name.startsWith(prefix)) {
                String className = name.replace('/', '.').substring(0, name.length() - ".class".length());
                try {
                    Class<?> clazz = Class.forName(className, false, cl);
                    registerIfAnnotated(clazz);
                } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException
                         | IllegalAccessException | InvocationTargetException e) {
                    LOGGER.debug("跳过扩展类 " + className + "：" + e.getMessage());
                }
            }
        }
    }

    private void registerIfAnnotated(Class<?> clazz) throws Exception {
        if (clazz.isAnnotationPresent(Extension.class) && !clazz.isInterface()
                && !java.lang.reflect.Modifier.isAbstract(clazz.getModifiers())) {
            Extension ann = clazz.getAnnotation(Extension.class);
            Object instance = clazz.getDeclaredConstructor().newInstance();
            registry.register(new cn.jowen.framework.plugin.registry.Extension(
                    ann.id(), ann.extensionPoint(), instance, ann.order(),
                    "", null));
        }
    }
}
