/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.config;

import cn.jowen.framework.extras.captcha.CaptchaProperties;
import cn.jowen.framework.extras.captcha.CaptchaService;
import cn.jowen.framework.extras.captcha.LocalCaptchaService;
import cn.jowen.framework.extras.datapermission.DataPermissionContext;
import cn.jowen.framework.extras.datapermission.DataPermissionProperties;
import cn.jowen.framework.extras.desensitize.serializer.DesensitizeModule;
import cn.jowen.framework.extras.desensitize.DesensitizeProperties;
import cn.jowen.framework.extras.notification.LocalNotificationService;
import cn.jowen.framework.extras.notification.NotificationChannelHandler;
import cn.jowen.framework.extras.notification.NotificationProperties;
import cn.jowen.framework.extras.notification.NotificationService;
import cn.jowen.framework.extras.notification.config.DingTalkProperties;
import cn.jowen.framework.extras.notification.config.EmailProperties;
import cn.jowen.framework.extras.notification.config.SmsProperties;
import cn.jowen.framework.extras.notification.config.WebhookProperties;
import cn.jowen.framework.extras.notification.config.WeChatWorkProperties;
import cn.jowen.framework.extras.operatelog.OperateLogDispatcher;
import cn.jowen.framework.extras.operatelog.OperateLogHandler;
import cn.jowen.framework.extras.operatelog.OperateLogProperties;
import cn.jowen.framework.extras.operatelog.aop.OperateLogAspect;
import cn.jowen.framework.extras.storage.FileStorage;
import cn.jowen.framework.extras.storage.FileStorageManager;
import cn.jowen.framework.extras.storage.StorageProperties;
import cn.jowen.framework.extras.storage.config.AliyunOssProperties;
import cn.jowen.framework.extras.storage.config.AwsS3Properties;
import cn.jowen.framework.extras.storage.config.MinioProperties;
import cn.jowen.framework.extras.storage.impl.AliyunOssFileStorage;
import cn.jowen.framework.extras.storage.impl.AwsS3FileStorage;
import cn.jowen.framework.extras.storage.impl.LocalFileStorage;
import cn.jowen.framework.extras.storage.impl.MinioFileStorage;
import cn.jowen.framework.extras.storage.registry.DefaultCloudStorageRegistry;
import cn.jowen.framework.extras.storage.strategy.DatePathStrategy;
import cn.jowen.framework.extras.storage.strategy.HashStrategy;
import cn.jowen.framework.extras.storage.strategy.ObjectNameStrategy;
import cn.jowen.framework.extras.storage.strategy.OriginalNameStrategy;
import cn.jowen.framework.extras.storage.strategy.UuidStrategy;
import cn.jowen.framework.extras.datapermission.aop.DataPermissionAspect;
import cn.jowen.framework.extras.storage.impl.DefaultFileStorageManager;
import cn.jowen.framework.extras.notification.channel.EmailNotificationHandler;
import cn.jowen.framework.extras.notification.channel.WebhookNotificationHandler;
import cn.jowen.framework.extras.excel.ExcelProperties;
import cn.jowen.framework.extras.excel.ExcelService;
import cn.jowen.framework.extras.ip2region.Ip2RegionProperties;
import cn.jowen.framework.extras.ip2region.IpRegionService;
import jakarta.mail.Message;
import okhttp3.OkHttpClient;
import io.minio.MinioClient;
import com.aliyun.oss.OSSClientBuilder;
import software.amazon.awssdk.services.s3.S3Client;
import java.nio.file.Path;
import java.util.List;
import org.jspecify.annotations.NullMarked;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * framework-extras 装配配置类。
 *
 * <p>被 {@link EnableExtras} 引入，负责按条件注册各项核心 Bean。
 * 所有 Bean 注册均带 {@code @ConditionalOnClass}，互不影响；
 * 缺少对应 optional 依赖时仅不注册该 Bean，不会导致启动失败。
 *
 * <p>未提供 {@code @ConfigurationProperties} 绑定逻辑（由 boot-autoconfigure 层处理）。
 * 各 Properties POJO 仅作为普通容器 Bean 注册，供后续注入使用。
 *
 * @author Jowen
 * @date 2026-08-22
 * @see EnableExtras
 */
@NullMarked
@Configuration
@ConditionalOnClass(name = {
        "cn.jowen.framework.extras.captcha.CaptchaService",
        "cn.jowen.framework.extras.storage.FileStorage",
        "cn.jowen.framework.extras.notification.NotificationService",
        "cn.jowen.framework.extras.operatelog.OperateLogHandler",
        "cn.jowen.framework.extras.datapermission.DataPermissionRule",
        "cn.jowen.framework.extras.desensitize.serializer.DesensitizeModule"
})
@ConditionalOnProperty(prefix = "framework.extras", name = "enabled", matchIfMissing = true)
public class ExtrasBootstrapConfiguration {

    // ==================== 1. 各 Properties POJO ====================

    @Bean
    @ConditionalOnClass(LocalCaptchaService.class)
    public CaptchaProperties captchaProperties() {
        return new CaptchaProperties();
    }

    @Bean
    @ConditionalOnClass(LocalFileStorage.class)
    public StorageProperties storageProperties() {
        return new StorageProperties();
    }

    @Bean
    @ConditionalOnClass(LocalNotificationService.class)
    public NotificationProperties notificationProperties() {
        return new NotificationProperties();
    }

    @Bean
    @ConditionalOnClass(OperateLogDispatcher.class)
    public OperateLogProperties operateLogProperties() {
        return new OperateLogProperties();
    }

    @Bean
    @ConditionalOnClass(DataPermissionContext.class)
    public DataPermissionProperties dataPermissionProperties() {
        return new DataPermissionProperties();
    }

    @Bean
    @ConditionalOnClass(DesensitizeModule.class)
    public DesensitizeProperties desensitizeProperties() {
        return new DesensitizeProperties();
    }

    @Bean
    @ConditionalOnClass(ExcelService.class)
    public ExcelProperties excelProperties() {
        return new ExcelProperties();
    }

    @Bean
    @ConditionalOnClass(Ip2RegionProperties.class)
    public Ip2RegionProperties ip2RegionProperties() {
        return new Ip2RegionProperties();
    }

    // ==================== 2. 核心服务 Bean ====================

    @Bean
    @ConditionalOnClass(LocalCaptchaService.class)
    public CaptchaService jowenCaptchaService(CaptchaProperties props) {
        return new LocalCaptchaService(null, props);
    }

    @Bean
    @ConditionalOnClass(LocalFileStorage.class)
    public FileStorage jowenFileStorage(StorageProperties props) {
        return new LocalFileStorage(
                Path.of(props.getRootLocation()),
                resolveNamingStrategy(props.getNamingStrategy()));
    }

    @Bean
    @ConditionalOnClass(LocalNotificationService.class)
    public NotificationService jowenNotificationService(List<NotificationChannelHandler> handlers) {
        return new LocalNotificationService(handlers);
    }

    @Bean
    @ConditionalOnClass(OperateLogDispatcher.class)
    public OperateLogDispatcher jowenOperateLogDispatcher(List<OperateLogHandler> handlers) {
        return new OperateLogDispatcher(handlers);
    }

    // ==================== 3. Cloud Storage Registry ====================

    /**
     * 注册默认 FileStorageManager（本地存储 + 所有云后端）。
     *
     * <p>本地存储始终注册为 {@code "local"}；云后端（如 minio、aliyun-oss、aws-s3）
     * 通过 {@code List<FileStorage>} 注入后，按 bean 名称推断存储名并注册。
     */
    @Bean
    @ConditionalOnClass(LocalFileStorage.class)
    public FileStorageManager fileStorageManager(
            List<FileStorage> fileStorages,
            StorageProperties props
    ) {
        DefaultFileStorageManager manager = new DefaultFileStorageManager("local");
        // 本地存储
        manager.registerStorage("local",
                new LocalFileStorage(Path.of(props.getRootLocation()),
                        resolveNamingStrategy(props.getNamingStrategy())));
        // 云后端（name 通过 @Qualifier 或 bean name 推断；此处使用存储类简单推断）
        for (FileStorage storage : fileStorages) {
            String name = inferStorageName(storage);
            manager.registerStorage(name, storage);
        }
        return manager;
    }

    // ==================== 4. Notification Channel Handlers（conditional） ====================

    @Bean
    @ConditionalOnClass(Message.class)
    public NotificationChannelHandler emailNotificationHandler(EmailProperties props) {
        return new EmailNotificationHandler(props);
    }

    @Bean
    @ConditionalOnClass(OkHttpClient.class)
    public NotificationChannelHandler webhookNotificationHandler(WebhookProperties props) {
        return new WebhookNotificationHandler(props);
    }

    // ==================== 5. Cloud Storage Backends（conditional） ====================

    @Bean
    @ConditionalOnClass(MinioClient.class)
    public FileStorage minioFileStorage(MinioProperties props) {
        return new MinioFileStorage(props);
    }

    @Bean
    @ConditionalOnClass(OSSClientBuilder.class)
    public FileStorage aliyunOssFileStorage(AliyunOssProperties props) {
        return new AliyunOssFileStorage(props);
    }

    @Bean
    @ConditionalOnClass(S3Client.class)
    public FileStorage awsS3FileStorage(AwsS3Properties props) {
        return new AwsS3FileStorage(props);
    }

    // ==================== 6. AOP Aspects（optional，依赖 spring-boot-starter-aop） ====================

    @Bean
    @ConditionalOnClass(name = {"org.aspectj.lang.annotation.Aspect",
            "cn.jowen.framework.extras.operatelog.OperateLog"})
    @ConditionalOnBean(OperateLogDispatcher.class)
    public OperateLogAspect operateLogAspect(OperateLogDispatcher dispatcher) {
        return new OperateLogAspect(dispatcher);
    }

    @Bean
    @ConditionalOnClass(name = {"org.aspectj.lang.annotation.Aspect",
            "cn.jowen.framework.extras.datapermission.DataPermission"})
    public DataPermissionAspect dataPermissionAspect() {
        return new DataPermissionAspect();
    }

    // ==================== Helpers ====================

    private ObjectNameStrategy resolveNamingStrategy(String strategy) {
        return switch (strategy.toLowerCase()) {
            case "hash" -> new HashStrategy();
            case "uuid" -> new UuidStrategy();
            case "original" -> new OriginalNameStrategy();
            default -> new DatePathStrategy();
        };
    }

    /**
     * 通过存储实现类的简单命名推断存储名称。
     * 例如 MinioFileStorage → "minio"，AliyunOssFileStorage → "aliyun-oss"。
     */
    private String inferStorageName(FileStorage storage) {
        String simpleName = storage.getClass().getSimpleName().toLowerCase();
        if (simpleName.contains("minio")) {
            return "minio";
        }
        if (simpleName.contains("aliyun") || simpleName.contains("oss")) {
            return "aliyun-oss";
        }
        if (simpleName.contains("aws") || simpleName.contains("s3")) {
            return "aws-s3";
        }
        return "cloud";
    }
}
