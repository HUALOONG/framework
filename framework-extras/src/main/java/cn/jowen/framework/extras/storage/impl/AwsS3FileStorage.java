package cn.jowen.framework.extras.storage.impl;

import cn.jowen.framework.extras.storage.FileInfo;
import cn.jowen.framework.extras.storage.FileStorage;
import cn.jowen.framework.extras.storage.config.AwsS3Properties;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.io.InputStream;
import java.util.List;
import java.util.Objects;

/**
 * AWS S3 云存储实现（骨架，兼容 S3 协议）。
 *
 * <p>基于 {@code software.amazon.awssdk:s3} SDK v2；依赖 optional。
 *
 * <p>当前为骨架实现，核心方法均返回 / 抛出占位信息。接入时需完成：
 * <ul>
 *   <li>使用 {@code S3Client.builder()} 构建客户端</li>
 *   <li>使用 {@code S3Presigner} 生成预签名 URL</li>
 *   <li>处理 {@code S3Exception} 及其子类（{@code NoSuchKeyException} 等）</li>
 * </ul>
 *
 * @author 王飞
 * @since 2026-08-22
 */
@NullMarked
public final class AwsS3FileStorage implements FileStorage {

    private final AwsS3Properties props;

    public AwsS3FileStorage(AwsS3Properties props) {
        this.props = Objects.requireNonNull(props, "props must not be null");
    }

    @Override
    public FileInfo upload(InputStream in, String originalFilename) {
        // TODO: 集成 AWS S3 SDK v2
        // S3Client s3 = S3Client.builder()
        //         .region(Region.of(props.getRegion()))
        //         .credentialsProvider(StaticCredentialsProvider.create(
        //                 AwsBasicCredentials.create(props.getAccessKey(), props.getSecretKey())))
        //         .build();
        // s3.putObject(req -> req.bucket(props.getBucket()).key(objectName), RequestBody.fromInputStream(in, in.available()));
        throw new UnsupportedOperationException("AWS S3 SDK 尚未接入（待集成 software.amazon.awssdk:s3）");
    }

    @Override
    public InputStream download(String objectName) {
        throw new UnsupportedOperationException("AWS S3 SDK 尚未接入");
    }

    @Override
    public @Nullable String getPresignedUrl(String objectName) {
        // TODO: 使用 S3Presigner 生成 V4 签名 URL
        // S3Presigner presigner = S3Presigner.builder()...build();
        // URL url = presigner.presignGetObject(GetObjectPresignRequest.builder()...build());
        return null;
    }

    @Override
    public void delete(String objectName) {
        throw new UnsupportedOperationException("AWS S3 SDK 尚未接入");
    }

    @Override
    public void deleteBatch(List<String> names) {
        if (names == null) {
            return;
        }
        for (String name : names) {
            delete(name);
        }
    }

    @Override
    public boolean exists(String objectName) {
        return false;
    }

    @Override
    public @Nullable FileInfo getFileInfo(String objectName) {
        return null;
    }

    @Override
    public List<FileInfo> listObjects(@Nullable String prefix) {
        return List.of();
    }
}
