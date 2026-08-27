package cn.jowen.framework.plugin.descriptor;

import cn.jowen.framework.core.spi.SPI;
import cn.jowen.framework.plugin.support.ValidationError;
import org.jspecify.annotations.NullMarked;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * 插件描述符加载器 SPI 接口。
 *
 * <p>实现类负责从不同来源（plugin.json / plugin.yaml / MANIFEST.MF）加载
 * {@link PluginDescriptor}。实现经 META-INF/services 注册后被 core
 * {@link cn.jowen.framework.core.spi.ExtensionLoader} 统一发现，由
 * {@code PluginLoader} 按 {@link #supportedExtensions()} 选择。
 *
 * @author 王飞
 */
@NullMarked
@SPI(value = "json")
public interface PluginDescriptorLoader {

    /**
     * 从 JAR 文件加载插件描述符。
     *
     * @param jarPath JAR 路径，不可为 {@code null}
     * @return 插件描述符，不可为 {@code null}
     * @throws IOException 读取失败时抛出
     */
    PluginDescriptor load(Path jarPath) throws IOException;

    /**
     * 支持的描述符文件后缀。
     *
     * @return 后缀列表，如 {@code List.of("json", "yaml")}
     */
    List<String> supportedExtensions();

    /**
     * 校验解析后的描述符。
     *
     * @param descriptor 描述符，不可为 {@code null}
     * @return 校验错误列表
     */
    default List<ValidationError> validate(PluginDescriptor descriptor) {
        return descriptor.validate();
    }
}
