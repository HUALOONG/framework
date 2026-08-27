package cn.jowen.framework.plugin.hotswap;

import cn.jowen.framework.logger.facade.Logger;
import cn.jowen.framework.logger.facade.LoggerFactory;
import org.jspecify.annotations.NullMarked;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 手动热部署策略：不自动加载/卸载，仅收集变更清单待人工处置。
 *
 * @author 王飞
 * @since 2026-08-27
 */
@NullMarked
public final class ManualHotSwapStrategy implements PluginFileWatcher.FileChangeHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ManualHotSwapStrategy.class);

    private final Set<String> pending = ConcurrentHashMap.newKeySet();

    @Override
    public void onFileCreated(String fileName) {
        recordPending(fileName);
    }

    @Override
    public void onFileModified(String fileName) {
        recordPending(fileName);
    }

    @Override
    public void onFileDeleted(String fileName) {
        recordPending(fileName);
    }

    private void recordPending(String fileName) {
        pending.add(fileName);
        LOGGER.info("插件变更待人工处置：" + fileName);
    }

    /**
     * @return 待人工处置的变更文件清单（不可变）
     */
    public Set<String> pendingChanges() {
        return Set.copyOf(pending);
    }

    /**
     * 清空待处置清单（人工处置完成后调用）。
     */
    public void clearPending() {
        pending.clear();
    }
}