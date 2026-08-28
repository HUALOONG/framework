package cn.jowen.framework.demo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * framework-demo 启动入口。
 *
 * <p>引入 {@code framework-boot-starter} 后，{@code spring.factories} 中注册的
 * {@code JowenAutoConfiguration} 会自动生效（位于
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}），
 * 并按 classpath 上的模块按需装配：data / cache / i18n / logger / extras / plugin。
 *
 * <p><b>扩展提示</b>：
 * <ul>
 *   <li>新增能力模块 —— 在 pom 中加入对应 {@code framework-*} 依赖即可，无需改动本类；</li>
 *   <li>覆盖框架默认 Bean —— 在任意 {@code @Configuration} 中声明同名 Bean，
 *       框架装配均标注了 {@code @ConditionalOnMissingBean}，业务 Bean 优先；</li>
 *   <li>关闭某项能力 —— 设置对应 {@code framework.xxx.enabled=false}。</li>
 * </ul>
 *
 * @author demo
  * @since 0.0.1
 * @version 0.0.1
 */
@SpringBootApplication
public class DemoApplication {

    /**
     * 执行main操作。
     */
    public static void main(String[] args) {
        SpringApplication.run(DemoApplication.class, args);
    }
}
