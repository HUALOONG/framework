package cn.jowen.framework.extras.storage.oss;

import cn.jowen.framework.extras.properties.StorageType;
import cn.jowen.framework.extras.storage.exception.StorageException;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.model.OSSObject;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

/**
 * {@link OssFileStorage} 单元测试。通过 {@code mockConstruction} 拦截 {@link OSSClientBuilder}
 * 的构建调用，注入 mock {@link OSS} 客户端。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class OssFileStorageTest {

    @Test
    void type_isOss() {
        OSS mockOss = mock(OSS.class);
        try (var ignored = mockConstruction(OSSClientBuilder.class,
                (mock, context) -> when(mock.build(anyString(), anyString(), anyString())).thenReturn(mockOss))) {
            assertThat(new OssFileStorage("ep", "bucket", "ak", "sk").type()).isEqualTo(StorageType.OSS);
        }
    }

    @Test
    void put_returnsKey() {
        OSS mockOss = mock(OSS.class);
        try (var ignored = mockConstruction(OSSClientBuilder.class,
                (mock, context) -> when(mock.build(anyString(), anyString(), anyString())).thenReturn(mockOss))) {
            OssFileStorage storage = new OssFileStorage("ep", "bucket", "ak", "sk");
            String result = storage.put("a/b.txt", new ByteArrayInputStream("hi".getBytes()), "text/plain");
            assertThat(result).isEqualTo("a/b.txt");
        }
    }

    @Test
    void get_notExists_returnsNull() {
        OSS mockOss = mock(OSS.class);
        when(mockOss.doesObjectExist(anyString(), anyString())).thenReturn(false);
        try (var ignored = mockConstruction(OSSClientBuilder.class,
                (mock, context) -> when(mock.build(anyString(), anyString(), anyString())).thenReturn(mockOss))) {
            assertThat(new OssFileStorage("ep", "bucket", "ak", "sk").get("missing")).isNull();
        }
    }

    @Test
    void get_exists_returnsStream() throws Exception {
        OSS mockOss = mock(OSS.class);
        when(mockOss.doesObjectExist(anyString(), anyString())).thenReturn(true);
        OSSObject object = mock(OSSObject.class);
        when(object.getObjectContent()).thenReturn(new ByteArrayInputStream("data".getBytes()));
        when(mockOss.getObject(anyString(), anyString())).thenReturn(object);
        try (var ignored = mockConstruction(OSSClientBuilder.class,
                (mock, context) -> when(mock.build(anyString(), anyString(), anyString())).thenReturn(mockOss))) {
            try (InputStream in = new OssFileStorage("ep", "bucket", "ak", "sk").get("key")) {
                assertThat(in).isNotNull();
            }
        }
    }

    @Test
    void delete_succeeds() {
        OSS mockOss = mock(OSS.class);
        try (var ignored = mockConstruction(OSSClientBuilder.class,
                (mock, context) -> when(mock.build(anyString(), anyString(), anyString())).thenReturn(mockOss))) {
            new OssFileStorage("ep", "bucket", "ak", "sk").delete("key");
        }
    }

    @Test
    void exists_true() {
        OSS mockOss = mock(OSS.class);
        when(mockOss.doesObjectExist(anyString(), anyString())).thenReturn(true);
        try (var ignored = mockConstruction(OSSClientBuilder.class,
                (mock, context) -> when(mock.build(anyString(), anyString(), anyString())).thenReturn(mockOss))) {
            assertThat(new OssFileStorage("ep", "bucket", "ak", "sk").exists("key")).isTrue();
        }
    }

    @Test
    void exists_false() {
        OSS mockOss = mock(OSS.class);
        when(mockOss.doesObjectExist(anyString(), anyString())).thenReturn(false);
        try (var ignored = mockConstruction(OSSClientBuilder.class,
                (mock, context) -> when(mock.build(anyString(), anyString(), anyString())).thenReturn(mockOss))) {
            assertThat(new OssFileStorage("ep", "bucket", "ak", "sk").exists("missing")).isFalse();
        }
    }

    @Test
    void generateUrl_returnsUrl() throws Exception {
        OSS mockOss = mock(OSS.class);
        when(mockOss.generatePresignedUrl(anyString(), anyString(), any(java.util.Date.class)))
                .thenReturn(new URL("https://oss.test/bucket/key"));
        try (var ignored = mockConstruction(OSSClientBuilder.class,
                (mock, context) -> when(mock.build(anyString(), anyString(), anyString())).thenReturn(mockOss))) {
            URL url = new OssFileStorage("ep", "bucket", "ak", "sk").generateUrl("key", 3600_000L);
            assertThat(url).isNotNull();
            assertThat(url.toString()).isEqualTo("https://oss.test/bucket/key");
        }
    }

    @Test
    void put_failureWrapsException() {
        OSS mockOss = mock(OSS.class);
        when(mockOss.putObject(any(com.aliyun.oss.model.PutObjectRequest.class)))
                .thenThrow(new RuntimeException("boom"));
        try (var ignored = mockConstruction(OSSClientBuilder.class,
                (mock, context) -> when(mock.build(anyString(), anyString(), anyString())).thenReturn(mockOss))) {
            assertThatThrownBy(() -> new OssFileStorage("ep", "bucket", "ak", "sk")
                    .put("key", new ByteArrayInputStream(new byte[0]), null))
                    .isInstanceOf(StorageException.class)
                    .hasMessageContaining("OSS 上传失败");
        }
    }

    @Test
    void get_failureWrapsException() {
        OSS mockOss = mock(OSS.class);
        when(mockOss.doesObjectExist(anyString(), anyString())).thenReturn(true);
        when(mockOss.getObject(anyString(), anyString())).thenThrow(new RuntimeException("boom"));
        try (var ignored = mockConstruction(OSSClientBuilder.class,
                (mock, context) -> when(mock.build(anyString(), anyString(), anyString())).thenReturn(mockOss))) {
            assertThatThrownBy(() -> new OssFileStorage("ep", "bucket", "ak", "sk").get("key"))
                    .isInstanceOf(StorageException.class)
                    .hasMessageContaining("OSS 下载失败");
        }
    }

    @Test
    void delete_failureWrapsException() {
        OSS mockOss = mock(OSS.class);
        org.mockito.Mockito.doThrow(new RuntimeException("boom"))
                .when(mockOss).deleteObject(anyString(), anyString());
        try (var ignored = mockConstruction(OSSClientBuilder.class,
                (mock, context) -> when(mock.build(anyString(), anyString(), anyString())).thenReturn(mockOss))) {
            assertThatThrownBy(() -> new OssFileStorage("ep", "bucket", "ak", "sk").delete("key"))
                    .isInstanceOf(StorageException.class)
                    .hasMessageContaining("OSS 删除失败");
        }
    }

    @Test
    void exists_failureWrapsException() {
        OSS mockOss = mock(OSS.class);
        when(mockOss.doesObjectExist(anyString(), anyString())).thenThrow(new IllegalStateException("boom"));
        try (var ignored = mockConstruction(OSSClientBuilder.class,
                (mock, context) -> when(mock.build(anyString(), anyString(), anyString())).thenReturn(mockOss))) {
            assertThatThrownBy(() -> new OssFileStorage("ep", "bucket", "ak", "sk").exists("key"))
                    .isInstanceOf(StorageException.class)
                    .hasMessageContaining("OSS 存在性判断失败");
        }
    }

    @Test
    void generateUrl_failureWrapsException() {
        OSS mockOss = mock(OSS.class);
        when(mockOss.generatePresignedUrl(anyString(), anyString(), any(java.util.Date.class)))
                .thenThrow(new RuntimeException("boom"));
        try (var ignored = mockConstruction(OSSClientBuilder.class,
                (mock, context) -> when(mock.build(anyString(), anyString(), anyString())).thenReturn(mockOss))) {
            assertThatThrownBy(() -> new OssFileStorage("ep", "bucket", "ak", "sk").generateUrl("key", 0L))
                    .isInstanceOf(StorageException.class)
                    .hasMessageContaining("OSS 生成 URL 失败");
        }
    }
}
