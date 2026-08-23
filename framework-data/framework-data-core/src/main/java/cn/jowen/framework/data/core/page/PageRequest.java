package cn.jowen.framework.data.core.page;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * 分页请求，从 1 起计的友好 API。内部委托 {@link Pageable}（从 0 起计）。
 *
 * @author 王飞
 * @since 2026-08-25
 */
@NullMarked
public final class PageRequest {

    private final int page;
    private final int size;
    private final @Nullable Sort sort;

    private PageRequest(int page, int size, @Nullable Sort sort) {
        if (page < 1) {
            throw new IllegalArgumentException("页码必须 >= 1");
        }
        if (size < 1) {
            throw new IllegalArgumentException("每页大小必须 >= 1");
        }
        this.page = page;
        this.size = size;
        this.sort = sort;
    }

    /**
     * 创建分页请求（无排序）。
     *
     * @param page 页码（从 1 起），≥1
     * @param size 每页大小，≥1
     * @return 分页请求
     */
    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size, null);
    }

    /**
     * 创建带排序的分页请求。
     *
     * @param page 页码（从 1 起），≥1
     * @param size 每页大小，≥1
     * @param sort 排序，可为 {@code null}
     * @return 分页请求
     */
    public static PageRequest of(int page, int size, @Nullable Sort sort) {
        return new PageRequest(page, size, sort);
    }

    /**
     * 转为 {@link Pageable}（从 0 起计）。
     *
     * @return 对应的 Pageable
     */
    public Pageable toPageable() {
        return Pageable.of(page - 1, size, sort != null ? sort : Sort.unsorted());
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    @Nullable
    public Sort getSort() {
        return sort;
    }
}
