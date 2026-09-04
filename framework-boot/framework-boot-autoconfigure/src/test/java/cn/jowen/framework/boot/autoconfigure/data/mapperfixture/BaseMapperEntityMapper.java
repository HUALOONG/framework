package cn.jowen.framework.boot.autoconfigure.data.mapperfixture;

import com.mybatisflex.core.BaseMapper;

/**
 * 扫描夹具：未标注 {@code @Mapper} 但继承 {@link BaseMapper} 的接口，
 * 用于验证 {@code FlexMapperScanner} 的接口父类型遍历分支。
 */
public interface BaseMapperEntityMapper extends BaseMapper<Object> {
}
