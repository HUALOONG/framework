## 项目概述

- 项目名称： **Jowen Framework**
- 技术栈：Spring Boot 4.x + Spring Cloud 2025.x

## 环境搭建

- JDK 21+
- Maven 4+

## 编码规范

### POM 规范

#### `<properties>` 排序

按以下顺序分组，组内按字母序排列：

1. **构建配置** — `maven.*`、`project.*`
2. **核心框架** — Spring Boot/Cloud、ORM 等
3. **缓存** — Caffeine、Redisson 等
4. **对象存储** — MinIO、阿里云 OSS、AWS S3 等
5. **通信服务** — 邮件、短信、钉钉、企微等
6. **工具库** — EasyExcel、ip2region、OkHttp 等
7. **测试工具** — H2、WireMock 等

所有版本号必须定义在 `<properties>` 中，禁止在 `<dependencyManagement>`和`<dependencies>` 里硬编码。

####  `<dependencyManagement>`和`<dependencies>`  排序

按以下顺序分组：

1. **BOM 导入** — `<scope>import</scope>` 的第三方 BOM
2. **内部模块** — 按架构层次排列（core → data → cache → extras → boot-starter）
3. **第三方依赖** — 与 `<properties>` 相同的功能域分组
4. **测试依赖** — 放最后，与生产依赖隔离

#### `<build>` 插件排序

按 Maven 生命周期阶段先后排列：

1. **initialize 阶段** — `flatten-maven-plugin`、`git-commit-id-maven-plugin`
2. **package 阶段** — `maven-jar-plugin`

插件版本统一提取到 `<properties>` 管理。

### 命名规范

- 类名：大驼峰（UserService）
- 方法名：小驼峰（getUserById）
- 常量：全大写下划线（MAX_RETRY_COUNT）
- 私有变量：_camelCase（如 _userData）
- 所有 public 方法必须写 Javadoc
- 每个类必须有 @author 和 @since

### 测试规范

- **一个被测类对应一个测试类**，命名为 `XxxTest`。
- **复杂类按功能拆分**，如 `OrderServiceCreateTest`、`OrderServicePaymentTest`。
- **禁止**一个大文件测所有类，也**禁止**每个方法一个测试文件。

## Git 提交规范

- feat: 新功能
- fix: 修复 bug
- docs: 文档修改
- refactor: 重构
- test: 测试
- chore: 构建/工具变动

## 工作要求

- 修改代码前，先简单说明准备改哪里。
- 不确定的地方不要猜，先说明不确定原因。
- 不要主动新增生产依赖，除非我明确同意。
- 修改完成后，告诉我改了哪些文件，以及如何验证。

## 完成标准

任务完成后，请按下面格式回复：

1. 修改了哪些文件
2. 完成了什么功能
3. 如何运行或测试
4. 是否还有待确认问题
