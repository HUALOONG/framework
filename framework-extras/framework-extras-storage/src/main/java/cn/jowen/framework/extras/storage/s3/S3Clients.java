package cn.jowen.framework.extras.storage.s3;

import cn.jowen.framework.extras.storage.FileStorage;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * AWS S3 客户端工厂：将 SDK 客户端的构建细节封闭在 storage 模块内。
 *
 * <p>方法签名只暴露框架自有类型（{@link FileStorage} / {@code String}），
 * 使装配层（framework-boot-autoconfigure）无需引入 AWS SDK 即可完成装配——
 * SDK 在本模块为 optional 依赖，不向下游传递。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class S3Clients {

    /** 缺省区域，用于未配置 {@code region} 时兜底 */
    private static final Region DEFAULT_REGION = Region.US_EAST_1;

    private S3Clients() {
    }

    /**
     * 按区域与静态密钥构建 S3 存储实现。
     *
     * @param region    区域标识（如 {@code cn-north-1}），为空时使用 {@code us-east-1}
     * @param accessKey 访问密钥
     * @param secretKey 密钥
     * @param bucket    桶名
     * @return S3 存储实现
     */
    public static FileStorage create(@Nullable String region, String accessKey,
                                     String secretKey, String bucket) {
        Region resolved = (region == null || region.isBlank())
                ? DEFAULT_REGION
                : Region.of(region);
        var credentials = AwsBasicCredentials.create(accessKey, secretKey);
        var provider = StaticCredentialsProvider.create(credentials);

        S3Client client = S3Client.builder()
                .region(resolved)
                .credentialsProvider(provider)
                .build();
        S3Presigner presigner = S3Presigner.builder()
                .region(resolved)
                .credentialsProvider(provider)
                .build();
        return new S3FileStorage(client, presigner, bucket);
    }
}
