# framework-extras-common 模块架构设计

> 文档元信息
> - **模块**：framework-extras-common
> - **关键词**：配置属性、异常体系、公共基础设施
> - **描述**：framework-extras 的公共枢纽，承载各子模块的配置属性（Properties POJO）与异常类
> - **基线**：Spring Boot 4.x + Java 21

---

## 一、模块定位

`framework-extras-common` 是 `framework-extras` 聚合器下的 **公共基础子模块**，承载跨子模块共享的配置属性与异常类。

**核心价值**：

| 场景         | 没有本模块                        | 有本模块                            |
|:-------------|:----------------------------------|:------------------------------------|
| 配置属性复用 | 各子模块重复定义相同的 Properties | 统一收口到本模块，按需 import       |
| 异常类复用   | 重复定义或依赖耦合到子模块        | 异常体系集中维护                    |
| 依赖收敛     | 各子模块都带 core + logger 基线   | 子模块仅依赖 common，再叠加可选依赖 |

---

## 二、功能清单与依赖矩阵

| 功能     | 子包      | 核心依赖 | 可选依赖 |
|:---------|:----------|:---------|:---------|
| 统一响应   | （根包）   | —        | —        |
| 异常类   | exception | —        | —        |
| 工具类   | util      | —        | —        |

> 各能力子模块的 Properties 收口在各自模块的 `properties/` 包下（如 web 的 `ExtrasWebProperties`），common 仅承载跨域共享的异常与工具。

---

## 三、整体包结构

```text
framework-extras-common
└─ src/main/java/cn/jowen/framework/extras/common/
   ├─ Result.java              # 统一 API 响应体
   ├─ exception/               # 异常体系
   └─ util/                    # 通用工具
```

---

## 四、各子包详细设计

### 4.1 exception/ — 异常类

```text
cn.jowen.framework.extras.common.exception
├─ ExtrasException              # 异常基类（继承 core 的 FrameworkException）
└─ ErrorCodeEnum                # 扩展模块通用错误码枚举
```

### 4.2 util/ — 工具类

```text
cn.jowen.framework.extras.common.util
├─ Assert                       # 参数校验断言（基于 ExtrasException）
├─ StringUtils                  # 轻量字符串工具
├─ Snowflake                    # 分布式 ID 生成器
└─ CryptoUtils                  # AES-GCM 对称加解密
```

---

## 五、核心类关系图

```text
┌─────────────────────────────────────────┐
│         framework-extras-common         │
│                                         │
│  config/*Properties（12 项能力）        │
│  exception/*Exception                   │
│                                         │
│  依赖：framework-core                   │
└─────────────────────────────────────────┘
```

---

## 六、分层依赖规则

```text
L0 (零内部依赖)        framework-extras-common
                          ▲
L1 (各能力子模块)      framework-extras-storage
                       framework-extras-notification
                       framework-extras-web
```

---

## 七、外部依赖

```xml

<dependencies>
    <dependency>
        <groupId>cn.jowen.framework</groupId>
        <artifactId>framework-core</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-autoconfigure</artifactId>
        <optional>true</optional>
    </dependency>
</dependencies>
```

---

## 八、配置属性

```yaml
framework:
  extras:
    enabled: true
    lock: { enabled: true, type: redis, key-prefix: "lock:" }
    ratelimit: { enabled: true, default-algorithm: sliding-window }
    idempotent: { enabled: true, default-ttl: 60s }
    storage: { enabled: true, type: local }
    notification: { enabled: true }
    excel: { enabled: true }
    captcha: { enabled: true, type: arithmetic }
    ip2region: { enabled: true, load-type: memory }
    desensitize: { enabled: true }
    operatelog: { enabled: true, async: true }
    datapermission: { enabled: true }
```

---

## 九、使用方式

```xml
<dependency>
    <groupId>cn.jowen.framework</groupId>
    <artifactId>framework-extras-common</artifactId>
</dependency>
```

## 十、SPI 扩展点汇总

本模块为纯配置与异常模块，无 SPI 扩展点。

---

## 十一、与整体框架的关系

```text
framework-extras（聚合父 POM）
├─ framework-extras-common    ← 本模块
├─ framework-extras-storage
├─ framework-extras-notification
└─ framework-extras-web
```
