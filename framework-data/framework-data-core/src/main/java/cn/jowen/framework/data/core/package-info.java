/**
 * framework-data-core：数据访问纯抽象层（L1），零 JDBC、零 Spring 依赖。
 *
 * <p>定义仓储、查询、分页、排序、事务、映射、方言、元注解与异常抽象，供 data-jdbc / data-mybatis 实现。
 *
 * @author Jowen
 * @date 2026-08-21
 */
@NullMarked
package cn.jowen.framework.data.core;

import org.jspecify.annotations.NullMarked;
