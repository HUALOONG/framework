package cn.jowen.framework.data.core.datasource;

import cn.jowen.framework.core.context.ContextCarrier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class DataSourceContextTest {

    ContextCarrier.Mode _previousMode;

    @BeforeEach
    void setup() {
        _previousMode = ContextCarrier.mode();
        ContextCarrier.configure(ContextCarrier.Mode.THREAD_LOCAL);
    }

    @AfterEach
    void cleanup() {
        DataSourceContext.clear();
        ContextCarrier.configure(_previousMode);
    }

    @Test
    void getDataSourceKey_initiallyNull() {
        assertThat(DataSourceContext.getDataSourceKey()).isNull();
    }

    @Test
    void setAndGetDataSourceKey() {
        DataSourceContext.setDataSourceKey("master");
        assertThat(DataSourceContext.getDataSourceKey()).isEqualTo("master");
    }

    @Test
    void clear_resetsToNull() {
        DataSourceContext.setDataSourceKey("master");
        DataSourceContext.clear();
        assertThat(DataSourceContext.getDataSourceKey()).isNull();
    }

    @Test
    void runWith_propagatesKey() {
        DataSourceContext.setDataSourceKey("master");
        final String[] captured = {null};

        DataSourceContext.runWith("slave", () -> {
            captured[0] = DataSourceContext.getDataSourceKey();
        });

        assertThat(captured[0]).isEqualTo("slave");
        assertThat(DataSourceContext.getDataSourceKey()).isEqualTo("master");
    }

    @Test
    void runWith_nested() {
        final String[] captured = {null};

        DataSourceContext.runWith("outer", () -> {
            assertThat(DataSourceContext.getDataSourceKey()).isEqualTo("outer");
            DataSourceContext.runWith("inner", () -> {
                captured[0] = DataSourceContext.getDataSourceKey();
            });
            assertThat(DataSourceContext.getDataSourceKey()).isEqualTo("outer");
        });

        assertThat(captured[0]).isEqualTo("inner");
    }

    @Test
    void runWith_removesAfterScope() {
        final String[] captured = {null};

        DataSourceContext.runWith("temp", () -> {
            // inside scope
        });

        captured[0] = DataSourceContext.getDataSourceKey();
        assertThat(captured[0]).isNull();
    }
}
