/*
 * Copyright (c) 2026 Jowen
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 */
package cn.jowen.framework.extras.storage.config;

import org.jspecify.annotations.NullMarked;

/**
 * 阿里云 OSS 云存储配置 POJO。
 *
 * <p>对应属性前缀 {@code framework.extras.storage.aliyun-oss}。
 *
 * @author Jowen
 * @date 2026-08-22
 */
@NullMarked
public class AliyunOssProperties {

    /** OSS Endpoint（如 https://oss-cn-hangzhou.aliyuncs.com）。 */
    private String endpoint;

    /** 默认 bucket 名称。 */
    private String bucket;

    /** AccessKey ID。 */
    private String accessKeyId;

    /** AccessKey Secret。 */
    private String accessKeySecret;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getBucket() {
        return bucket;
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public void setAccessKeyId(String accessKeyId) {
        this.accessKeyId = accessKeyId;
    }

    public String getAccessKeySecret() {
        return accessKeySecret;
    }

    public void setAccessKeySecret(String accessKeySecret) {
        this.accessKeySecret = accessKeySecret;
    }
}
