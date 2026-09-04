# 覆盖率工具链接通（JaCoCo）

本项目使用 JaCoCo 收集全工程测试覆盖率，并通过 `framework-coverage` 模块在 reactor 构建中汇总生成统一的聚合 HTML 报告。

## 生成聚合覆盖率报告

在项目根目录执行：

```bash
mvn verify
```

> 说明：必须从项目根目录（包含根 `pom.xml`）运行，否则 reactor 不会包含全部子模块，聚合报告将无法汇总。

## 报告产出位置

```
framework-coverage/target/site/jacoco-aggregate/index.html
```

各叶子模块在 `verify` 阶段还会于各自 `target/site/jacoco/index.html` 生成独立模块覆盖率报告。

## 工作原理与前提

- 根 `pom.xml` 的 `<build><plugins>` 中声明了 `jacoco-maven-plugin`（版本由 `${jacoco.version}` 管理），其 `prepare-agent` 执行会在 `initialize` 阶段把探针参数写入 `argLine` 属性，`maven-surefire-plugin` 默认读取该属性，因此各子模块无需额外配置即可自动插桩并产出 `target/jacoco.exec`。
- `framework-coverage` 模块已列入根 `pom.xml` 的 `<modules>` 列表末尾，且其 `pom.xml` 声明了 `report-aggregate` 目标（绑定 `verify` 阶段），并依赖全部叶子模块。
- `report-aggregate` 依赖各叶子模块在**同一次 reactor 构建**中先生成各自的 `target/jacoco.exec`，因此 `framework-coverage` 必须排在 reactor 最后构建（其依赖关系已保证这一顺序）。

## 常用命令提示

- 编译并生成聚合覆盖率报告：`mvn verify`
- 仅运行测试（只生成各模块 `target/jacoco.exec`，**不**生成聚合报告页）：`mvn test`

## 注意事项

- 若某个子模块在 `maven-surefire-plugin` 的 `<configuration>` 中硬编码了 `<argLine>...</argLine>` 字面量（而非通过属性引用），会覆盖 JaCoCo 注入的探针参数，导致该模块覆盖率丢失。遇到此类情况应在其 `argLine` 末尾追加 `${argLine}` 占位以合并两者。当前各子模块均未硬编码 `argLine`，无需改动。
