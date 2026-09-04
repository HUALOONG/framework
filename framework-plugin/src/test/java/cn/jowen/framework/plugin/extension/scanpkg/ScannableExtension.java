package cn.jowen.framework.plugin.extension.scanpkg;

import cn.jowen.framework.plugin.extension.Extension;

/**
 * 供 {@link cn.jowen.framework.plugin.extension.ExtensionScanner} 目录扫描测试使用的
 * 标注了 {@link Extension} 的样例类。
 */
@Extension(id = "scan-me", extensionPoint = "scan.ep")
public class ScannableExtension {
}
