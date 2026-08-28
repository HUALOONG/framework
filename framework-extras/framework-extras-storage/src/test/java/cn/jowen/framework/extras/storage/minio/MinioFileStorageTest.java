package cn.jowen.framework.extras.storage.minio;

import cn.jowen.framework.extras.properties.StorageType;
import cn.jowen.framework.extras.storage.exception.StorageException;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import io.minio.errors.ErrorResponseException;
import io.minio.GetObjectResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link MinioFileStorage} 单元测试（mock {@link MinioClient}）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@ExtendWith(MockitoExtension.class)
class MinioFileStorageTest {

    @Mock
    private MinioClient client;

    private MinioFileStorage newStorage() {
        return new MinioFileStorage(client, "bucket");
    }

    @Test
    void type_isMinio() {
        assertThat(newStorage().type()).isEqualTo(StorageType.MINIO);
    }

    @Test
    void put_returnsKey() throws Exception {
        MinioFileStorage storage = newStorage();
        InputStream content = new ByteArrayInputStream("hello".getBytes());
        String result = storage.put("a/b.txt", content, "text/plain");
        assertThat(result).isEqualTo("a/b.txt");
    }

    @Test
    void put_withoutContentType() throws Exception {
        MinioFileStorage storage = newStorage();
        String result = storage.put("key", new ByteArrayInputStream(new byte[0]), null);
        assertThat(result).isEqualTo("key");
    }

    @Test
    void get_notExists_returnsNull() throws Exception {
        when(client.statObject(any(StatObjectArgs.class))).thenThrow(mock(ErrorResponseException.class));
        assertThat(newStorage().get("missing")).isNull();
    }

    @Test
    void get_exists_returnsStream() throws Exception {
        when(client.statObject(any(StatObjectArgs.class))).thenReturn(mock(StatObjectResponse.class));
        when(client.getObject(any(GetObjectArgs.class))).thenReturn(mock(GetObjectResponse.class));
        try (InputStream in = newStorage().get("key")) {
            assertThat(in).isNotNull();
        }
    }

    @Test
    void delete_succeeds() throws Exception {
        newStorage().delete("key");
    }

    @Test
    void exists_trueWhenStatOk() throws Exception {
        when(client.statObject(any(StatObjectArgs.class))).thenReturn(mock(StatObjectResponse.class));
        assertThat(newStorage().exists("key")).isTrue();
    }

    @Test
    void exists_falseWhenErrorResponse() throws Exception {
        when(client.statObject(any(StatObjectArgs.class))).thenThrow(mock(ErrorResponseException.class));
        assertThat(newStorage().exists("missing")).isFalse();
    }

    @Test
    void generateUrl_returnsUrl() throws Exception {
        when(client.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenReturn("https://minio.test/bucket/key");
        URL url = newStorage().generateUrl("key", 3600_000L);
        assertThat(url).isNotNull();
        assertThat(url.toString()).isEqualTo("https://minio.test/bucket/key");
    }

    @Test
    void put_failureWrapsException() throws Exception {
        when(client.putObject(any(PutObjectArgs.class))).thenThrow(new RuntimeException("boom"));
        assertThatThrownBy(() -> newStorage().put("key", new ByteArrayInputStream(new byte[0]), null))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("MinIO 上传失败");
    }

    @Test
    void get_failureWrapsException() throws Exception {
        when(client.statObject(any(StatObjectArgs.class))).thenReturn(mock(StatObjectResponse.class));
        when(client.getObject(any(GetObjectArgs.class))).thenThrow(new RuntimeException("boom"));
        assertThatThrownBy(() -> newStorage().get("key"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("MinIO 下载失败");
    }

    @Test
    void delete_failureWrapsException() throws Exception {
        org.mockito.Mockito.doThrow(new RuntimeException("boom")).when(client).removeObject(any(RemoveObjectArgs.class));
        assertThatThrownBy(() -> newStorage().delete("key"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("MinIO 删除失败");
    }

    @Test
    void exists_failureWrapsException() throws Exception {
        when(client.statObject(any(StatObjectArgs.class))).thenThrow(new IllegalStateException("boom"));
        assertThatThrownBy(() -> newStorage().exists("key"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("MinIO 存在性判断失败");
    }

    @Test
    void generateUrl_failureWrapsException() throws Exception {
        when(client.getPresignedObjectUrl(any(GetPresignedObjectUrlArgs.class))).thenThrow(new RuntimeException("boom"));
        assertThatThrownBy(() -> newStorage().generateUrl("key", 0L))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("MinIO 生成 URL 失败");
    }
}
