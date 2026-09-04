package cn.jowen.framework.boot.autoconfigure.data.mapperfixture;

import com.mybatisflex.core.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 扫描夹具：显式标注 {@code @Mapper} 的接口，用于验证 {@code FlexMapperScanner}
 * 的 {@code hasAnnotation} 分支。仅测试可见，不参与生产装配。
 */
@Mapper
public interface MappedOrderMapper extends BaseMapper<Object> {
}
