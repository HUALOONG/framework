package cn.jowen.framework.plugin.resolver;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DependencyResolutionExceptionTest {

    @Test
    void constructors() {
        DependencyResolutionException e1 = new DependencyResolutionException("bad dependency");
        assertThat(e1.getMessage()).isEqualTo("bad dependency");

        DependencyResolutionException e2 = new DependencyResolutionException(
                "bad dependency", new RuntimeException("c"));
        assertThat(e2.getMessage()).isEqualTo("bad dependency");
        assertThat(e2.getCause()).hasMessage("c");
    }
}
