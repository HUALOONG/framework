package cn.jowen.framework.extras.common.geo;

import cn.jowen.framework.extras.properties.Ip2RegionProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link IpRegionSearcher} 异常路径测试：库文件缺失时各加载方式的快速失败行为
 * （正常查询需真实 xdb 数据文件，超出单测范围）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class IpRegionSearcherFallbackTest {

    @TempDir
    Path tempDir;

    private String missingDbPath() {
        return tempDir.resolve("nonexistent.xdb").toString();
    }

    @Test
    void construct_memoryMode_missingDb_throwsUncheckedIO() {
        assertThatThrownBy(() -> new IpRegionSearcher(missingDbPath()))
                .isInstanceOf(UncheckedIOException.class)
                .hasMessageContaining("ip2region");
    }

    @Test
    void construct_indexMode_missingDb_throwsUncheckedIO() {
        assertThatThrownBy(() -> new IpRegionSearcher(missingDbPath(),
                Ip2RegionProperties.LoadType.INDEX))
                .isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void construct_dbPathIsDirectory_throwsUncheckedIO() throws IOException {
        // 目录不可作为 xdb 读取：FileInputStream 打开即抛 IOException，构造期快速失败
        Path dir = Files.createDirectory(tempDir.resolve("db-as-dir"));

        assertThatThrownBy(() -> new IpRegionSearcher(dir.toString()))
                .isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void construct_fileMode_defersFailureToSearch() {
        // FILE 模式构造期不读库文件，失败推迟到首次查询
        IpRegionSearcher searcher = new IpRegionSearcher(missingDbPath(),
                Ip2RegionProperties.LoadType.FILE);

        assertThatThrownBy(() -> searcher.search("114.114.114.114"))
                .isInstanceOf(UncheckedIOException.class);
    }

    @Test
    void close_fileMode_noThrow() {
        IpRegionSearcher searcher = new IpRegionSearcher(missingDbPath(),
                Ip2RegionProperties.LoadType.FILE);

        assertThatCode(searcher::close).doesNotThrowAnyException();
        assertThat(searcher).isNotNull();
    }
}
