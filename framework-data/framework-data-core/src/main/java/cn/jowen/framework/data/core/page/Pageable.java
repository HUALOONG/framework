package cn.jowen.framework.data.core.page;

import cn.jowen.framework.data.core.sort.Sort;
import org.jspecify.annotations.NullMarked;

/**
 * 分页请求，含页码、大小与排序。页码从 0 开始。
 *
 * @author 王飞
 * @since 0.0.1
 * @version 0.0.1
 */
@NullMarked
public final class Pageable {

    private final int page;
    private final int size;
    private final Sort sort;

    private Pageable(int page, int size, Sort sort) {
        if (page < 0) {
            throw new IllegalArgumentException("页码不可为负");
        }
        if (size < 1) {
            throw new IllegalArgumentException("每页大小至少为 1");
        }
        this.page = page;
        this.size = size;
        this.sort = sort;
    }

    /**
     * 创建分页请求（无排序）。
     *
     * @param page 页码（从 0 起），非负
     * @param size 每页大小，≥1
     * @return 分页请求
     */
    public static Pageable of(int page, int size) {
        return new Pageable(page, size, Sort.unsorted());
    }

    /**
     * 创建带排序的分页请求。
     *
     * @param page 页码（从 0 起），非负
     * @param size 每页大小，≥1
     * @param sort 排序，不可为 {@code null}
     * @return 分页请求
     */
    public static Pageable of(int page, int size, Sort sort) {
        return new Pageable(page, size, sort);
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public Sort getSort() {
        return sort;
    }

    /**
     * 返回起始偏移量（用于 SQL LIMIT 偏移）。
     */
    public long getOffset() {
        return (long) page * size;
    }
}
