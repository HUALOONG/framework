package cn.jowen.framework.extras.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link FileInfo} record 契约验证。
 */
class FileInfoTest {

    @Test
    void recordCarriesAllComponents() {
        FileInfo info = new FileInfo("bucket-1", "photo.jpg",
                "2024/01/01/uuid.jpg", "https://cdn/x.jpg",
                "abc123", "image/jpeg", 1024L, 1700000000000L);

        assertThat(info.bucket()).isEqualTo("bucket-1");
        assertThat(info.name()).isEqualTo("photo.jpg");
        assertThat(info.key()).isEqualTo("2024/01/01/uuid.jpg");
        assertThat(info.url()).isEqualTo("https://cdn/x.jpg");
        assertThat(info.hash()).isEqualTo("abc123");
        assertThat(info.contentType()).isEqualTo("image/jpeg");
        assertThat(info.size()).isEqualTo(1024L);
        assertThat(info.uploadTime()).isEqualTo(1700000000000L);
    }

    @Test
    void nullableHashAndContentTypeAllowed() {
        FileInfo info = new FileInfo("b", "n", "k", "u", null, null, -1L, 0L);

        assertThat(info.hash()).isNull();
        assertThat(info.contentType()).isNull();
        assertThat(info.size()).isEqualTo(-1L);
    }

    @Test
    void equalityIsValueBased() {
        FileInfo a = new FileInfo("b", "n", "k", "u", "h", "ct", 1L, 2L);
        FileInfo b = new FileInfo("b", "n", "k", "u", "h", "ct", 1L, 2L);
        FileInfo c = new FileInfo("b", "n", "k", "u", "h", "ct", 9L, 2L);

        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }
}
