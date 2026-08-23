package cn.jowen.framework.plugin.config;

import cn.jowen.framework.plugin.Plugin;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class PluginApplicationContextTest {

    @Test
    void createsChildWithParentReference() {
        var parent = new AnnotationConfigApplicationContext();
        parent.refresh();
        try {
            PluginApplicationContext ctx = PluginApplicationContext.create(parent);
            ctx.configure("com.example", true);

            Plugin testPlugin = new Plugin() {
                @Override public String id() { return "test-spring"; }
                @Override public String version() { return "1.0.0"; }
                @Override public void afterPropertiesSet() {}
                @Override public void destroy() {}
            };

            var child = ctx.createFor(testPlugin);
            assertThat(child).isNotNull();
            assertThat(child.getParent()).isSameAs(parent);
            assertThat(ctx.hasChild("test-spring")).isTrue();

            ctx.closeFor("test-spring");
            assertThat(ctx.hasChild("test-spring")).isFalse();
        } finally {
            parent.close();
        }
    }

    @Test
    void closeAllDestroysAllChildren() {
        var parent = new AnnotationConfigApplicationContext();
        parent.refresh();
        try {
            PluginApplicationContext ctx = PluginApplicationContext.create(parent);
            ctx.configure("", false);

            Plugin p1 = makePlugin("p1");
            Plugin p2 = makePlugin("p2");
            ctx.createFor(p1);
            ctx.createFor(p2);
            assertThat(ctx.hasChild("p1")).isTrue();
            assertThat(ctx.hasChild("p2")).isTrue();

            ctx.close();
            assertThat(ctx.hasChild("p1")).isFalse();
            assertThat(ctx.hasChild("p2")).isFalse();
        } finally {
            parent.close();
        }
    }

    private static Plugin makePlugin(String id) {
        return new Plugin() {
            @Override public String id() { return id; }
            @Override public String version() { return "1.0.0"; }
            @Override public void afterPropertiesSet() {}
            @Override public void destroy() {}
        };
    }
}
