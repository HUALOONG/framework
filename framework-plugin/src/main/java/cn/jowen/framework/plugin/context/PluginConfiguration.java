package cn.jowen.framework.plugin.context;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Map;

/**
 * 插件配置读取器接口。
 *
 * @author 王飞
 */
@NullMarked
public interface PluginConfiguration {

    /**
     * 获取字符串配置。
     */
    String getString(String key);

    /**
     * 获取整型配置，带默认值。
     */
    int getInt(String key, int defaultValue);

    /**
     * 获取布尔配置，带默认值。
     */
    boolean getBoolean(String key, boolean defaultValue);

    /**
     * 获取列表配置。
     */
    List<String> getList(String key);

    /**
     * 获取 Map 配置。
     */
    Map<String, String> getMap(String key);

    /**
     * 获取所有配置项。
     */
    Map<String, Object> getAll();
}
