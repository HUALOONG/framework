package cn.jowen.framework.plugin.hotswap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HotSwapContextTest {

    @Test
    void success_recordsOldAndNewId() {
        HotSwapContext ctx = HotSwapContext.success("old", "new");
        assertThat(ctx.oldVersion()).isEqualTo("old");
        assertThat(ctx.newVersion()).isEqualTo("new");
        assertThat(ctx.success()).isTrue();
        assertThat(ctx.swapTime()).isNotNull();
    }

    @Test
    void failed_recordsOldIdOnly() {
        HotSwapContext ctx = HotSwapContext.failed("old", "reason");
        assertThat(ctx.oldVersion()).isEqualTo("old");
        assertThat(ctx.newVersion()).isNull();
        assertThat(ctx.success()).isFalse();
        assertThat(ctx.swapTime()).isNotNull();
    }
}
