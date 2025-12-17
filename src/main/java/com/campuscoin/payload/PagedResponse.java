package com.campuscoin.payload;

import java.util.List;

/**
 * 分页响应对象
 */
public class PagedResponse<T> {
    private List<T> items; // 数据列表
    private int total; // 总记录数
    private int page; // 当前页码
    private int pageSize; // 每页大小
    private int totalPages; // 总页数

    public PagedResponse() {
    }

    public PagedResponse(List<T> items, int total, int page, int pageSize) {
        this.items = items;
        this.total = total;
        this.page = page;
        this.pageSize = pageSize;
        this.totalPages = pageSize > 0 ? (int) Math.ceil((double) total / pageSize) : 0;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }
}
