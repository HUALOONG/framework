package cn.jowen.framework.boot.autoconfigure.data.mapperfixture;

/**
 * 扫描夹具：既无 {@code @Mapper} 也不继承 {@code BaseMapper} 的普通接口，
 * 用于验证 {@code FlexMapperScanner} 包含过滤器的 {@code return false} 分支。
 */
public interface PlainInterface {

    String name();
}
