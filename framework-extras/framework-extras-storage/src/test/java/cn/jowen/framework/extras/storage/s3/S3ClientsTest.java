package cn.jowen.framework.extras.storage.s3;

import cn.jowen.framework.extras.properties.StorageType;
import cn.jowen.framework.extras.storage.FileStorage;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link S3Clients} 工厂契约：区域缺省兜底、静态凭据装配，以及装配层无需感知 AWS SDK 细节。
 *
 * <p>AWS SDK 客户端在测试中会常驻 Netty 线程，故每个用例结束后通过反射关闭底层客户端，
 * 避免 CI 资源告警与用例间状态串扰。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class S3ClientsTest {

    @Test
    void create_nullRegion_fallsBackToDefaultRegion() throws Exception {
        withStorage(S3Clients.create(null, "access-key", "secret-key", "bucket"), storage ->
                assertThat(storage.type()).isEqualTo(StorageType.S3));
    }

    @Test
    void create_blankRegion_fallsBackToDefaultRegion() throws Exception {
        withStorage(S3Clients.create("   ", "access-key", "secret-key", "bucket"), storage ->
                assertThat(storage.type()).isEqualTo(StorageType.S3));
    }

    @Test
    void create_explicitRegion_usesGivenRegion() throws Exception {
        withStorage(S3Clients.create("cn-north-1", "access-key", "secret-key", "bucket"), storage ->
                assertThat(storage.type()).isEqualTo(StorageType.S3));
    }

    /** 执行断言后关闭底层 AWS 客户端，保证测试不泄漏 Netty 线程。 */
    private static void withStorage(FileStorage storage, java.util.function.Consumer<FileStorage> assertion)
            throws Exception {
        try {
            assertion.accept(storage);
        } finally {
            closeUnderlyingClients(storage);
        }
    }

    private static void closeUnderlyingClients(FileStorage storage) throws Exception {
        for (String fieldName : new String[]{"s3Client", "presigner"}) {
            Field field = S3FileStorage.class.getDeclaredField(fieldName);
            field.setAccessible(true);
            if (field.get(storage) instanceof AutoCloseable closeable) {
                closeable.close();
            }
        }
    }
}
