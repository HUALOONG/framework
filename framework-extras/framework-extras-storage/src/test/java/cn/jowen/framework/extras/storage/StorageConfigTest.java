package cn.jowen.framework.extras.storage;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link StorageConfig} 注解默认值与可读取性验证。
 */
class StorageConfigTest {

    @StorageConfig(bucket = "explicit-bucket")
    private static final class AnnotatedTarget {
    }

    @Test
    void defaultBucketIsEmpty() throws Exception {
        Method bucketMethod = StorageConfig.class.getDeclaredMethod("bucket");
        Object defaultValue = bucketMethod.getDefaultValue();
        assertThat(defaultValue).isEqualTo("");
    }

    @Test
    void annotatedElementExposesSpecifiedBucket() {
        StorageConfig annotation = AnnotatedTarget.class.getAnnotation(StorageConfig.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.bucket()).isEqualTo("explicit-bucket");
    }

    @Test
    void unannotatedElementHasNoAnnotation() {
        assertThat(StorageConfigTest.class.getAnnotation(StorageConfig.class)).isNull();
    }
}
