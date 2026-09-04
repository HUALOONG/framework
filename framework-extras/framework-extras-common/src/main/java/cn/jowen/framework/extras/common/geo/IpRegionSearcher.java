package cn.jowen.framework.extras.common.geo;

import cn.jowen.framework.extras.properties.Ip2RegionProperties;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.lionsoul.ip2region.xdb.Searcher;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * IP 属地查询器：封装 ip2region {@link Searcher}，按配置的加载方式初始化库文件。
 *
 * <p>加载方式（{@link Ip2RegionProperties.LoadType}）：
 * <ul>
 *   <li>{@code MEMORY}：xdb 全量载入内存，查询最快、内存占用最高（默认）；</li>
 *   <li>{@code INDEX}：仅载入索引，查询时读文件，内存占用低；</li>
 *   <li>{@code FILE}：纯文件随机访问，内存占用最低、查询最慢。</li>
 * </ul>
 *
 * <p>异常策略：库文件缺失/损坏在构造期即以 {@link UncheckedIOException} 快速失败；
 * 查询失败属运行期故障，同样以非受检异常上抛，不静默吞掉。
 *
 * <p>依赖 {@code org.lionsoul:ip2region}（optional）：未引入时本类不可用。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class IpRegionSearcher implements AutoCloseable {

    /** xdb 库文件路径。 */
    private final String dbPath;

    /** 加载方式。 */
    private final Ip2RegionProperties.LoadType loadType;

    /** 常驻 Searcher（MEMORY/INDEX 模式；FILE 模式为 null，查询时按需创建）。 */
    private final @Nullable Searcher searcher;

    /**
     * 以默认内存模式构造。
     *
     * @param dbPath xdb 库文件路径
     * @throws UncheckedIOException 库文件不存在或读取失败
     */
    public IpRegionSearcher(String dbPath) {
        this(dbPath, Ip2RegionProperties.LoadType.MEMORY);
    }

    /**
     * 构造实例。
     *
     * @param dbPath   xdb 库文件路径
     * @param loadType 加载方式
     * @throws UncheckedIOException 库文件不存在或读取失败
     */
    public IpRegionSearcher(String dbPath, Ip2RegionProperties.LoadType loadType) {
        this.dbPath = dbPath;
        this.loadType = loadType;
        try {
            this.searcher = switch (loadType) {
                case MEMORY -> Searcher.newWithBuffer(Searcher.loadContentFromFile(dbPath));
                case INDEX -> Searcher.newWithVectorIndex(dbPath, Searcher.loadVectorIndexFromFile(dbPath));
                case FILE -> null;
            };
        } catch (IOException e) {
            throw new UncheckedIOException("ip2region 库文件加载失败: " + dbPath, e);
        }
    }

    /**
     * 查询 IP 属地。
     *
     * @param ip IPv4 字符串
     * @return 属地信息，不可为 {@code null}（原始串无效时各段为空串）
     * @throws UncheckedIOException     库文件读取失败（FILE/INDEX 模式）
     * @throws IllegalArgumentException IP 格式非法
     */
    public IpRegionInfo search(String ip) {
        try {
            Searcher current = searcher != null ? searcher : Searcher.newWithFileOnly(dbPath);
            String region = current.search(ip);
            if (searcher == null) {
                current.close();
            }
            return IpRegionInfo.parse(region);
        } catch (IOException e) {
            throw new UncheckedIOException("IP 属地查询失败: " + ip, e);
        } catch (Exception e) {
            // search(String) 声明受检 Exception：格式非法等以 IllegalArgumentException 语义上抛
            throw new IllegalArgumentException("IP 属地查询失败: " + ip, e);
        }
    }

    /**
     * 释放底层资源（FILE 模式无常驻资源，空操作）。
     */
    @Override
    public void close() {
        if (searcher != null) {
            try {
                searcher.close();
            } catch (IOException e) {
                throw new UncheckedIOException("ip2region Searcher 关闭失败", e);
            }
        }
    }
}
