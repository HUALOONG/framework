package cn.jowen.framework.boot.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * 自动配置登记文件（{@code AutoConfiguration.imports}）与生产代码的一致性回归测试。
 *
 * <p><b>本测试存在的原因（真实事故）</b>：本模块的登记文件曾经漏登
 * {@code FileStorageAutoConfiguration} / {@code MessageAutoConfiguration} /
 * {@code WebExtrasAutoConfiguration} 三个装配类，导致文件存储、消息通知、分布式 Web
 * 增强三组能力在生产环境<b>静默失效</b>——没有报错、没有启动失败，只是功能不存在。
 * 而当时 95% 的测试覆盖率依然是绿的：因为既有测试全部使用
 * {@code ApplicationContextRunner.withConfiguration(AutoConfigurations.of(Xxx.class))}
 * <b>显式</b>传入装配类，绕开了 imports 注册机制，形成典型的测试盲区。
 * 提交 {@code fb3ae56} 补登了这三行，但防回归的断言一直没补，本测试即为填补该盲区。
 *
 * <p>本测试做四件事：
 * <ol>
 *   <li>直接读取 classpath 上的 {@code AutoConfiguration.imports}，解析出登记的类名集合；</li>
 *   <li>扫描 {@value #BASE_PACKAGE} 包下所有带 {@link AutoConfiguration} 注解的类；</li>
 *   <li><b>双向断言</b>两个集合完全相等——漏登会失效，多登/写错类名同样要失败；</li>
 *   <li>校验每个条目都能被类加载器解析，且不是接口、不是抽象类。</li>
 * </ol>
 *
 * <p><b>与既有测试的区别</b>：本测试不启动 Spring 容器、不实例化任何 Bean，
 * 因此不受 {@code @ConditionalOnClass} / {@code @ConditionalOnProperty} 影响，
 * 无论运行环境是否具备 optional 依赖，登记完整性都得到校验。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class AutoConfigurationImportsRegistrationTest {

    /** Spring Boot 读取自动配置条目的标准登记文件路径。 */
    private static final String IMPORTS_RESOURCE =
            "META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";

    /** 本模块所有装配类所在的根包，扫描范围与登记文件的作用域保持一致。 */
    private static final String BASE_PACKAGE = "cn.jowen.framework.boot.autoconfigure";

    /**
     * 本模块在 classpath 上的根地址（目录形式以 {@code /} 结尾，jar 形式以 {@code !/} 结尾）。
     *
     * <p>用途：classpath 上存在<b>多个</b>同名登记文件——{@code spring-boot-autoconfigure}、
     * {@code spring-boot-jdbc}、{@code spring-boot-health} 等 jar 各自都带一份。若不加区分地全部读取，
     * 反向断言会把 Spring 自带的几十个装配类当成「本模块的脏条目」而误报。
     * 此处用本模块内固定类的 {@code .class} 资源地址反推模块根路径，再据此只挑出本模块自己的登记文件，
     * 对目录形态（{@code target/classes/}）与 jar 形态（{@code xxx.jar!/}）均成立。
     */
    private static final String MODULE_CLASSPATH_ROOT = resolveModuleClasspathRoot();

    /** 装配类自身与登记文件都在本模块 classpath 上，用同一个类加载器即可。 */
    private static final ClassLoader CLASS_LOADER =
            AutoConfigurationImportsRegistrationTest.class.getClassLoader();

    /** 失败信息里多条目的缩进前缀，保证控制台输出可直接阅读。 */
    private static final String ENTRY_INDENT = System.lineSeparator() + "  - ";

    /**
     * 登记文件必须存在且至少声明一个条目。
     *
     * <p>这是其余断言的前置条件：文件被误删或打包插件未拷贝资源时，
     * 后续断言会因为「集合都为空」而通过，形成假绿。
     */
    @Test
    void importsResourceMustExistAndDeclareAtLeastOneEntry() {
        assertThat(readRawImportsLines())
                .as("本模块的登记文件 %s 必须存在且至少声明一个自动配置条目", IMPORTS_RESOURCE)
                .isNotEmpty();
    }

    /**
     * 每个 {@code @AutoConfiguration} 类都必须在登记文件中声明。
     *
     * <p>这是本测试的核心断言，直接对应历史上「漏登三行导致能力静默失效」的事故。
     * 失败信息会列出全部漏登类的全限定名，便于直接复制到登记文件。
     */
    @Test
    void everyAutoConfigurationClassMustBeRegisteredInImports() {
        Set<String> registered = importedClassNames();
        Set<String> missing = new TreeSet<>(scanAutoConfigurationClassNames());
        missing.removeAll(registered);

        if (!missing.isEmpty()) {
            fail("以下 @AutoConfiguration 类未在 %s 中登记（共 %d 个）：%s%s%n"
                            + "后果：这些装配类在 Spring Boot 启动时根本不会被加载，对应能力静默失效"
                            + "（不报错、不启动失败，只是功能不存在）。请补登后重跑本测试。",
                    IMPORTS_RESOURCE,
                    missing.size(),
                    ENTRY_INDENT,
                    String.join(ENTRY_INDENT, missing));
        }
    }

    /**
     * 登记文件中的每一行都必须是真实存在、且带 {@code @AutoConfiguration} 的类。
     *
     * <p>反向断言，用于拦截两类脏条目：类名拼写错误（Spring 启动阶段才会抛
     * {@code IllegalArgumentException}）、以及装配类已删除或降级为普通配置类后忘记清理登记行。
     */
    @Test
    void everyImportsEntryMustBeAnExistingAutoConfigurationClass() {
        Set<String> scanned = scanAutoConfigurationClassNames();
        Set<String> unknown = new TreeSet<>(importedClassNames());
        unknown.removeAll(scanned);

        if (!unknown.isEmpty()) {
            fail("登记文件 %s 中的以下条目（共 %d 个）不是 %s 包下带 @AutoConfiguration 的类：%s%s%n"
                            + "可能原因：类名拼写错误、类已被删除或改名、或该类已不再标注 @AutoConfiguration。"
                            + "请修正或删除这些登记行。",
                    IMPORTS_RESOURCE,
                    unknown.size(),
                    BASE_PACKAGE,
                    ENTRY_INDENT,
                    String.join(ENTRY_INDENT, unknown));
        }
    }

    /**
     * 登记文件中的每个条目都必须能被类加载器解析，且是可被 Spring 实例化的具体类。
     *
     * <p>接口与抽象类无法作为自动配置类实例化，登记它们会在容器启动阶段失败；
     * 无法加载的类名则说明登记文件与代码已经脱节。
     */
    @Test
    void everyImportsEntryMustBeResolvableAndInstantiable() {
        List<String> invalid = new ArrayList<>();

        for (String className : importedClassNames()) {
            Class<?> type;
            try {
                type = ClassUtils.forName(className, CLASS_LOADER);
            } catch (ClassNotFoundException | LinkageError ex) {
                invalid.add(className + " → 无法加载（" + ex.getClass().getSimpleName() + "）");
                continue;
            }
            if (type.isInterface()) {
                invalid.add(className + " → 是接口，不可实例化");
            } else if (Modifier.isAbstract(type.getModifiers())) {
                invalid.add(className + " → 是抽象类，不可实例化");
            }
        }

        assertThat(invalid)
                .as("登记文件 %s 中存在不可用的条目：%s%s",
                        IMPORTS_RESOURCE, ENTRY_INDENT, String.join(ENTRY_INDENT, invalid))
                .isEmpty();
    }

    /**
     * 登记文件中不允许出现重复条目。
     *
     * <p>重复条目不会导致启动失败，但会让合并多次提交时的 diff 难以审阅，
     * 也容易掩盖「同一装配类被登记两次而其他类被漏登」的问题。
     */
    @Test
    void importsMustNotDeclareDuplicateEntries() {
        List<String> rawLines = readRawImportsLines();
        Set<String> seen = new LinkedHashSet<>();
        Set<String> duplicates = new TreeSet<>();

        for (String line : rawLines) {
            if (!seen.add(line)) {
                duplicates.add(line);
            }
        }

        assertThat(duplicates)
                .as("登记文件 %s 中存在重复条目：%s%s",
                        IMPORTS_RESOURCE, ENTRY_INDENT, String.join(ENTRY_INDENT, duplicates))
                .isEmpty();
    }

    /**
     * 读取本模块自身的登记文件，返回解析后的条目列表（保持出现顺序、保留重复项）。
     *
     * <p>遍历 classpath 上的<b>全部</b>同名资源（而非只取第一个），再按
     * {@link #MODULE_CLASSPATH_ROOT} 过滤出属于本模块的那一份，避免读到依赖 jar 的登记文件。
     * 空行与 {@code #} 开头的注释行会被忽略——这与 Spring Boot 自身解析登记文件的规则一致。
     *
     * @return 去重前的原始条目列表；若本模块的登记文件不存在或读取失败则直接断言失败
     */
    private static List<String> readRawImportsLines() {
        List<String> lines = new ArrayList<>();
        int ownedResourceCount = 0;

        try {
            Enumeration<URL> resources = CLASS_LOADER.getResources(IMPORTS_RESOURCE);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                if (!resource.toExternalForm().startsWith(MODULE_CLASSPATH_ROOT)) {
                    // 其他 jar 自带的登记文件，不属于本模块的装配契约，跳过。
                    continue;
                }
                ownedResourceCount++;
                try (InputStream in = resource.openStream()) {
                    String content = new String(in.readAllBytes(), StandardCharsets.UTF_8);
                    for (String line : content.split("\\R")) {
                        String trimmed = line.trim();
                        if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
                            lines.add(trimmed);
                        }
                    }
                }
            }
        } catch (IOException ex) {
            fail("读取登记文件 %s 失败：%s", IMPORTS_RESOURCE, ex.getMessage());
        }

        assertThat(ownedResourceCount)
                .as("未能在模块根 %s 下找到登记文件 %s（classpath 上存在同名文件，但都不属于本模块）",
                        MODULE_CLASSPATH_ROOT, IMPORTS_RESOURCE)
                .isPositive();
        return lines;
    }

    /**
     * 反推本模块在 classpath 上的根地址。
     *
     * <p>取本模块内固定类 {@link JowenAutoConfiguration} 的 {@code .class} 资源地址，
     * 截去其包路径与文件名部分，剩下的即为模块根。目录形态得到
     * {@code file:/.../target/classes/}，jar 形态得到 {@code jar:file:/.../xxx.jar!/}，
     * 两种形态都能与登记文件的资源地址做前缀比较。
     *
     * @return 模块 classpath 根地址，末尾带分隔符
     */
    private static String resolveModuleClasspathRoot() {
        String classFileName = "JowenAutoConfiguration.class";
        URL classResource = JowenAutoConfiguration.class.getResource(classFileName);
        if (classResource == null) {
            fail("无法定位 %s 的 .class 资源，无法反推模块 classpath 根", JowenAutoConfiguration.class.getName());
        }
        String externalForm = classResource.toExternalForm();
        String suffix = BASE_PACKAGE.replace('.', '/') + "/" + classFileName;
        if (!externalForm.endsWith(suffix)) {
            fail("类资源地址 %s 不以预期后缀 %s 结尾，无法反推模块 classpath 根", externalForm, suffix);
        }
        return externalForm.substring(0, externalForm.length() - suffix.length());
    }

    /**
     * 返回登记文件中声明的类名集合。
     *
     * @return 去重后的类名集合，按文件内出现顺序排列
     */
    private static Set<String> importedClassNames() {
        return new LinkedHashSet<>(readRawImportsLines());
    }

    /**
     * 扫描 {@value #BASE_PACKAGE} 包下所有标注 {@link AutoConfiguration} 的类。
     *
     * <p>{@code useDefaultFilters=false} 是必须的：默认的过滤器会把
     * {@code @Component}、{@code @ManagedBean}、{@code @Named} 等一并纳入，
     * 从而把测试夹具类和内部类混进结果，导致断言误报。
     *
     * <p>扫描器自身会过滤掉非独立类（内部类）与不携带 {@code @Lookup}
     * 方法的抽象类，与 Spring 对自动配置类的实例化要求一致。
     *
     * @return 扫描到的装配类全限定名集合，按字典序排列以保证失败信息稳定
     */
    private static Set<String> scanAutoConfigurationClassNames() {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(AutoConfiguration.class));

        Set<String> classNames = new TreeSet<>();
        for (BeanDefinition definition : scanner.findCandidateComponents(BASE_PACKAGE)) {
            classNames.add(definition.getBeanClassName());
        }
        return classNames;
    }
}
