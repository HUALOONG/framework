# framework-extras-message 模块架构设计

> 文档元信息
> - **模块**：framework-extras-message
> - **关键词**：消息通知、邮件、短信、钉钉、企微、Webhook、统一抽象
> - **描述**：统一消息通知抽象，支持邮件/短信/钉钉/企业微信/Webhook 多渠道发送，按 @ConditionalOnClass 按需装配
> - **基线**：Spring Boot 4.x + Java 21

---

## 一、模块定位

`framework-extras-message` 是框架的 **消息通知模块**，提供统一的通知发送接口，屏蔽各渠道差异。

**核心价值**：

| 场景       | 没有本模块             | 有本模块                        |
| :--------- | :--------------------- | :------------------------------ |
| 多渠道发送 | 业务代码硬编码多个 SDK | 统一 messageService 接口        |
| 渠道扩展   | 新增渠道需改业务代码   | 实现 messageChannelHandler 即可 |
| 模板支持   | 手动拼接消息内容       | SPI 模板引擎支持（预留）        |

---

## 二、功能清单与依赖矩阵

| 功能     | 子包        | 核心依赖                | 可选依赖            |
| :------- | :---------- | :---------------------- | :------------------ |
| 通知抽象 | core        | —                       | —                   |
| 渠道实现 | provider/   | —                       | 短信 SDK / 邮件 SDK |
| 模板引擎 | template/   | —                       | —（预留 SPI）       |
| 配置属性 | properties/ | framework-extras-common | —                   |

---

## 三、整体包结构

```text
framework-extras-message
└─ src/main/java/cn/jowen/framework/extras/message/
   ├─ core/                 # 抽象：Message / MessageType / MessageSender / MessageChannel
   ├─ provider/             # 渠道骨架：Sms / Email / Site / Push 四个抽象发送器
   └─ template/             # 模板引擎：Template / TemplateEngine / SimpleTemplateEngine
```

---

## 四、各子包详细设计

### 4.1 core/ — 核心抽象

```text
cn.jowen.framework.extras.message.core
├─ Message                  # 不可变消息描述：type / receiver / title / content / attachments
├─ MessageType              # 枚举：SMS / EMAIL / SITE / PUSH
├─ MessageSender            # SPI 接口：supportedType() / send(Message)
├─ MessageChannel           # 渠道路由接口：dispatch(Message)
└─ DefaultMessageChannel    # 默认路由：按 MessageType 分发到对应 Sender
```

### 4.2 provider/ — 渠道骨架

```text
cn.jowen.framework.extras.message.provider
├─ SmsMessageSender         # 短信发送器（骨架，doSend(phone, content) 由子类实现）
├─ EmailMessageSender       # 邮件发送器（骨架，doSend(to, subject, body)）
├─ SiteMessageSender        # 站内信发送器（骨架，doSend(receiver, title, content)）
└─ PushMessageSender        # 推送发送器（骨架，doSend(receiver, title, content)）
```

### 4.3 template/ — 模板引擎

```text
cn.jowen.framework.extras.message.template
├─ Template                 # 模板：code / titleTemplate / contentTemplate
├─ TemplateEngine           # SPI 接口：render(Template, variables) -> Rendered
└─ SimpleTemplateEngine     # 骨架实现：${key} 占位符替换
```

---

## 五、核心类关系图

```text
┌─────────────────────────────────────────┐
│     framework-extras-message            │
│                                         │
│  messageService                         │
│      │                                  │
│      ▼                                  │
│  messageChannelHandler (SPI)            │
│      │                                  │
│  ┌────┴────────────────────────┐        │
│  │     channel/                │        │
│  │  Email / Sms / DingTalk /   │        │
│  │  WeChatWork / Webhook       │        │
│  └─────────────────────────────┘        │
│                                         │
│ 可选依赖：angus-mail / okhttp / 短信SDK │
└─────────────────────────────────────────┘
```

---

## 六、分层依赖规则

```text
L0              framework-extras-message
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

    <!-- 渠道依赖（全部 optional） -->
    <dependency>
        <groupId>org.eclipse.angus</groupId>
        <artifactId>angus-mail</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>com.squareup.okhttp3</groupId>
        <artifactId>okhttp</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>com.aliyun</groupId>
        <artifactId>dysmsapi20170525</artifactId>
        <optional>true</optional>
    </dependency>

    <!-- Spring Boot 装配（optional） -->
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
    message:
      enabled: true
      email:
        host: smtp.example.com
        port: 465
        username: noreply@example.com
        password: xxx
        from: noreply@example.com
      sms:
        access-key-id: xxx
        access-key-secret: xxx
        sign-name: 签名
      dingtalk:
        webhook-url: https://oapi.dingtalk.com/robot/send?access_token=xxx
      wechat-work:
        agent-id: xxx
        secret: xxx
        corpid: xxx
      webhook:
        url: https://hooks.example.com/xxx
        headers:
          Authorization: Bearer xxx
```

---

## 九、使用方式

```java
// 发送通知
messageResult result = messageService.send(
                messageRequest.builder()
                        .channel(messageChannel.EMAIL)
                        .to("user@example.com")
                        .subject("测试邮件")
                        .content("&lt;h1&gt;你好&lt;/h1&gt;")
                        .build()
        );

// 批量发送
List<messageRequest> requests = ...; List<messageResult> results = messageService.sendBatch(requests);
```

## 十、SPI 扩展点汇总

| 扩展点接口              | 所在包           | 用途                   |
| :---------------------- | :--------------- | :--------------------- |
| `messageChannelHandler` | message          | 自定义通知渠道         |
| `messageTemplateEngine` | message/template | 自定义模板引擎（预留） |

---

## 十一、与整体框架的关系

```text
framework-extras-message
├─ 依赖：framework-extras-common
├─ 依赖：framework-core
└─ 可选：angus-mail / okhttp / 阿里云短信 SDK
```
