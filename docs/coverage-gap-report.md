# Jowen Framework 覆盖率缺口报告

> 测量时间：2026-09-02 21:20（verify5，merge 阶段前移修复后首次端到端正确聚合）
> 门禁标准：framework-coverage BUNDLE LINE COVEREDRATIO ≥ 0.95

## 1. 总量

| 指标 | 数值 |
|---|---|
| 聚合行覆盖 | **7,058 / 8,420 = 83.82%** |
| 缺口（missed lines） | 1,362 |
| 达标所需 | 覆盖率 ≥ 0.95 → missed ≤ 421，**还需补测约 941 行** |
| 测试规模 | ~2246 测试，0 失败，20 模块全绿 |

## 2. 缺口 Top 40（按 missed lines 降序）

| # | missed/total | 类 | 归属 Phase |
|---|---|---|---|
| 1 | 59/199 | plugin.descriptor.PluginJsonDescriptorParser | P4 |
| 2 | 58/117 | boot.autoconfigure.cache.SpringCacheAnnotationProcessor | P3 |
| 3 | 51/56 | data.mybatis.codegen.EntityGenerator | P2 |
| 4 | 49/67 | plugin.hotswap.PluginHotSwapManager | P4 |
| 5 | 42/47 | data.mybatis.codegen.TableDefGenerator | P2 |
| 6 | 38/77 | plugin.extension.ExtensionScanner | P4 |
| 7 | 37/91 | plugin.descriptor.PluginYamlDescriptorParser | P4 |
| 8 | 34/34 | cache.redisson.RedissonCache | P2 |
| 9 | 33/72 | boot.autoconfigure.plugin.PluginBootstrap | P3/P4 |
| 10 | 31/31 | plugin.hotswap.PluginHotSwapManager$WatchServiceWrapper | P4 |
| 11 | 30/30 | data.mybatis.adapter.FlexDataSourceAdapter | P2 |
| 12 | 29/44 | boot.autoconfigure.i18n.I18nAutoConfiguration | P3 |
| 13 | 26/119 | core.util.ReflectionUtils | P4 |
| 14 | 26/67 | i18n.source.PropertiesMessageSource | P2 |
| 15 | 22/111 | plugin.lifecycle.PluginLifecycleManager | P4 |
| 16 | 21/26 | data.mybatis.codegen.MapperGenerator | P2 |
| 17 | 20/20 | cache.redisson.RedissonCacheManager | P2 |
| 18 | 20/34 | boot.autoconfigure.plugin.PluginAutoConfiguration | P3 |
| 19 | 19/31 | boot.autoconfigure.data.MybatisAutoConfiguration$FlexMapperScanner | P2/P3 |
| 20 | 17/32 | plugin.support.PluginUtils | P4 |
| 21 | 16/59 | plugin.hotswap.PluginFileWatcher | P4 |
| 22 | 15/66 | boot.autoconfigure.data.MybatisAutoConfiguration | P2/P3 |
| 23 | 15/28 | extras.common.geo.IpRegionSearcher | P1 |
| 24 | 15/15 | extras.storage.s3.S3Clients | P1 |
| 25 | 15/18 | boot.autoconfigure.plugin.PluginEndpoint | P3 |
| 26 | 15/53 | core.event.EventBus | P4 |
| 27 | 14/16 | data.mybatis.adapter.FlexExceptionTranslator | P2 |
| 28 | 14/40 | data.jdbc.util.JdbcUtils | P2 |
| 29 | 14/53 | extras.web.properties.ExtrasWebProperties | P1 |
| 30 | 14/40 | plugin.support.PluginPackage | P4 |
| 31 | 13/16 | boot.autoconfigure.plugin.PluginHealthIndicator | P3 |
| 32 | 12/12 | plugin.extension.ExtensionFactory | P4 |
| 33 | 12/13 | data.core.support.AuditableEntity | P2 |
| 34 | 11/77 | plugin.hotswap.RestartHotSwapStrategy | P4 |
| 35 | 11/25 | data.mybatis.extension.FlexTenantHandler | P2 |
| 36 | 11/11 | boot.autoconfigure.data.MybatisAutoConfiguration$FlexMapperFactoryBean | P2/P3 |
| 37 | 10/33 | plugin.config.PluginProperties | P4 |
| 38 | 9/31 | extras.storage.local.LocalFileStorage | P1 |
| 39 | 9/133 | data.jdbc.mapping.BeanPropertyRowMapper$TypeConvert | P2 |
| 40 | 9/55 | data.mybatis.extension.ExtensionRegistry | P2 |

## 3. 按 Phase 汇总（Top 40 内）

| Phase | 范围 | missed 合计 | 主要目标 |
|---|---|---|---|
| **P2** data-mybatis + cache + i18n | #3,5,8,11,14,16,17,19,22,27,28,33,35,36,39,40 | ~373 | codegen 三件套（Entity/TableDef/Mapper）走临时目录端到端；RedissonCache/Manager 用 mock Redisson 实例；FlexDataSourceAdapter/ExceptionTranslator 分支 |
| **P3** boot-autoconfigure | #2,9,12,18,19,22,25,31,36 | ~222 | SpringCacheAnnotationProcessor AOP 切面链；I18nAutoConfiguration 装配分支；Plugin 装配 + Endpoint + HealthIndicator |
| **P4** plugin + core | #1,4,6,7,10,13,15,20,21,26,30,32,34,37 | ~383 | 描述符解析（JSON/YAML）数据驱动用例；hotswap（Manager+WatchServiceWrapper+FileWatcher+Restart）；ExtensionScanner 补类路径场景；ReflectionUtils / EventBus |

## 4. 建议攻坚顺序

1. **codegen 三件套 + Flex 系**（P2，~184 行）：表结构 → 实体/表定义/Mapper 生成，可用 H2 元数据 + 临时输出目录端到端断言，单投入产出比最高
2. **描述符解析 + hotswap**（P4，~281 行）：PluginJson/YamlDescriptorParser 用数据驱动 JSON/YAML 字符串；WatchServiceWrapper 抽象出可注入的 WatchService mock
3. **SpringCacheAnnotationProcessor**（P3，58 行）：单独一个 @Cacheable/@CacheEvict 切面集成测试
4. **RedissonCache/Manager**（P2，54 行）：mock RedissonClient/RMap/RBucket 返回值

## 5. 本次修复的聚合管道问题（备忘）

- 症状：聚合 CSV 与模块级报告矛盾（MapPluginConfiguration 37 missed vs 0 missed）
- 根因：父 pom 为所有模块声明 id=report 的 verify 执行，framework-coverage 同 id 重声明时**继承父执行槽位**（排 verify 最前），merge-results 作为新增执行排其后 → report 先跑、读到上一轮旧 merged exec
- 修复：merge-results 绑定 **package** 阶段（framework-coverage/pom.xml），彻底与 verify 内顺序解耦；verify5 已确认 merge → report → check 全部基于本轮数据
- 诊断工具留存：`.workbuddy/tools/Diag.java`（单类双 exec 对比）、`.workbuddy/tools/ReportGen.java`（绕过 Maven 快速出聚合报告 + 缺口榜）
