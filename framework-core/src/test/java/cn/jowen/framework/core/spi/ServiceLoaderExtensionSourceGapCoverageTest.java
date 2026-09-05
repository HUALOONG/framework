package cn.jowen.framework.core.spi;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ServiceLoaderExtensionSource} 注解过滤逻辑测试，补齐 JaCoCo 缺口行 37–44。
 *
 * <p>覆盖场景：
 * <ul>
 *   <li>实现类无 {@code @SPIImplementation} 且无 {@code @Activate} → 跳过（continue）；</li>
 *   <li>实现类仅有 {@code @Activate} → 以类简单名注册，order=0。</li>
 * </ul>
 *
 * <p>实现类经 {@code META-INF/services} 静态注册，见同目录 resources 下同名文件。
 *
 * @author Yan
 * @since 0.0.1
 * @version 0.0.1
 */
class ServiceLoaderExtensionSourceGapCoverageTest {

    /**
     * 测试用扩展点接口。
     */
    public interface GapTestSPI {
        String id();
    }

    /**
     * 无 {@code @SPIImplementation} 也无 {@code @Activate}，应被跳过。
     */
    public static final class NoAnnotationImpl implements GapTestSPI {
        @Override
        public String id() {
            return "no-anno";
        }
    }

    /**
     * 仅有 {@code @Activate}，应以类简单名注册，order=0，activated=true。
     */
    @Activate(order = 5)
    public static final class OnlyActivateImpl implements GapTestSPI {
        @Override
        public String id() {
            return "only-activate";
        }
    }

    @Test
    void load_skipsUnannotated_andRegistersOnlyActivate() {
        ServiceLoaderExtensionSource<GapTestSPI> source = new ServiceLoaderExtensionSource<>();
        List<NamedExtension<GapTestSPI>> result = source.load(GapTestSPI.class, getClass().getClassLoader());

        // 无注解实现被跳过，不应出现
        assertThat(result).extracting(n -> n.instance().getClass().getSimpleName())
                .doesNotContain("NoAnnotationImpl");

        // 仅 @Activate 实现应以简单名注册
        NamedExtension<GapTestSPI> activateExt = result.stream()
                .filter(n -> n.instance().getClass() == OnlyActivateImpl.class)
                .findFirst()
                .orElseThrow();

        assertThat(activateExt.name()).isEqualTo("OnlyActivateImpl");
        // @Activate.order() 优先级高于 implOrder（此处为 0），故取注解值 5
        assertThat(activateExt.order()).isEqualTo(5);
        assertThat(activateExt.activated()).isTrue();
        assertThat(activateExt.sourceId()).isEqualTo(ServiceLoaderExtensionSource.SOURCE_ID);
    }
}
