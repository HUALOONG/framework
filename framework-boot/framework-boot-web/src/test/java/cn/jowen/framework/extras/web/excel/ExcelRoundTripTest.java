package cn.jowen.framework.extras.web.excel;

import com.alibaba.excel.annotation.ExcelProperty;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link ExcelExporter}/{@link ExcelImporter} 导入导出往返测试：
 * 覆盖正常往返、行数上限保护（导出/导入双向）与非法输入。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
class ExcelRoundTripTest {

    /** 测试行模型。 */
    public static class UserRow {
        /** 姓名。 */
        @ExcelProperty("姓名")
        private String name;

        /** 手机号。 */
        @ExcelProperty("手机号")
        private String phone;

        /** 分数。 */
        @ExcelProperty("分数")
        private Integer score;

        public UserRow() {
        }

        public UserRow(String name, String phone, Integer score) {
            this.name = name;
            this.phone = phone;
            this.score = score;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public Integer getScore() {
            return score;
        }

        public void setScore(Integer score) {
            this.score = score;
        }
    }

    @Test
    void roundTrip_preservesData() {
        ExcelExporter exporter = new ExcelExporter(1000);
        ExcelImporter importer = new ExcelImporter(1000);
        List<UserRow> rows = List.of(
                new UserRow("张三", "13812345678", 90),
                new UserRow("李四", "13987654321", 75));

        byte[] bytes = exporter.export("用户", rows, UserRow.class);
        List<UserRow> parsed = importer.read(new ByteArrayInputStream(bytes), UserRow.class);

        assertThat(parsed).hasSize(2);
        assertThat(parsed.get(0).getName()).isEqualTo("张三");
        assertThat(parsed.get(0).getPhone()).isEqualTo("13812345678");
        assertThat(parsed.get(0).getScore()).isEqualTo(90);
        assertThat(parsed.get(1).getName()).isEqualTo("李四");
    }

    @Test
    void export_emptyOrNullRows_producesReadableFile() {
        ExcelExporter exporter = new ExcelExporter(1000);
        ExcelImporter importer = new ExcelImporter(1000);

        byte[] empty = exporter.export("空", List.of(), UserRow.class);
        assertThat(importer.read(new ByteArrayInputStream(empty), UserRow.class)).isEmpty();

        byte[] fromNull = exporter.export("空", null, UserRow.class);
        assertThat(importer.read(new ByteArrayInputStream(fromNull), UserRow.class)).isEmpty();
    }

    @Test
    void export_exceedsMaxRows_throws() {
        ExcelExporter exporter = new ExcelExporter(2);
        List<UserRow> rows = List.of(
                new UserRow("a", "1", 1), new UserRow("b", "2", 2), new UserRow("c", "3", 3));

        assertThatThrownBy(() -> exporter.export("用户", rows, UserRow.class))
                .isInstanceOf(ExcelException.class)
                .hasMessageContaining("超过上限");
    }

    @Test
    void import_exceedsMaxRows_interruptsParsing() {
        ExcelExporter exporter = new ExcelExporter(1000);
        List<UserRow> rows = List.of(
                new UserRow("a", "1", 1), new UserRow("b", "2", 2), new UserRow("c", "3", 3));
        byte[] bytes = exporter.export("用户", rows, UserRow.class);

        ExcelImporter importer = new ExcelImporter(2);
        assertThatThrownBy(() -> importer.read(new ByteArrayInputStream(bytes), UserRow.class))
                .isInstanceOf(ExcelException.class)
                .hasMessageContaining("超过上限");
    }

    @Test
    void import_invalidStream_throwsExcelException() {
        ExcelImporter importer = new ExcelImporter(1000);
        // 伪 zip 头 + 二进制垃圾：POI 解包必然失败
        byte[] garbage = new byte[] {0x50, 0x4B, 0x03, 0x04, 0x00, 0x01, 0x02, 0x03, 0x04, 0x05};
        assertThatThrownBy(() -> importer.read(new ByteArrayInputStream(garbage), UserRow.class))
                .isInstanceOf(ExcelException.class);
    }

    @Test
    void constructors_rejectNonPositiveMaxRows() {
        assertThatThrownBy(() -> new ExcelExporter(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExcelExporter(-1)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new ExcelImporter(0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new ExcelExporter(1).getMaxRows()).isEqualTo(1);
        assertThat(new ExcelImporter(1).getMaxRows()).isEqualTo(1);
    }
}
