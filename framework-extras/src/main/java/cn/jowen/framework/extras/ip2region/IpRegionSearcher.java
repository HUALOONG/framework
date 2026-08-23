package cn.jowen.framework.extras.ip2region;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.lionsoul.ip2region.xdb.Searcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * IP 地域搜索器，封装三种加载策略（FILE / MEMORY / INDEX）于单一类中。
 *
 * <p>使用示例：
 * <pre>{@code
 * IpRegionSearcher searcher = IpRegionSearcher.of("classpath:ip2region.xdb", LoadType.MEMORY);
 * IpRegion region = searcher.search("8.8.8.8");
 * searcher.close();
 * }</pre>
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public final class IpRegionSearcher implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(IpRegionSearcher.class);

    private final Searcher searcher;

    private IpRegionSearcher(String dbPath, LoadType loadType) {
        this.searcher = switch (loadType) {
            case MEMORY -> newMemorySearcher(dbPath);
            case INDEX -> newVectorIndexSearcher(dbPath);
            default -> newFileOnlySearcher(dbPath);
        };
        log.info("IpRegionSearcher initialized, dbPath={}, loadType={}", dbPath, loadType);
    }

    /**
     * 创建搜索器实例。
     *
     * @param dbPath   .xdb 数据库文件路径（支持 {@code classpath:} 前缀）
     * @param loadType 加载策略，不可为 {@code null}
     * @return 搜索器实例，不可为 {@code null}
     */
    public static IpRegionSearcher of(String dbPath, LoadType loadType) {
        return new IpRegionSearcher(dbPath, loadType);
    }

    /**
     * 查询 IP 地域信息。
     *
     * @param ip IP 地址字符串
     * @return 地域结果，查询失败或为空时返回 {@code null}
     */
    public @Nullable IpRegion search(String ip) {
        if (ip == null || ip.isBlank()) {
            return null;
        }
        try {
            String result = searcher.search(ip);
            return parseResult(ip, result);
        } catch (Exception e) {
            log.warn("search failed for ip={}", ip, e);
            return null;
        }
    }

    @Override
    public void close() {
        try {
            searcher.close();
        } catch (Exception ignored) {
        }
    }

    /**
     * MEMORY 策略：将 .xdb 文件全量加载到堆内存，查询最快（<1ms），内存开销约 10MB。
     */
    private static Searcher newMemorySearcher(String dbPath) {
        byte[] data = loadBytes(dbPath);
        try {
            return Searcher.newWithBuffer(data);
        } catch (Exception e) {
            throw new IllegalStateException("failed to create memory searcher from db: " + dbPath, e);
        }
    }

    /**
     * INDEX 策略：加载向量索引到内存，数据库文件保持在磁盘。
     */
    private static Searcher newVectorIndexSearcher(String dbPath) {
        try (RandomAccessFile raf = new RandomAccessFile(dbPath, "r")) {
            byte[] vectorIndex = Searcher.loadVectorIndex(raf);
            return Searcher.newWithVectorIndex(dbPath, vectorIndex);
        } catch (Exception e) {
            throw new IllegalStateException("failed to create vector index searcher from: " + dbPath, e);
        }
    }

    /**
     * FILE 策略：每次查询直接从文件读取，内存占用最小，查询最慢。
     */
    private static Searcher newFileOnlySearcher(String dbPath) {
        try {
            return Searcher.newWithFileOnly(dbPath);
        } catch (Exception e) {
            throw new IllegalStateException("failed to create file searcher from: " + dbPath, e);
        }
    }

    private static byte[] loadBytes(String dbPath) {
        String cleanPath = dbPath.startsWith("classpath:")
                ? dbPath.substring("classpath:".length()) : dbPath;
        InputStream is = IpRegionSearcher.class.getClassLoader().getResourceAsStream(cleanPath);
        if (is != null) {
            try (InputStream stream = is) {
                return stream.readAllBytes();
            } catch (Exception e) {
                throw new IllegalStateException("failed to read classpath resource: " + cleanPath, e);
            }
        }
        Path path = Path.of(dbPath);
        if (!Files.exists(path)) {
            throw new IllegalStateException("db file not found: " + dbPath);
        }
        try {
            return Files.readAllBytes(path);
        } catch (Exception e) {
            throw new IllegalStateException("failed to read db file: " + dbPath, e);
        }
    }

    private static IpRegion parseResult(String ip, String raw) {
        if (raw == null || raw.isBlank()) {
            return new IpRegion(ip, null, null, null, null, null, "");
        }
        String[] parts = raw.split("\\|", -1);
        String country = safe(parts, 0);
        String region = safe(parts, 1);
        String province = safe(parts, 2);
        String city = safe(parts, 3);
        String isp = safe(parts, 4);
        String fullRegion = String.join(" ", Stream.of(region, province, city)
                .filter(Objects::nonNull)
                .filter(s -> !"0".equals(s) && !s.isBlank())
                .toArray(String[]::new)).trim();
        return new IpRegion(ip, country, region, province, city, isp, fullRegion);
    }

    private static @Nullable String safe(String[] arr, int index) {
        return index < arr.length && !"0".equals(arr[index])
                ? arr[index] : null;
    }

    /**
     * .xdb 数据库加载策略。
     */
    public enum LoadType {
        /**
         * 文件加载：每次查询直接读文件，内存占用最小，查询最慢。
         */
        FILE,
        /**
         * 内存加载：全量加载到堆内存，查询最快（<1ms），内存开销约 10MB。
         */
        MEMORY,
        /**
         * 向量索引加载：加载向量索引到内存，查询速度介于 FILE 与 MEMORY 之间。
         */
        INDEX
    }
}