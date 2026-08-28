package cn.jowen.framework.data.core.page;

import org.jspecify.annotations.NullMarked;

import java.util.List;

/**
 * 分页结果，含当前页数据、总记录数与分页元信息。
 *
 * @param <T> 元素类型
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class Page<T> {

    private final List<T> content;
    private final long total;
    private final int page;
    private final int size;

    public Page(List<T> content, long total, int page, int size) {
        this.content = List.copyOf(content);
        this.total = total;
        this.page = page;
        this.size = size;
    }

    /**
     * 返回当前页内容（不可变）。
     *
     * @return 内容列表
     */
    public List<T> getContent() {
        return content;
    }

    /**
     * 返回总记录数。
     *
     * @return 总数
     */
    public long getTotal() {
        return total;
    }

    /**
     * 返回总页数。
     *
     * @return 总页数
     */
    public int getTotalPages() {
        return size == 0 ? 0 : (int) Math.ceil((double) total / size);
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    /**
     * 是否首页。
     *
     * @return 是首页返回 {@code true}
     */
    public boolean isFirst() {
        return page == 0;
    }

    /**
     * 是否末页。
     *
     * @return 是末页返回 {@code true}
     */
    public boolean isLast() {
        return page + 1 >= getTotalPages();
    }

    /**
     * 是否为空页。
     *
     * @return 空返回 {@code true}
     */
    public boolean isEmpty() {
        return content.isEmpty();
    }

    /**
     * 创建空分页结果。
     *
     * @param <T> 元素类型
     * @return 空分页
     */
    public static <T> Page<T> empty() {
        return new Page<>(List.of(), 0, 0, 0);
    }
}
