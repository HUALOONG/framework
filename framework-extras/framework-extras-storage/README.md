# framework-extras-storage 模块架构设计

> 文档元信息
> - **模块**：framework-extras-storage
> - **关键词**：文件存储、MinIO、阿里云OSS、AWS S3、统一抽象
> - **描述**：统一文件存储抽象层，支持本地/MinIO/阿里云OSS/AWS S3 四种后端，按 @ConditionalOnClass 按需装配
> - **基线**：Spring Boot 4.x + Java 21

---

## 一、模块定位

`framework-extras-storage` 是框架的 **文件存储模块**，提供统一的文件上传、下载、删除、预签名 URL 生成能力，屏蔽 MinIO/阿里云
OSS/AWS S3 后端差异。

**核心价值**：

| 场景       | 没有本模块                   | 有本模块                            |
|:-----------|:-----------------------------|:------------------------------------|
| 多后端切换 | 业务代码硬编码存储 SDK       | 统一 FileStorage 接口，后端可插拔   |
| 命名策略   | 各自实现 objectName 生成逻辑 | 5 种策略可插拔（日期/UUID/Hash 等） |
| 存储管理   | 手动管理多后端实例           | FileStorageManager 统一多后端管理   |

---

## 二、功能清单与依赖矩阵

| 功能         | 子包      | 核心依赖                | 可选依赖               |
|:-------------|:----------|:------------------------|:-----------------------|
| 存储抽象接口 | （根包）   | —                       | —                      |
| 本地实现     | local/    | —                       | —                      |
| OSS 实现    | oss/      | —                       | AliyunOSS              |
| S3 实现      | s3/       | —                       | AWS SDK v2             |
| 存储异常     | exception/| —                       | —                      |

---

## 三、整体包结构

```text
framework-extras-storage
└─ src/main/java/cn/jowen/framework/extras/storage/
   ├─ FileStorage.java       # 统一存储接口：put / get / delete / exists / generateUrl
   ├─ StorageType.java       # 枚举：LOCAL / OSS / S3
   ├─ exception/             # StorageException
   ├─ local/                 # LocalFileStorage
   ├─ oss/                   # OssFileStorage
   └─ s3/                    # S3FileStorage
```

---

## 四、各子包详细设计

### 4.1 核心接口

```text
cn.jowen.framework.extras.storage
├─ FileStorage              # 接口：put / get / delete / exists / generateUrl
├─ StorageType              # 枚举：LOCAL / OSS / S3
└─ exception/StorageException  # 异常
```

### 4.2 后端实现（已实现骨架）

```text
cn.jowen.framework.extras.storage.local
└─ LocalFileStorage         # 本地磁盘实现（路径穿越防护）

cn.jowen.framework.extras.storage.oss
└─ OssFileStorage           # 阿里云 OSS 实现（aliyun-sdk-oss）

cn.jowen.framework.extras.storage.s3
└─ S3FileStorage            # AWS S3 实现（SDK v2）
```

---

## 五、核心类关系图

```text
┌─────────────────────────────────────────────────┐
│        framework-extras-storage                 │
│                                                 │
│  FileStorage (接口)                             │
│      ▲                                          │
│  ┌────┴────┬───────────┬──────────────┐         │
│  │         │           │              │         │
│ LocalFile  MinioFile   AliyunOssFile  AwsS3File │
│ Storage    Storage     Storage        Storage   │
│                                                 │
│  FileStorageManager（多后端管理）               │
│  ObjectNameStrategy（命名策略）                 │
│                                                 │
│  可选依赖：MinIO / AliyunOSS / AWS S3           │
└─────────────────────────────────────────────────┘

```

---

## 六、分层依赖规则

```text
L0              framework-extras-storage
                  依赖：framework-extras-common + framework-core
```

---

## 七、外部依赖

```xml
<dependencies>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-extras-common</artifactId>
    </dependency>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-core</artifactId>
    </dependency>

    <!-- 存储后端（全部 optional） -->
    <dependency>
        <groupId>io.minio</groupId>
        <artifactId>minio</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>com.aliyun.oss</groupId>
        <artifactId>aliyun-sdk-oss</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>software.amazon.awssdk</groupId>
        <artifactId>s3</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot 装配（optional） -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-autoconfigure</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- 测试 -->
    <dependency>
        <groupId>org.wiremock</groupId>
        <artifactId>wiremock</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

---

## 八、配置属性

```yaml
framework:
  extras:
    storage:
      enabled: true
      type: local                    # local / minio / aliyun-oss / aws-s3
      object-name-strategy: date-path # date-path / uuid / hash / original
      local:
        path: /tmp/storage
      minio:
        endpoint: http://127.0.0.1:9000
        access-key: minioadmin
        secret-key: minioadmin
        bucket: myapp
      aliyun-oss:
        endpoint: oss-cn-hangzhou.aliyuncs.com
        access-key-id: xxx
        access-key-secret: xxx
        bucket: myapp
      aws-s3:
        region: us-east-1
        bucket: myapp
```

---

## 九、使用方式

```java
// 编程式
FileInfo info = storageManager.getStorage().upload(inputStream, "photo.jpg");
String url = storageManager.getStorage().getPresignedUrl(info.getObjectName(), Duration.ofHours(1));

// 多后端
FileStorage minioStorage = storageManager.getStorage("minio"); FileStorage localStorage = storageManager.getStorage("local");
```

## 十、SPI 扩展点汇总

| 扩展点接口           | 所在包   | 用途               |
|:---------------------|:---------|:-------------------|
| `FileStorage`        | storage  | 自定义存储后端     |
| `ObjectNameStrategy` | strategy | 自定义对象命名策略 |

---

## 十一、与整体框架的关系

```text
framework-extras-storage
├─ 依赖：framework-extras-common（Properties + Exception）
├─ 依赖：framework-core
└─ 可选：MinIO / AliyunOSS / AWS S3 SDK
```
