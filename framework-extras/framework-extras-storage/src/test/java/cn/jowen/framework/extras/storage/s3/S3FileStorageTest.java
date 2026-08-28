package cn.jowen.framework.extras.storage.s3;

import cn.jowen.framework.extras.properties.StorageType;
import cn.jowen.framework.extras.storage.exception.StorageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link S3FileStorage} 单元测试（mock {@link S3Client} 与 {@link S3Presigner}）。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@ExtendWith(MockitoExtension.class)
class S3FileStorageTest {

    @Mock
    private S3Client s3Client;
    @Mock
    private S3Presigner presigner;

    private S3FileStorage newStorage() {
        return new S3FileStorage(s3Client, presigner, "bucket");
    }

    @Test
    void type_isS3() {
        assertThat(newStorage().type()).isEqualTo(StorageType.S3);
    }

    @Test
    void put_returnsKey() {
        when(s3Client.putObject(any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class), any(RequestBody.class)))
                .thenReturn(PutObjectResponse.builder().build());
        String result = newStorage().put("a/b.txt", new ByteArrayInputStream("hi".getBytes()), "text/plain");
        assertThat(result).isEqualTo("a/b.txt");
    }

    @Test
    void get_notExists_returnsNull() {
        when(s3Client.headObject(any(software.amazon.awssdk.services.s3.model.HeadObjectRequest.class)))
                .thenThrow(s3Exception(404));
        assertThat(newStorage().get("missing")).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void get_exists_returnsStream() throws Exception {
        when(s3Client.headObject(any(software.amazon.awssdk.services.s3.model.HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());
        ResponseBytes<GetObjectResponse> bytes = mock(ResponseBytes.class);
        when(bytes.asByteArray()).thenReturn("data".getBytes());
        when(s3Client.getObjectAsBytes(any(software.amazon.awssdk.services.s3.model.GetObjectRequest.class)))
                .thenReturn(bytes);
        try (InputStream in = newStorage().get("key")) {
            assertThat(in).isNotNull();
        }
    }

    @Test
    void delete_succeeds() {
        when(s3Client.deleteObject(any(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.class)))
                .thenReturn(DeleteObjectResponse.builder().build());
        newStorage().delete("key");
    }

    @Test
    void exists_trueWhenHeadOk() {
        when(s3Client.headObject(any(software.amazon.awssdk.services.s3.model.HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());
        assertThat(newStorage().exists("key")).isTrue();
    }

    @Test
    void exists_falseWhen404() {
        when(s3Client.headObject(any(software.amazon.awssdk.services.s3.model.HeadObjectRequest.class)))
                .thenThrow(s3Exception(404));
        assertThat(newStorage().exists("missing")).isFalse();
    }

    @Test
    void generateUrl_returnsUrl() throws Exception {
        PresignedGetObjectRequest presigned = mock(PresignedGetObjectRequest.class);
        when(presigned.url()).thenReturn(URI.create("https://s3.test/bucket/key").toURL());
        when(presigner.presignGetObject(any(software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.class)))
                .thenReturn(presigned);
        URL url = newStorage().generateUrl("key", 3600_000L);
        assertThat(url).isNotNull();
        assertThat(url.toString()).isEqualTo("https://s3.test/bucket/key");
    }

    @Test
    void put_failureWrapsException() {
        when(s3Client.putObject(any(software.amazon.awssdk.services.s3.model.PutObjectRequest.class), any(RequestBody.class)))
                .thenThrow(new RuntimeException("boom"));
        assertThatThrownBy(() -> newStorage().put("key", new ByteArrayInputStream(new byte[0]), null))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("S3 上传失败");
    }

    @Test
    void get_failureWrapsS3Exception() {
        when(s3Client.headObject(any(software.amazon.awssdk.services.s3.model.HeadObjectRequest.class)))
                .thenReturn(HeadObjectResponse.builder().build());
        when(s3Client.getObjectAsBytes(any(software.amazon.awssdk.services.s3.model.GetObjectRequest.class)))
                .thenThrow(s3Exception(500));
        assertThatThrownBy(() -> newStorage().get("key"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("S3 下载失败");
    }

    @Test
    void delete_failureWrapsS3Exception() {
        when(s3Client.deleteObject(any(software.amazon.awssdk.services.s3.model.DeleteObjectRequest.class)))
                .thenThrow(s3Exception(500));
        assertThatThrownBy(() -> newStorage().delete("key"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("S3 删除失败");
    }

    @Test
    void exists_failureWrapsNon404() {
        when(s3Client.headObject(any(software.amazon.awssdk.services.s3.model.HeadObjectRequest.class)))
                .thenThrow(s3Exception(500));
        assertThatThrownBy(() -> newStorage().exists("key"))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("S3 存在性判断失败");
    }

    @Test
    void generateUrl_failureWrapsException() {
        when(presigner.presignGetObject(any(software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest.class)))
                .thenThrow(new RuntimeException("boom"));
        assertThatThrownBy(() -> newStorage().generateUrl("key", 0L))
                .isInstanceOf(StorageException.class)
                .hasMessageContaining("S3 生成 URL 失败");
    }

    private static S3Exception s3Exception(int statusCode) {
        S3Exception.Builder builder = S3Exception.builder();
        builder.statusCode(statusCode);
        builder.message("s3 error");
        return (S3Exception) builder.build();
    }
}
